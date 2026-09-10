package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.EmployeeUpdateDTO;
import bd.edu.seu.jerseysee.model.EmployeeProfile;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.EmployeeProfileRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceLifecycleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeProfileRepository employeeProfileRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        employeeService = new EmployeeService(userRepository, employeeProfileRepository, passwordEncoder);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void administratorCanUpdateStaffWithoutResettingPassword() {
        authenticate("admin@demo.local", Role.ADMIN);
        EmployeeProfile profile = staffProfile(Role.SALESMAN, "sales@example.com");
        String originalHash = passwordEncoder.encode("Original1!");
        profile.getUser().setPassword(originalHash);
        stubExistingProfile(42L, profile);

        EmployeeUpdateDTO update = updateFor(profile);
        update.setName("Updated Sales Person");
        update.setPassword("");
        update.setPasswordConfirmation("");

        User saved = employeeService.update(42L, update);

        assertThat(saved.getName()).isEqualTo("Updated Sales Person");
        assertThat(saved.getPassword()).isEqualTo(originalHash);
        assertThat(saved.getRole()).isEqualTo(Role.SALESMAN);
    }

    @Test
    void administratorCanRotateStaffPasswordDuringUpdate() {
        authenticate("admin@demo.local", Role.ADMIN);
        EmployeeProfile profile = staffProfile(Role.CASHIER, "cashier@example.com");
        profile.getUser().setPassword(passwordEncoder.encode("Original1!"));
        stubExistingProfile(43L, profile);

        EmployeeUpdateDTO update = updateFor(profile);
        update.setPassword("Replacement2!");
        update.setPasswordConfirmation("Replacement2!");

        User saved = employeeService.update(43L, update);

        assertThat(passwordEncoder.matches("Replacement2!", saved.getPassword())).isTrue();
    }

    @Test
    void managerCannotManageManagerAccount() {
        authenticate("manager@example.com", Role.MANAGER);
        EmployeeProfile profile = staffProfile(Role.MANAGER, "other.manager@example.com");
        when(employeeProfileRepository.findById(44L)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> employeeService.setActive(44L, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Managers may manage only salesman or cashier accounts.");
    }

    @Test
    void disablingStaffSynchronizesEmployeeAndLoginState() {
        authenticate("manager@example.com", Role.MANAGER);
        EmployeeProfile profile = staffProfile(Role.SALESMAN, "sales@example.com");
        when(employeeProfileRepository.findById(45L)).thenReturn(Optional.of(profile));
        when(userRepository.saveAndFlush(profile.getUser())).thenReturn(profile.getUser());

        employeeService.setActive(45L, false);

        assertThat(profile.isActive()).isFalse();
        assertThat(profile.getUser().isEnabled()).isFalse();
        verify(userRepository).saveAndFlush(profile.getUser());
    }

    @Test
    void staffManagerCannotDisableOwnLogin() {
        authenticate("manager@example.com", Role.MANAGER);
        EmployeeProfile profile = staffProfile(Role.SALESMAN, "manager@example.com");
        when(employeeProfileRepository.findById(46L)).thenReturn(Optional.of(profile));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> employeeService.setActive(46L, false))
                .withMessage("You cannot disable your own staff account.");
    }

    @Test
    void administratorCanRemoveManagedStaffAccount() {
        authenticate("admin@demo.local", Role.ADMIN);
        EmployeeProfile profile = staffProfile(Role.SALESMAN, "remove@example.com");
        when(employeeProfileRepository.findById(47L)).thenReturn(Optional.of(profile));

        employeeService.delete(47L);

        verify(userRepository).delete(profile.getUser());
        verify(userRepository).flush();
    }

    @Test
    void managerCannotRemoveStaffAccountsEvenWhenCalledDirectly() {
        authenticate("manager@example.com", Role.MANAGER);

        assertThatThrownBy(() -> employeeService.delete(48L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only administrators can remove staff accounts.");
    }

    private void stubExistingProfile(Long id, EmployeeProfile profile) {
        when(employeeProfileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(userRepository.findByEmail(profile.getUser().getEmail())).thenReturn(Optional.of(profile.getUser()));
        when(employeeProfileRepository.findByEmployeeCode(profile.getEmployeeCode())).thenReturn(Optional.of(profile));
        when(userRepository.saveAndFlush(profile.getUser())).thenReturn(profile.getUser());
    }

    private EmployeeProfile staffProfile(Role role, String email) {
        User user = new User();
        user.setName("Staff Person");
        user.setEmail(email);
        user.setPhone("01800000000");
        user.setAddress("Dhaka");
        user.setPassword(passwordEncoder.encode("Password1!"));
        user.setRole(role);
        user.setEnabled(true);

        EmployeeProfile profile = new EmployeeProfile();
        profile.setEmployeeCode("EMP-42");
        profile.setPosition(role == Role.CASHIER ? "Cashier" : "Salesman");
        profile.setSalary(new BigDecimal("30000.00"));
        profile.setJoiningDate(LocalDate.of(2026, 9, 1));
        profile.setActive(true);
        user.setEmployeeProfile(profile);
        return profile;
    }

    private EmployeeUpdateDTO updateFor(EmployeeProfile profile) {
        User user = profile.getUser();
        EmployeeUpdateDTO update = new EmployeeUpdateDTO();
        update.setName(user.getName());
        update.setEmail(user.getEmail());
        update.setPhone(user.getPhone());
        update.setAddress(user.getAddress());
        update.setRole(user.getRole());
        update.setEmployeeCode(profile.getEmployeeCode());
        update.setPosition(profile.getPosition());
        update.setSalary(profile.getSalary());
        update.setJoiningDate(profile.getJoiningDate());
        return update;
    }

    private void authenticate(String email, Role role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email, "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
