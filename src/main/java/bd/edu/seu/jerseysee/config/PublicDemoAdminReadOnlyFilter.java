package bd.edu.seu.jerseysee.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Keeps the public portfolio administrator useful for review without allowing live data mutation. */
public class PublicDemoAdminReadOnlyFilter extends OncePerRequestFilter {

    static final String PUBLIC_DEMO_ADMIN = "admin@jerseysee.demo";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();
        boolean demoAdministrator = authentication != null
                && authentication.isAuthenticated()
                && PUBLIC_DEMO_ADMIN.equalsIgnoreCase(authentication.getName());
        boolean staffMutation = (path.equals("/staff") || path.startsWith("/staff/"))
                && !SAFE_METHODS.contains(request.getMethod());

        if (demoAdministrator && staffMutation) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "The public demo administrator is read-only.");
            return;
        }
        chain.doFilter(request, response);
    }
}
