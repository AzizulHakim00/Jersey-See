package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.config.SecurityConfig;
import bd.edu.seu.jerseysee.service.CartService;
import bd.edu.seu.jerseysee.service.ProductService;
import bd.edu.seu.jerseysee.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
@Import(SecurityConfig.class)
class ProductOperationsSecurityTest {

    @MockitoBean private ProductService productService;
    @MockitoBean private UserService userService;
    @MockitoBean private CartService cartService;

    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(productService.listForManagement(any(Pageable.class))).thenReturn(Page.empty());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotManageProducts() throws Exception {
        mockMvc.perform(get("/staff/products")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SALESMAN")
    void salesmanCannotManageProducts() throws Exception {
        mockMvc.perform(get("/staff/products")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void cashierCannotManageProducts() throws Exception {
        mockMvc.perform(get("/staff/products")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanManageProducts() throws Exception {
        mockMvc.perform(get("/staff/products")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorCanManageProducts() throws Exception {
        mockMvc.perform(get("/staff/products")).andExpect(status().isOk());
    }
}
