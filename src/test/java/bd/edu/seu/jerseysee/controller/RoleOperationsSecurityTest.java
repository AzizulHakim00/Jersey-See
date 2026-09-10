package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.config.SecurityConfig;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.service.CartService;
import bd.edu.seu.jerseysee.service.InvoiceService;
import bd.edu.seu.jerseysee.service.OrderService;
import bd.edu.seu.jerseysee.service.PaymentService;
import bd.edu.seu.jerseysee.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OrderController.class, PaymentController.class})
@Import(SecurityConfig.class)
class RoleOperationsSecurityTest {

    @MockitoBean private OrderService orderService;
    @MockitoBean private InvoiceService invoiceService;
    @MockitoBean private UserService userService;
    @MockitoBean private CartService cartService;
    @MockitoBean private PaymentService paymentService;

    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(userService.getRequiredByEmail(anyString())).thenAnswer(invocation -> user(
                invocation.getArgument(0), roleFor(invocation.getArgument(0))));
        when(orderService.listFor(any(User.class))).thenReturn(List.of());
        when(paymentService.listFor(any(User.class))).thenReturn(List.of());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void customerCannotAccessStaffOrders() throws Exception {
        mockMvc.perform(get("/staff/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "salesman@example.com", roles = "SALESMAN")
    void salesmanCanReadAndUpdateOrders() throws Exception {
        mockMvc.perform(get("/staff/orders"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/staff/orders/1/status").with(csrf()).param("status", "PROCESSING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/orders/1?statusUpdated"));
    }

    @Test
    @WithMockUser(username = "salesman@example.com", roles = "SALESMAN")
    void salesmanCannotAccessPayments() throws Exception {
        mockMvc.perform(get("/staff/payments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cashier@example.com", roles = "CASHIER")
    void cashierCanReadAndConfirmPayments() throws Exception {
        mockMvc.perform(get("/staff/payments"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/staff/payments/1/confirm").with(csrf()).param("transactionId", "TX-100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/payments?confirmed"));
    }

    @Test
    @WithMockUser(username = "cashier@example.com", roles = "CASHIER")
    void cashierCannotChangeOrderStatus() throws Exception {
        mockMvc.perform(post("/staff/orders/1/status").with(csrf()).param("status", "PROCESSING"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void managerCanReadPaymentsAndUpdateOrdersButCannotConfirmPayments() throws Exception {
        mockMvc.perform(get("/staff/payments"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/staff/orders/1/status").with(csrf()).param("status", "PROCESSING"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/staff/payments/1/confirm").with(csrf()).param("transactionId", "TX-200"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
    void administratorCanUpdateOrdersAndConfirmPayments() throws Exception {
        mockMvc.perform(post("/staff/orders/1/status").with(csrf()).param("status", "PROCESSING"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/staff/payments/1/confirm").with(csrf()).param("transactionId", "TX-300"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/payments?confirmed"));
    }

    private Role roleFor(String email) {
        if (email.startsWith("salesman")) return Role.SALESMAN;
        if (email.startsWith("cashier")) return Role.CASHIER;
        if (email.startsWith("manager")) return Role.MANAGER;
        if (email.startsWith("admin")) return Role.ADMIN;
        return Role.CUSTOMER;
    }

    private User user(String email, Role role) {
        User user = new User();
        user.setName(role.name());
        user.setEmail(email);
        user.setPhone("01700000000");
        user.setAddress("Dhaka");
        user.setPassword("unused");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}
