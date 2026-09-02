package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.ProfileDTO;
import bd.edu.seu.jerseysee.dto.RegistrationDTO;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.UserRepository;
import java.util.Locale;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationDTO registration) {
        if (registration.getPassword() == null
                || !Objects.equals(registration.getPassword(), registration.getPasswordConfirmation())) {
            throw new IllegalArgumentException("Password confirmation does not match.");
        }

        String email = normalizeEmail(registration.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account already exists for this email.");
        }

        User user = new User();
        user.setName(registration.getName().trim());
        user.setEmail(email);
        user.setPhone(registration.getPhone().trim());
        user.setAddress(registration.getAddress().trim());
        user.setPassword(passwordEncoder.encode(registration.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("An account already exists for this email.", exception);
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("authentication.name == #email")
    public ProfileDTO getProfile(String email) {
        User user = findByEmail(email);
        ProfileDTO profile = new ProfileDTO();
        profile.setName(user.getName());
        profile.setPhone(user.getPhone());
        profile.setAddress(user.getAddress());
        return profile;
    }

    @Transactional
    @PreAuthorize("authentication.name == #email")
    public User updateProfile(String email, ProfileDTO profile) {
        User user = findByEmail(email);
        user.setName(profile.getName().trim());
        user.setPhone(profile.getPhone().trim());
        user.setAddress(profile.getAddress().trim());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getRequiredByEmail(String email) {
        return findByEmail(email);
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User account was not found."));
    }
}
