package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.EmployeeDTO;
import bd.edu.seu.jerseysee.dto.EmployeeUpdateDTO;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.EmployeeProfileRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EmployeeOperationsPersistenceTest {

    @Autowired private EmployeeService employeeService;
    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeProfileRepository employeeProfileRepository;

    @BeforeEach
    void authenticateAdministrator() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@operations-test.local", "unused",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createUpdateDisableEnableAndDeleteSalesmanPersistsCorrectly() {
        EmployeeDTO create = new EmployeeDTO();
        create.setName("Operations Salesman");
        create.setEmail("operations.salesman@test.local");
        create.setPhone("01700000001");
        create.setAddress("Dhaka");
        create.setPassword("Password1!");
        create.setPasswordConfirmation("Password1!");
        create.setRole(Role.SALESMAN);
        create.setEmployeeCode("OPS-SALES-001");
        create.setPosition("Salesman");
        create.setSalary(new BigDecimal("30000.00"));
        create.setJoiningDate(LocalDate.of(2026, 9, 1));

        User created = employeeService.create(create);
        Long employeeId = created.getEmployeeProfile().getId();

        assertThat(employeeId).isNotNull();
        assertThat(userRepository.findByEmail("operations.salesman@test.local")).isPresent();
        assertThat(employeeProfileRepository.findByEmployeeCode("OPS-SALES-001")).isPresent();

        EmployeeUpdateDTO update = employeeService.getUpdateForm(employeeId);
        update.setName("Updated Operations Salesman");
        update.setPosition("Senior Salesman");
        update.setSalary(new BigDecimal("35000.00"));
        update.setPassword("");
        update.setPasswordConfirmation("");
        employeeService.update(employeeId, update);

        User updated = userRepository.findByEmail("operations.salesman@test.local").orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Operations Salesman");
        assertThat(updated.getEmployeeProfile().getPosition()).isEqualTo("Senior Salesman");

        employeeService.setActive(employeeId, false);
        User disabled = userRepository.findByEmail("operations.salesman@test.local").orElseThrow();
        assertThat(disabled.isEnabled()).isFalse();
        assertThat(disabled.getEmployeeProfile().isActive()).isFalse();

        employeeService.setActive(employeeId, true);
        User enabled = userRepository.findByEmail("operations.salesman@test.local").orElseThrow();
        assertThat(enabled.isEnabled()).isTrue();
        assertThat(enabled.getEmployeeProfile().isActive()).isTrue();

        employeeService.delete(employeeId);
        assertThat(userRepository.findByEmail("operations.salesman@test.local")).isEmpty();
        assertThat(employeeProfileRepository.findByEmployeeCode("OPS-SALES-001")).isEmpty();
    }
}
