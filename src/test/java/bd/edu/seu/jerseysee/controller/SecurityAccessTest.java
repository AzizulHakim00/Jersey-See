package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;

@WebMvcTest({DashboardController.class, EmployeeController.class, AuthController.class})
@Import(SecurityConfig.class)
class SecurityAccessTest {

    @MockitoBean
    private bd.edu.seu.jerseysee.service.UserService userService;

    @MockitoBean
    private bd.edu.seu.jerseysee.service.EmployeeService employeeService;

    @MockitoBean
    private bd.edu.seu.jerseysee.service.DashboardService dashboardService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousUsersAreRedirectedFromDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customersCannotAccessStaffManagement() throws Exception {
        mockMvc.perform(get("/staff/employees"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managersCanAccessStaffManagement() throws Exception {
        mockMvc.perform(get("/staff/employees"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void staffCreationPostRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/staff/employees"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/staff/employees").with(csrf())
                        .param("name", "Rahim Uddin")
                        .param("email", "rahim@example.com")
                        .param("phone", "01800000000")
                        .param("address", "Dhaka")
                        .param("password", "Password1!")
                        .param("passwordConfirmation", "Password1!")
                        .param("role", "CASHIER")
                        .param("employeeCode", "EMP-100")
                        .param("position", "Cashier")
                        .param("salary", "25000.00")
                        .param("joiningDate", "2026-09-01"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void registrationCanonicalizesWhitespaceEmailBeforeValidationAndServiceUse() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "Amina Rahman")
                        .param("email", "  AMINA@Example.COM  ")
                        .param("phone", "01700000000")
                        .param("address", "Dhaka")
                        .param("password", "Password1!")
                        .param("passwordConfirmation", "Password1!"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<bd.edu.seu.jerseysee.dto.RegistrationDTO> registration = ArgumentCaptor.forClass(
                bd.edu.seu.jerseysee.dto.RegistrationDTO.class);
        verify(userService).register(registration.capture());
        org.assertj.core.api.Assertions.assertThat(registration.getValue().getEmail()).isEqualTo("AMINA@Example.COM");
    }
}
