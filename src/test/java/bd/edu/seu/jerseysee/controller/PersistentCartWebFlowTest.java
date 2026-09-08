package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.cart.ShoppingCart;
import bd.edu.seu.jerseysee.config.SecurityConfig;
import bd.edu.seu.jerseysee.dto.CheckoutDTO;
import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.service.CartService;
import bd.edu.seu.jerseysee.service.InvoiceService;
import bd.edu.seu.jerseysee.service.OrderService;
import bd.edu.seu.jerseysee.service.UserService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({CartController.class, OrderController.class})
@Import({SecurityConfig.class, GlobalModelAttributes.class})
class PersistentCartWebFlowTest {

    private static final String EMAIL = "persistent-web@example.com";

    @MockitoBean private CartService cartService;
    @MockitoBean private OrderService orderService;
    @MockitoBean private InvoiceService invoiceService;
    @MockitoBean private UserService userService;

    @Autowired private MockMvc mockMvc;

    private User customer;

    @BeforeEach
    void setUp() {
        customer = new User();
        ReflectionTestUtils.setField(customer, "id", 41L);
        customer.setName("Persistent Web Customer");
        customer.setEmail(EMAIL);
        customer.setRole(Role.CUSTOMER);
        customer.setEnabled(true);
        when(userService.getRequiredByEmail(EMAIL)).thenReturn(customer);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void cartProjectionIsReloadedFromDatabaseForEveryBrowserSession() throws Exception {
        ShoppingCart databaseCart = mock(ShoppingCart.class);
        when(cartService.getCart(customer)).thenReturn(databaseCart);

        MockHttpSession firstSession = new MockHttpSession();
        mockMvc.perform(get("/cart").session(firstSession))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/view"))
                .andExpect(model().attribute("shoppingCart", databaseCart));

        MockHttpSession replacementSession = new MockHttpSession();
        mockMvc.perform(get("/cart").session(replacementSession))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/view"))
                .andExpect(model().attribute("shoppingCart", databaseCart));

        verify(cartService, times(2)).getCart(customer);
        assertThat(firstSession.getAttribute("shoppingCart")).isNull();
        assertThat(replacementSession.getAttribute("shoppingCart")).isNull();
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void navbarCountUsesPersistentCartInsteadOfStaleSessionState() throws Exception {
        ShoppingCart databaseCart = mock(ShoppingCart.class);
        ShoppingCart staleSessionCart = mock(ShoppingCart.class);
        when(staleSessionCart.getTotalQuantity()).thenReturn(99);
        when(cartService.getCart(customer)).thenReturn(databaseCart);
        when(cartService.getTotalQuantity(customer)).thenReturn(3);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("shoppingCart", staleSessionCart);

        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cartCount", 3));

        verify(cartService).getTotalQuantity(customer);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void checkoutFormReloadsPersistentCartEvenWhenSessionContainsAnotherCart() throws Exception {
        ShoppingCart databaseCart = mock(ShoppingCart.class);
        ShoppingCart staleSessionCart = mock(ShoppingCart.class);
        when(cartService.getCart(customer)).thenReturn(databaseCart);
        when(cartService.getTotalQuantity(customer)).thenReturn(2);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("shoppingCart", staleSessionCart);

        mockMvc.perform(get("/checkout").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/checkout"))
                .andExpect(model().attribute("shoppingCart", databaseCart));

        verify(cartService).getCart(customer);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void successfulCheckoutUsesAuthenticatedCustomerAndDatabaseCartOnly() throws Exception {
        ShoppingCart databaseCart = mock(ShoppingCart.class);
        when(cartService.getCart(customer)).thenReturn(databaseCart);
        when(cartService.getTotalQuantity(customer)).thenReturn(1);
        CustomerOrder saved = new CustomerOrder();
        ReflectionTestUtils.setField(saved, "id", 77L);
        when(orderService.checkout(eq(customer), any(CheckoutDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/checkout").with(csrf())
                        .param("deliveryRecipientName", "Amina Rahman")
                        .param("deliveryPhone", "01700000000")
                        .param("deliveryAddress", "Dhaka")
                        .param("paymentMethod", "CASH_ON_DELIVERY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/77?created"));

        verify(orderService).checkout(eq(customer), any(CheckoutDTO.class));
    }

    @Test
    void cartAndOrderControllersDeclareNoSessionCartState() throws Exception {
        String cartController = Files.readString(Path.of(
                "src/main/java/bd/edu/seu/jerseysee/controller/CartController.java"));
        String orderController = Files.readString(Path.of(
                "src/main/java/bd/edu/seu/jerseysee/controller/OrderController.java"));

        assertThat(cartController).doesNotContain("@SessionAttributes", "new ShoppingCart()");
        assertThat(orderController).doesNotContain(
                "@SessionAttributes",
                "new ShoppingCart()",
                "shoppingCart.clear()",
                "orderService.checkout(customer, shoppingCart, checkout)");
    }
}
