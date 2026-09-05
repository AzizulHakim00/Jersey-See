package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@Profile({"demo", "production"})
@ConditionalOnProperty(name = "app.public-demo.enabled", havingValue = "true")
public class PublicDemoLoginController {

    private static final Map<String, String> DEMO_ACCOUNTS = Map.of(
            "customer", "customer@jerseysee.demo",
            "admin", "admin@jerseysee.demo"
    );

    private final CustomUserDetailsService userDetailsService;

    public PublicDemoLoginController(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/demo-login/{account}")
    public String login(@PathVariable String account, HttpServletRequest request) {
        String email = DEMO_ACCOUNTS.get(account);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        UserDetails user = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return "redirect:/dashboard";
    }
}
