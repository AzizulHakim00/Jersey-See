package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.config.SecurityConfig;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.service.CartService;
import bd.edu.seu.jerseysee.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CartController.class, ProfileController.class})
@Import(SecurityConfig.class)
class DemoCustomerOperationsTest {

    private static final String DEMO_CUSTOMER = "customer@demo.local";

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    private User customer;

    @BeforeEach
    void configureDemoCustomer() {
        customer = new User();
        customer.setName("Demo Customer");
        customer.setEmail(DEMO_CUSTOMER);
        customer.setRole(Role.CUSTOMER);
        customer.setEnabled(true);
        when(userService.getRequiredByEmail(DEMO_CUSTOMER)).thenReturn(customer);
    }

    @Test
    @WithMockUser(username = DEMO_CUSTOMER, roles = "CUSTOMER")
    void demoCustomerCanAddProductToBagUsingAuthenticatedDatabaseOwner() throws Exception {
        mockMvc.perform(post("/cart/items").with(csrf())
                        .param("variantId", "1")
                        .param("quantity", "1")
                        .param("printingType", "NONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart?added"));

        verify(cartService).add(eq(customer), any());
    }

    @Test
    @WithMockUser(username = DEMO_CUSTOMER, roles = "CUSTOMER")
    void demoCustomerCanUpdateProfile() throws Exception {
        mockMvc.perform(post("/profile").with(csrf())
                        .param("name", "Demo Customer Updated")
                        .param("phone", "01700000000")
                        .param("address", "Dhaka, Bangladesh"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?updated"));

        verify(userService).updateProfile(eq(DEMO_CUSTOMER), any());
    }
}
