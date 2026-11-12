package bd.edu.seu.jerseysee.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService tokens;
    private final ObjectProvider<UserDetailsService> users;
    public JwtAuthFilter(JwtService tokens, ObjectProvider<UserDetailsService> users) {
        this.tokens = tokens; this.users = users;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String token = null;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) token = header.substring(7).trim();
        if (token == null && !path.startsWith("/api/") && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (JwtCookieService.NAME.equals(cookie.getName())) { token = cookie.getValue(); break; }
            }
        }
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                var claims = tokens.verify(token);
                var user = users.getObject().loadUserByUsername(claims.getSubject());
                if (tokens.matches(claims, user)) {
                    SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
                }
            } catch (JwtException | IllegalArgumentException | AuthenticationException invalid) {
                // Expired, tampered, disabled, deleted or password-revoked accounts stay anonymous.
            }
        }
        chain.doFilter(request, response);
    }
}
