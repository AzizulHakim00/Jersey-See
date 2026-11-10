package bd.edu.seu.jerseysee.config;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Lazy(false)
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService tokens, JwtCookieService cookies,
            ObjectProvider<UserDetailsService> users) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .csrf(csrf -> csrf.csrfTokenRepository(new CookieCsrfTokenRepository())
                        .ignoringRequestMatchers("/api/**"))
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
                            response.sendRedirect(request.getContextPath() + "/dashboard");
                        })
                        .permitAll())
                .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access-denied")
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (request.getRequestURI().substring(request.getContextPath().length()).startsWith("/api/")) response.sendError(401);
                            else response.sendRedirect(request.getContextPath() + "/login");
                        }))
                .logout(logout -> logout.addLogoutHandler((request, response, authentication) -> cookies.clear(response))
                        .logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
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
