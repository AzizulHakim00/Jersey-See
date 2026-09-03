package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.EmployeeDTO;
import bd.edu.seu.jerseysee.dto.RegistrationDTO;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.EmployeeProfileRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeProfileRepository employeeProfileRepository;

    private UserService userService;
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
        employeeService = new EmployeeService(userRepository, employeeProfileRepository, passwordEncoder);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndAlwaysAssignsCustomerRole() {
        stubSuccessfulUserSave();
        RegistrationDTO registration = registration("  AMINA@Example.COM  ");

        User saved = userService.register(registration);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(saved).isSameAs(userCaptor.getValue());
        assertThat(saved.getEmail()).isEqualTo("amina@example.com");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getPassword()).isNotEqualTo("Password1!");
        assertThat(new BCryptPasswordEncoder().matches("Password1!", saved.getPassword())).isTrue();
    }

    @Test
    void registerRejectsExistingNormalizedEmail() {
        when(userRepository.existsByEmail("amina@example.com")).thenReturn(true);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> userService.register(registration("AMINA@example.com")))
                .withMessage("An account already exists for this email.");
    }

    @Test
    void registerRejectsMismatchedPasswordConfirmation() {
        RegistrationDTO registration = registration("amina@example.com");
        registration.setPasswordConfirmation("Different1!");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> userService.register(registration))
                .withMessage("Password confirmation does not match.");
    }

    @Test
    void registerRejectsNullPasswordInsteadOfThrowingNullPointerException() {
        RegistrationDTO registration = registration("amina@example.com");
        registration.setPassword(null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> userService.register(registration))
                .withMessage("Password confirmation does not match.");
    }

    @Test
    void registerConvertsUniqueConstraintRaceToDuplicateEmailRejection() {
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> userService.register(registration("amina@example.com")))
                .withMessage("An account already exists for this email.");
    }

    @Test
    void managerCanCreateCashierWithLinkedEmployeeProfile() {
        stubSuccessfulUserSave();
        authenticateAs(Role.MANAGER);
        EmployeeDTO employee = employee(Role.CASHIER);

        User created = employeeService.create(employee);

        assertThat(created.getRole()).isEqualTo(Role.CASHIER);
        assertThat(created.getEmployeeProfile()).isNotNull();
        assertThat(created.getEmployeeProfile().getEmployeeCode()).isEqualTo("EMP-100");
        assertThat(created.getEmployeeProfile().getUser()).isSameAs(created);
        assertThat(new BCryptPasswordEncoder().matches("Password1!", created.getPassword())).isTrue();
    }

    @Test
    void managerCannotCreateManagerOrAdministrator() {
        authenticateAs(Role.MANAGER);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> employeeService.create(employee(Role.MANAGER)))
                .withMessage("Managers may create only salesman or cashier accounts.");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> employeeService.create(employee(Role.ADMIN)))
                .withMessage("Managers may create only salesman or cashier accounts.");
    }

    @Test
    void administratorCannotCreateAnotherAdministrator() {
        authenticateAs(Role.ADMIN);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> employeeService.create(employee(Role.ADMIN)))
                .withMessage("Administrator accounts cannot be created through staff management.");
    }

    @Test
    void employeeCreationRejectsNullPasswordInsteadOfThrowingNullPointerException() {
        authenticateAs(Role.MANAGER);
        EmployeeDTO employee = employee(Role.CASHIER);
        employee.setPassword(null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> employeeService.create(employee))
                .withMessage("Password confirmation does not match.");
    }

    private RegistrationDTO registration(String email) {
        RegistrationDTO registration = new RegistrationDTO();
        registration.setName("Amina Rahman");
        registration.setEmail(email);
        registration.setPhone("01700000000");
        registration.setAddress("Dhaka");
        registration.setPassword("Password1!");
        registration.setPasswordConfirmation("Password1!");
        return registration;
    }

    private EmployeeDTO employee(Role role) {
        EmployeeDTO employee = new EmployeeDTO();
        employee.setName("Rahim Uddin");
        employee.setEmail("rahim@example.com");
        employee.setPhone("01800000000");
        employee.setAddress("Dhaka");
        employee.setPassword("Password1!");
        employee.setPasswordConfirmation("Password1!");
        employee.setRole(role);
        employee.setEmployeeCode("EMP-100");
        employee.setPosition("Cashier");
        employee.setSalary(new BigDecimal("25000.00"));
        employee.setJoiningDate(LocalDate.of(2026, 9, 1));
        return employee;
    }

    private void authenticateAs(Role role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "manager@example.com", "unused",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private void stubSuccessfulUserSave() {
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
