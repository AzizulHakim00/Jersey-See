package bd.edu.seu.jerseysee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, PublicDemoAdminReadOnlyFilter publicDemoAdminReadOnlyFilter)
            throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/catalog/**", "/products/**", "/product-images/**", "/register", "/login",
                                "/demo-login/**", "/css/**", "/js/**", "/images/**", "/error", "/error/**", "/access-denied",
                                "/actuator/health", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/staff/employees/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/staff/**").hasAnyRole("SALESMAN", "CASHIER", "MANAGER", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/dashboard", true).permitAll())
                .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access-denied"))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
                .addFilterBefore(publicDemoAdminReadOnlyFilter, AuthorizationFilter.class);
        return http.build();
    }

    @Bean
    PublicDemoAdminReadOnlyFilter publicDemoAdminReadOnlyFilter() {
        return new PublicDemoAdminReadOnlyFilter();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
