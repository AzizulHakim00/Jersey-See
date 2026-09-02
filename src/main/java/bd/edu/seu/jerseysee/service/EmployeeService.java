package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.EmployeeDTO;
import bd.edu.seu.jerseysee.model.EmployeeProfile;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.EmployeeProfileRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class EmployeeService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(UserRepository userRepository, EmployeeProfileRepository employeeProfileRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public User create(EmployeeDTO employee) {
        Role creatorRole = currentCreatorRole();
        assertPermittedRole(creatorRole, employee.getRole());
        if (employee.getPassword() == null
                || !Objects.equals(employee.getPassword(), employee.getPasswordConfirmation())) {
            throw new IllegalArgumentException("Password confirmation does not match.");
        }

        String email = UserService.normalizeEmail(employee.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account already exists for this email.");
        }
        String employeeCode = employee.getEmployeeCode().trim().toUpperCase(Locale.ROOT);
        if (employeeProfileRepository.findByEmployeeCode(employeeCode).isPresent()) {
            throw new IllegalArgumentException("An employee already exists for this code.");
        }

        User user = new User();
        user.setName(employee.getName().trim());
        user.setEmail(email);
        user.setPhone(employee.getPhone().trim());
        user.setAddress(employee.getAddress().trim());
        user.setPassword(passwordEncoder.encode(employee.getPassword()));
        user.setRole(employee.getRole());
        user.setEnabled(true);

        EmployeeProfile profile = new EmployeeProfile();
        profile.setEmployeeCode(employeeCode);
        profile.setPosition(employee.getPosition().trim());
        profile.setSalary(employee.getSalary());
        profile.setJoiningDate(employee.getJoiningDate());
        profile.setActive(true);
        user.setEmployeeProfile(profile);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "A user or employee already exists for the supplied email or employee code.", exception);
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<EmployeeProfile> listAll() {
        return employeeProfileRepository.findAllWithUser();
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<Role> rolesAvailableToCurrentUser() {
        return currentCreatorRole() == Role.ADMIN
                ? List.of(Role.SALESMAN, Role.CASHIER, Role.MANAGER)
                : List.of(Role.SALESMAN, Role.CASHIER);
    }

    private Role currentCreatorRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            return Role.ADMIN;
        }
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_MANAGER"))) {
            return Role.MANAGER;
        }
        throw new AccessDeniedException("Only managers and administrators can manage staff.");
    }

    private void assertPermittedRole(Role creatorRole, Role requestedRole) {
        if (creatorRole == Role.MANAGER && requestedRole != Role.SALESMAN && requestedRole != Role.CASHIER) {
            throw new IllegalArgumentException("Managers may create only salesman or cashier accounts.");
        }
        if (requestedRole == Role.ADMIN) {
            throw new IllegalArgumentException("Administrator accounts cannot be created through staff management.");
        }
        if (creatorRole == Role.ADMIN && requestedRole != Role.SALESMAN && requestedRole != Role.CASHIER
                && requestedRole != Role.MANAGER) {
            throw new IllegalArgumentException("Staff accounts must have a staff role.");
        }
    }
}
