package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {
    private final AuthenticationConfiguration authentication;
    private final UserDetailsService users;
    private final JwtService tokens;
    public ApiAuthController(AuthenticationConfiguration authentication, UserDetailsService users, JwtService tokens) {
        this.authentication = authentication; this.users = users; this.tokens = tokens;
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login input) throws Exception {
        try {
            var result = authentication.getAuthenticationManager().authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(input.email(), input.password()));
            var user = users.loadUserByUsername(result.getName());
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(new Token(tokens.issue(user), "Bearer", JwtService.LIFETIME_SECONDS));
        } catch (AuthenticationException invalid) {
            return ResponseEntity.status(401).cacheControl(CacheControl.noStore())
                    .body(java.util.Map.of("error", "Invalid email or password"));
        }
    }
    @GetMapping("/me")
    public ResponseEntity<?> me(java.security.Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(java.util.Map.of("email", principal.getName()));
    }
    public record Login(@NotBlank String email, @NotBlank String password) {}
    public record Token(String accessToken, String tokenType, long expiresIn) {}
}
