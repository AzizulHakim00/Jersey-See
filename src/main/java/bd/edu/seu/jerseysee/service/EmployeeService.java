package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.EmployeeDTO;
import bd.edu.seu.jerseysee.dto.EmployeeUpdateDTO;
import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.model.EmployeeProfile;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.EmployeeProfileRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        assertPermittedRoleForCreation(creatorRole, employee.getRole());
        if (employee.getPassword() == null
                || !Objects.equals(employee.getPassword(), employee.getPasswordConfirmation())) {
            throw new IllegalArgumentException("Password confirmation does not match.");
        }

        String email = UserService.normalizeEmail(employee.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account already exists for this email.");
        }
        String employeeCode = normalizeEmployeeCode(employee.getEmployeeCode());
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
            throw duplicateStaffException(exception);
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<EmployeeProfile> listAll() {
        return employeeProfileRepository.findAllWithUser();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public EmployeeUpdateDTO getUpdateForm(Long employeeId) {
        EmployeeProfile profile = getRequiredProfile(employeeId);
        Role creatorRole = currentCreatorRole();
        assertCanManageExistingRole(creatorRole, profile.getUser().getRole());
        return toUpdateDTO(profile);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public User update(Long employeeId, EmployeeUpdateDTO employee) {
        EmployeeProfile profile = getRequiredProfile(employeeId);
        User user = profile.getUser();
        Role creatorRole = currentCreatorRole();
        assertCanManageExistingRole(creatorRole, user.getRole());
        assertPermittedRoleForUpdate(creatorRole, employee.getRole());
        assertNotCurrentUser(user, "You cannot change your own staff role or account details here. Use your profile page instead.");

        String email = UserService.normalizeEmail(employee.getEmail());
        userRepository.findByEmail(email)
                .filter(existing -> existing != user)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("An account already exists for this email.");
                });

        String employeeCode = normalizeEmployeeCode(employee.getEmployeeCode());
        employeeProfileRepository.findByEmployeeCode(employeeCode)
                .filter(existing -> existing != profile)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("An employee already exists for this code.");
                });

        String password = employee.getPassword() == null ? "" : employee.getPassword();
        String confirmation = employee.getPasswordConfirmation() == null ? "" : employee.getPasswordConfirmation();
        if (password.isBlank() && !confirmation.isBlank()) {
            throw new IllegalArgumentException("Enter the new password before confirming it.");
        }
        if (!password.isBlank() && !Objects.equals(password, confirmation)) {
            throw new IllegalArgumentException("Password confirmation does not match.");
        }

        user.setName(employee.getName().trim());
        user.setEmail(email);
        user.setPhone(employee.getPhone().trim());
        user.setAddress(employee.getAddress().trim());
        user.setRole(employee.getRole());
        if (!password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        profile.setEmployeeCode(employeeCode);
        profile.setPosition(employee.getPosition().trim());
        profile.setSalary(employee.getSalary());
        profile.setJoiningDate(employee.getJoiningDate());

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateStaffException(exception);
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void setActive(Long employeeId, boolean active) {
        EmployeeProfile profile = getRequiredProfile(employeeId);
        User user = profile.getUser();
        Role creatorRole = currentCreatorRole();
        assertCanManageExistingRole(creatorRole, user.getRole());
        assertNotCurrentUser(user, "You cannot disable your own staff account.");

        profile.setActive(active);
        user.setEnabled(active);
        userRepository.saveAndFlush(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long employeeId) {
        if (currentCreatorRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only administrators can remove staff accounts.");
        }
        EmployeeProfile profile = getRequiredProfile(employeeId);
        User user = profile.getUser();
        assertCanManageExistingRole(Role.ADMIN, user.getRole());
        assertNotCurrentUser(user, "You cannot remove your own account.");
        userRepository.delete(user);
        userRepository.flush();
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<Role> rolesAvailableToCurrentUser() {
        return currentCreatorRole() == Role.ADMIN
                ? List.of(Role.SALESMAN, Role.CASHIER, Role.MANAGER)
                : List.of(Role.SALESMAN, Role.CASHIER);
    }

    private EmployeeProfile getRequiredProfile(Long employeeId) {
        return employeeProfileRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found."));
    }

    private EmployeeUpdateDTO toUpdateDTO(EmployeeProfile profile) {
        User user = profile.getUser();
        EmployeeUpdateDTO employee = new EmployeeUpdateDTO();
        employee.setName(user.getName());
        employee.setEmail(user.getEmail());
        employee.setPhone(user.getPhone());
        employee.setAddress(user.getAddress());
        employee.setRole(user.getRole());
        employee.setEmployeeCode(profile.getEmployeeCode());
        employee.setPosition(profile.getPosition());
        employee.setSalary(profile.getSalary());
        employee.setJoiningDate(profile.getJoiningDate());
        return employee;
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

    private void assertPermittedRoleForCreation(Role creatorRole, Role requestedRole) {
        if (creatorRole == Role.MANAGER && requestedRole != Role.SALESMAN && requestedRole != Role.CASHIER) {
            throw new IllegalArgumentException("Managers may create only salesman or cashier accounts.");
        }
        if (requestedRole == Role.ADMIN) {
            throw new IllegalArgumentException("Administrator accounts cannot be created through staff management.");
        }
        if (creatorRole == Role.ADMIN && !isManagedStaffRole(requestedRole)) {
            throw new IllegalArgumentException("Staff accounts must have a staff role.");
        }
    }

    private void assertPermittedRoleForUpdate(Role creatorRole, Role requestedRole) {
        if (requestedRole == null || requestedRole == Role.ADMIN || requestedRole == Role.CUSTOMER) {
            throw new IllegalArgumentException("Staff accounts must keep a permitted staff role.");
        }
        if (creatorRole == Role.MANAGER && requestedRole != Role.SALESMAN && requestedRole != Role.CASHIER) {
            throw new AccessDeniedException("Managers may manage only salesman or cashier accounts.");
        }
        if (creatorRole == Role.ADMIN && !isManagedStaffRole(requestedRole)) {
            throw new IllegalArgumentException("Staff accounts must keep a permitted staff role.");
        }
    }

    private void assertCanManageExistingRole(Role creatorRole, Role targetRole) {
        if (targetRole == Role.ADMIN || targetRole == Role.CUSTOMER || targetRole == null) {
            throw new AccessDeniedException("This account cannot be managed through staff management.");
        }
        if (creatorRole == Role.MANAGER && targetRole != Role.SALESMAN && targetRole != Role.CASHIER) {
            throw new AccessDeniedException("Managers may manage only salesman or cashier accounts.");
        }
        if (creatorRole == Role.ADMIN && !isManagedStaffRole(targetRole)) {
            throw new AccessDeniedException("This account cannot be managed through staff management.");
        }
    }

    private boolean isManagedStaffRole(Role role) {
        return role == Role.SALESMAN || role == Role.CASHIER || role == Role.MANAGER;
    }

    private void assertNotCurrentUser(User target, String message) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && target.getEmail() != null
                && target.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizeEmployeeCode(String employeeCode) {
        return employeeCode.trim().toUpperCase(Locale.ROOT);
    }

    private IllegalArgumentException duplicateStaffException(DataIntegrityViolationException exception) {
        return new IllegalArgumentException(
                "A user or employee already exists for the supplied email or employee code.", exception);
    }
}
