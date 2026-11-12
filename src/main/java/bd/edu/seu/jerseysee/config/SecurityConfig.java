package bd.edu.seu.jerseysee.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import bd.edu.seu.jerseysee.security.JwtService;
import bd.edu.seu.jerseysee.security.JwtCookieService;
import bd.edu.seu.jerseysee.security.JwtAuthFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Lazy(false)
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService tokens, JwtCookieService cookies,
            ObjectProvider<UserDetailsService> users) throws Exception {
        http.sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        // Rotate the form token on login rather than on each JWT-authenticated request.
                        .requireExplicitAuthenticationStrategy(true))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                // Browser forms use the session CSRF token. Authentication remains JWT-based.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .addFilterBefore(new JwtAuthFilter(tokens, users), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/catalog/**", "/products/**", "/product-images/**", "/register", "/login",
                                "/css/**", "/js/**", "/images/**", "/error", "/error/**", "/access-denied",
                                "/actuator/health", "/actuator/health/**", "/actuator/info", "/api/auth/login")
                        .permitAll()
                        .requestMatchers("/staff/employees/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/staff/**").hasAnyRole("SALESMAN", "CASHIER", "MANAGER", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            UserDetails user = users.getObject().loadUserByUsername(authentication.getName());
                            cookies.issue(response, tokens.issue(user));
                            redirect(request, response, "/dashboard");
                        })
                        .failureHandler((request, response, exception) -> redirect(request, response, "/login?error"))
                        .permitAll())
                .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access-denied")
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (request.getRequestURI().substring(request.getContextPath().length()).startsWith("/api/")) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            } else {
                                redirect(request, response, "/login");
                            }
                        }))
                .logout(logout -> logout.addLogoutHandler((request, response, authentication) -> cookies.clear(response))
                        .logoutSuccessHandler((request, response, authentication) -> redirect(request, response, "/login?logout"))
                        .permitAll());
        return http.build();
    }

    private static void redirect(HttpServletRequest request, HttpServletResponse response, String path) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(HttpHeaders.LOCATION, request.getContextPath() + path);
    }

    @Bean
    @Lazy(false)
    JwtService jwtService(Environment environment) {
        return new JwtService(environment.getProperty("app.jwt.secret-base64", ""),
                environment.acceptsProfiles(Profiles.of("production", "render-postgres")));
    }

    @Bean
    JwtCookieService jwtCookies(Environment environment) {
        boolean production = environment.acceptsProfiles(Profiles.of("production", "render-postgres"));
        return new JwtCookieService(environment.getProperty("app.auth.secure-cookies", Boolean.class, production));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
