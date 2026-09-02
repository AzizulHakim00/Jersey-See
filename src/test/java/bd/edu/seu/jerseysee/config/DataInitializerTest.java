package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void seedAdministratorRejectsBlankPasswordBeforePersistence() {
        DataInitializer initializer = new DataInitializer();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> initializer.seedAdministrator(userRepository, passwordEncoder,
                        "admin@example.com", "   ").run())
                .withMessage("Seed administrator password must not be blank.");

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void seedAdministratorRejectsPasswordThatDoesNotMeetRegistrationStrengthRequirements() {
        DataInitializer initializer = new DataInitializer();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> initializer.seedAdministrator(userRepository, passwordEncoder,
                        "admin@example.com", "weak").run())
                .withMessage("Seed administrator password must meet password strength requirements.");

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void seedAdministratorRejectsBlankOrInvalidEmailBeforePersistence() {
        DataInitializer initializer = new DataInitializer();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> initializer.seedAdministrator(userRepository, passwordEncoder,
                        "   ", "Password1!").run())
                .withMessage("Seed administrator email must be valid.");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> initializer.seedAdministrator(userRepository, passwordEncoder,
                        "admin@", "Password1!").run())
                .withMessage("Seed administrator email must be valid.");

        verifyNoInteractions(userRepository, passwordEncoder);
    }
}
