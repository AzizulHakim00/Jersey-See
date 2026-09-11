package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.config.SecurityConfig;
import bd.edu.seu.jerseysee.service.CartService;
import bd.edu.seu.jerseysee.service.EmployeeService;
import bd.edu.seu.jerseysee.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@Import(SecurityConfig.class)
class EmployeeLifecycleWebTest {

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CartService cartService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void managerCanUpdateSalesmanOrCashierProfile() throws Exception {
        mockMvc.perform(post("/staff/employees/42").with(csrf())
                        .param("name", "Sales Person")
                        .param("email", "sales@example.com")
                        .param("phone", "01800000000")
                        .param("address", "Dhaka")
                        .param("role", "SALESMAN")
                        .param("employeeCode", "EMP-42")
                        .param("position", "Salesman")
                        .param("salary", "30000.00")
                        .param("joiningDate", "2026-09-01")
                        .param("password", "")
                        .param("passwordConfirmation", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/employees?updated"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void managerCanDisableManagedStaffAccount() throws Exception {
        mockMvc.perform(post("/staff/employees/42/status").with(csrf())
                        .param("active", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/employees?disabled"));
    }

    @Test
    @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
    void administratorCanReEnableStaffAccount() throws Exception {
        mockMvc.perform(post("/staff/employees/42/status").with(csrf())
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/employees?enabled"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void managerCannotDeleteStaffAccounts() throws Exception {
        mockMvc.perform(post("/staff/employees/42/delete").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
    void administratorCanDeleteStaffAccount() throws Exception {
        mockMvc.perform(post("/staff/employees/42/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/employees?deleted"));
    }
}
