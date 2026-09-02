package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.config.SecurityConfig;
import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.service.InvoiceService;
import bd.edu.seu.jerseysee.service.OrderService;
import bd.edu.seu.jerseysee.service.UserService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderOwnershipTest {

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "intruder@example.com", roles = "CUSTOMER")
    void customerCannotViewAnotherCustomersOrder() throws Exception {
        User intruder = user("intruder@example.com", Role.CUSTOMER);
        when(userService.getRequiredByEmail("intruder@example.com")).thenReturn(intruder);
        when(orderService.getAccessible(21L, intruder)).thenThrow(new AccessDeniedException("Order access denied."));

        mockMvc.perform(get("/orders/21"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "CUSTOMER")
    void ownerCanDownloadUtf8TextInvoiceWithSafeFilename() throws Exception {
        User owner = user("owner@example.com", Role.CUSTOMER);
        CustomerOrder order = new CustomerOrder();
        when(userService.getRequiredByEmail("owner@example.com")).thenReturn(owner);
        when(orderService.getAccessible(21L, owner)).thenReturn(order);
        when(invoiceService.createTextInvoice(order)).thenReturn("Invoice – আমিনা".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/orders/21/invoice"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"jerseysee-invoice-21.txt\""))
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().bytes("Invoice – আমিনা".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @WithMockUser(username = "cashier@example.com", roles = "CASHIER")
    void cashierCannotUpdateOrderWorkflow() throws Exception {
        mockMvc.perform(post("/staff/orders/21/status").with(csrf()).param("status", "CONFIRMED"))
                .andExpect(status().isForbidden());

        verify(orderService, never()).updateStatus(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private User user(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}
