package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.cart.ShoppingCart;
import bd.edu.seu.jerseysee.config.SecurityConfig;
import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.service.InvoiceService;
import bd.edu.seu.jerseysee.service.OrderService;
import bd.edu.seu.jerseysee.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void checkoutClearsSessionCartOnlyAfterSuccessfulServiceReturn() throws Exception {
        User customer = customer();
        ShoppingCart cart = mock(ShoppingCart.class);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("shoppingCart", cart);
        CustomerOrder saved = new CustomerOrder();
        ReflectionTestUtils.setField(saved, "id", 21L);
        when(userService.getRequiredByEmail("customer@example.com")).thenReturn(customer);
        when(orderService.checkout(eq(customer), eq(cart), any())).thenReturn(saved);

        mockMvc.perform(post("/checkout").session(session).with(csrf())
                        .param("deliveryRecipientName", "Amina Rahman")
                        .param("deliveryPhone", "01700000000")
                        .param("deliveryAddress", "Dhaka")
                        .param("paymentMethod", "CASH_ON_DELIVERY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/21?created"));

        verify(cart).clear();
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void failedCheckoutPreservesSessionCartAndFormInput() throws Exception {
        User customer = customer();
        ShoppingCart cart = mock(ShoppingCart.class);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("shoppingCart", cart);
        when(userService.getRequiredByEmail("customer@example.com")).thenReturn(customer);
        when(orderService.checkout(eq(customer), eq(cart), any()))
                .thenThrow(new IllegalArgumentException("Insufficient stock for NAT-L."));

        mockMvc.perform(post("/checkout").session(session).with(csrf())
                        .param("deliveryRecipientName", "Amina Rahman")
                        .param("deliveryPhone", "01700000000")
                        .param("deliveryAddress", "Dhaka")
                        .param("paymentMethod", "CASH_ON_DELIVERY"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/checkout"));

        verify(cart, never()).clear();
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void repeatedCancellationReturnsUserFacingErrorInsteadOfServerError() throws Exception {
        User customer = customer();
        when(userService.getRequiredByEmail("customer@example.com")).thenReturn(customer);
        when(orderService.cancel(21L, customer))
                .thenThrow(new IllegalArgumentException("This order can no longer be cancelled."));

        mockMvc.perform(post("/orders/21/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/21?error"))
                .andExpect(flash().attribute("error", "This order can no longer be cancelled."));
    }

    @Test
    @WithMockUser(username = "sales@example.com", roles = "SALESMAN")
    void illegalWorkflowTransitionReturnsUserFacingErrorInsteadOfServerError() throws Exception {
        User salesman = new User();
        salesman.setEmail("sales@example.com");
        salesman.setRole(Role.SALESMAN);
        salesman.setEnabled(true);
        when(userService.getRequiredByEmail("sales@example.com")).thenReturn(salesman);
        when(orderService.updateStatus(org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(salesman)))
                .thenThrow(new IllegalArgumentException("Order cannot move from PENDING to SHIPPED."));

        mockMvc.perform(post("/staff/orders/21/status").with(csrf()).param("status", "SHIPPED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/orders/21?error"))
                .andExpect(flash().attribute("error", "Order cannot move from PENDING to SHIPPED."));
    }

    private User customer() {
        User user = new User();
        user.setEmail("customer@example.com");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        return user;
    }
}
