package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.UserRepository;
import bd.edu.seu.jerseysee.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    @ConditionalOnProperty(name = "app.seed-admin.enabled", havingValue = "true")
    CommandLineRunner seedAdministrator(UserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${app.seed-admin.email}") String email,
            @Value("${app.seed-admin.password}") String password) {
        return args -> {
            String normalizedEmail = UserService.normalizeEmail(email);
            validateSeedCredentials(normalizedEmail, password);
            if (userRepository.existsByEmail(normalizedEmail)) {
                return;
            }
            User administrator = new User();
            administrator.setName("System Administrator");
            administrator.setEmail(normalizedEmail);
            administrator.setPassword(passwordEncoder.encode(password));
            administrator.setPhone("N/A");
            administrator.setAddress("System account");
            administrator.setRole(Role.ADMIN);
            administrator.setEnabled(true);
            userRepository.save(administrator);
        };
    }

    private void validateSeedCredentials(String email, String password) {
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Seed administrator email must be valid.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Seed administrator password must not be blank.");
        }
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$")) {
            throw new IllegalArgumentException("Seed administrator password must meet password strength requirements.");
        }
    }
}
