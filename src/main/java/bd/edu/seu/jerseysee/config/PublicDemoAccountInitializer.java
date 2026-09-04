package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.UserRepository;
import bd.edu.seu.jerseysee.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"demo", "production"})
@ConditionalOnProperty(name = "app.public-demo.enabled", havingValue = "true")
public class PublicDemoAccountInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public PublicDemoAccountInitializer(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.public-demo.password}") String demoPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialize() {
        ensureUser("public.demo.customer", "Demo Customer", "customer@jerseysee.demo", Role.CUSTOMER);
        ensureUser("public.demo.admin", "Demo Administrator", "admin@jerseysee.demo", Role.ADMIN);
    }

    private void ensureUser(String seedKey, String name, String email, Role role) {
        String normalizedEmail = UserService.normalizeEmail(email);
        User user = userRepository.findByDemoSeedKey(seedKey).orElse(null);
        User emailOwner = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null && emailOwner != null) {
            if (emailOwner.getRole() != role) {
                throw new IllegalStateException("Public demo email belongs to a different role.");
            }
            user = emailOwner;
            user.setDemoSeedKey(seedKey);
        }
        if (user == null) {
            user = new User();
            user.setDemoSeedKey(seedKey);
        }
        user.setName(name);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(demoPassword));
        user.setPhone("01700000000");
        user.setAddress("Public JerseySee portfolio demo account");
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }
}
