package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.cart.CartItem;
import bd.edu.seu.jerseysee.cart.ShoppingCart;
import bd.edu.seu.jerseysee.config.SecurityConfig;
import bd.edu.seu.jerseysee.dto.ProductDTO;
import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.EmployeeProfile;
import bd.edu.seu.jerseysee.model.OrderItem;
import bd.edu.seu.jerseysee.model.Payment;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.OrderStatus;
import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import bd.edu.seu.jerseysee.model.enums.PaymentStatus;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.service.CartService;
import bd.edu.seu.jerseysee.service.DashboardService;
import bd.edu.seu.jerseysee.service.EmployeeService;
import bd.edu.seu.jerseysee.service.InvoiceService;
import bd.edu.seu.jerseysee.service.OrderService;
import bd.edu.seu.jerseysee.service.PaymentService;
import bd.edu.seu.jerseysee.service.ProductService;
import bd.edu.seu.jerseysee.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({
        AdminProductController.class,
        AuthController.class,
        CartController.class,
        DashboardController.class,
        EmployeeController.class,
        ErrorViewController.class,
        HomeController.class,
        OrderController.class,
        PaymentController.class,
        ProductController.class,
        ProfileController.class
})
@Import(SecurityConfig.class)
class PageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private DashboardService dashboardService;

    private Product product;
    private CustomerOrder order;
    private Payment payment;

    @BeforeEach
    void setUpRealisticModels() {
        Category category = category();
        product = product(category);
        order = order(product.getVariants().get(0));
        payment = order.getPayment();

        when(productService.featuredProducts()).thenReturn(List.of(product));
        when(productService.categories()).thenReturn(List.of(category));
        when(productService.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 12), 1));
        when(productService.getActive(11L)).thenReturn(product);
        when(productService.getActive(999L)).thenThrow(new ResourceNotFoundException("Product not found."));
        when(productService.listForManagement(any()))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));
        when(productService.getForManagement(11L)).thenReturn(product);
        when(productService.toDTO(product)).thenReturn(productDto());

        when(userService.getRequiredByEmail("customer@example.com")).thenReturn(user(Role.CUSTOMER));
        when(userService.getRequiredByEmail("salesman@example.com")).thenReturn(user(Role.SALESMAN));
        when(userService.getRequiredByEmail("cashier@example.com")).thenReturn(user(Role.CASHIER));
        when(userService.getRequiredByEmail("manager@example.com")).thenReturn(user(Role.MANAGER));
        when(userService.getRequiredByEmail("admin@example.com")).thenReturn(user(Role.ADMIN));
        when(userService.getProfile(any())).thenReturn(profile());

        when(orderService.listFor(any())).thenReturn(List.of(order));
        when(orderService.getAccessible(eq(31L), any())).thenReturn(order);
        when(paymentService.listFor(any())).thenReturn(List.of(payment));
        when(employeeService.listAll()).thenReturn(List.of(employee()));
        when(employeeService.rolesAvailableToCurrentUser()).thenReturn(List.of(Role.SALESMAN, Role.CASHIER));
        when(dashboardService.summary(any())).thenReturn(DashboardService.DashboardSummary.empty());
    }

    @Test
    void homeRendersFeaturedProduct() throws Exception {
        assertPage(get("/"), "home/index", "Match-day identity");
    }

    @Test
    void loginRenders() throws Exception {
        assertPage(get("/login"), "auth/login", "Welcome back");
    }

    @Test
    void registrationRenders() throws Exception {
        assertPage(get("/register"), "auth/register", "Create your account");
    }

    @Test
    void catalogRendersFiltersAndProducts() throws Exception {
        assertPage(get("/catalog"), "catalog/list", "Find your colours");
    }

    @Test
    void catalogPaginationPreservesEveryCatalogFilter() throws Exception {
        when(productService.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 12), 24));

        mockMvc.perform(get("/catalog")
                        .param("keyword", "Dhaka home")
                        .param("categoryId", "7")
                        .param("productType", "JERSEY")
                        .param("clubOrCountry", "Dhaka Strikers")
                        .param("edition", "PLAYER")
                        .param("kitType", "HOME")
                        .param("size", "M")
                        .param("available", "true")
                        .param("minimumPrice", "1000.00")
                        .param("maximumPrice", "3000.00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("keyword=Dhaka")))
                .andExpect(content().string(containsString("productType=JERSEY")))
                .andExpect(content().string(containsString("clubOrCountry=Dhaka")))
                .andExpect(content().string(containsString("edition=PLAYER")))
                .andExpect(content().string(containsString("kitType=HOME")))
                .andExpect(content().string(containsString("size=M")))
                .andExpect(content().string(containsString("available=true")))
                .andExpect(content().string(containsString("minimumPrice=1000.00")))
                .andExpect(content().string(containsString("maximumPrice=3000.00")));
    }

    @Test
    void productDetailRendersVariants() throws Exception {
        assertPage(get("/products/11"), "catalog/detail", "Dhaka Strikers Home Jersey");
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void customerDashboardRendersRoleContext() throws Exception {
        assertPage(get("/dashboard"), "dashboard/index", "Customer dashboard");
    }

    @Test
    @WithMockUser(username = "salesman@example.com", roles = "SALESMAN")
    void salesmanDashboardRendersRoleContext() throws Exception {
        assertPage(get("/dashboard"), "dashboard/index", "Salesman dashboard");
    }

    @Test
    @WithMockUser(username = "cashier@example.com", roles = "CASHIER")
    void cashierDashboardRendersRoleContext() throws Exception {
        assertPage(get("/dashboard"), "dashboard/index", "Cashier dashboard");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void managerDashboardRendersRoleContext() throws Exception {
        assertPage(get("/dashboard"), "dashboard/index", "Manager dashboard");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void administratorDashboardRendersRoleContext() throws Exception {
        assertPage(get("/dashboard"), "dashboard/index", "Admin dashboard");
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void populatedCartRenders() throws Exception {
        assertPage(get("/cart").session(cartSession()), "cart/view", "Order summary");
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void checkoutRendersDeliveryAndPaymentFields() throws Exception {
        assertPage(get("/checkout").session(cartSession()), "orders/checkout", "Delivery details");
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void customerOrderListRenders() throws Exception {
        assertPage(get("/orders"), "orders/list", "Your orders");
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void customerOrderDetailRenders() throws Exception {
        assertPage(get("/orders/31"), "orders/detail", "Order #31");
    }

    @Test
    @WithMockUser(username = "salesman@example.com", roles = "SALESMAN")
    void staffOrderListRenders() throws Exception {
        assertPage(get("/staff/orders"), "staff/orders/list", "Order operations");
    }

    @Test
    @WithMockUser(username = "salesman@example.com", roles = "SALESMAN")
    void staffOrderDetailRendersWorkflowControls() throws Exception {
        assertPage(get("/staff/orders/31"), "staff/orders/detail", "Workflow");
    }

    @Test
    @WithMockUser(username = "cashier@example.com", roles = "CASHIER")
    void staffPaymentsRenderConfirmationForm() throws Exception {
        assertPage(get("/staff/payments"), "staff/payments/list", "Payment desk");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void managedProductListRenders() throws Exception {
        assertPage(get("/staff/products"), "staff/products/list", "Product control");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void newProductFormRendersUploadGuidance() throws Exception {
        assertPage(get("/staff/products/new"), "staff/products/form", "JPG, JPEG, PNG, WEBP or GIF");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void editProductFormRendersImageAndVariantControls() throws Exception {
        assertPage(get("/staff/products/11/edit"), "staff/products/form", "Inventory variants");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void employeeListRenders() throws Exception {
        assertPage(get("/staff/employees"), "staff/employees/list", "Team directory");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void employeeFormRendersWithoutAnyPasswordValue() throws Exception {
        mockMvc.perform(get("/staff/employees/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/employees/form"))
                .andExpect(content().string(containsString("Create staff account")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Password1!"))));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void profileFormRenders() throws Exception {
        assertPage(get("/profile"), "profile/edit", "Profile settings");
    }

    @Test
    void registrationValidationErrorsRenderBesideFields() throws Exception {
        mockMvc.perform(post("/register").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registration", "name", "email", "phone", "address",
                        "password", "passwordConfirmation"))
                .andExpect(content().string(containsString("Name is required.")));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void checkoutValidationErrorsRenderWithPreservedCart() throws Exception {
        mockMvc.perform(post("/checkout").session(cartSession()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/checkout"))
                .andExpect(model().attributeHasFieldErrors("checkout", "deliveryRecipientName", "deliveryPhone",
                        "deliveryAddress", "paymentMethod"))
                .andExpect(content().string(containsString("Delivery recipient name is required.")));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void deniedStaffRouteUsesFriendly403Endpoint() throws Exception {
        mockMvc.perform(get("/staff/employees"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void friendly403PageRenders() throws Exception {
        assertPage(get("/access-denied"), "error/403", "That area is offside");
    }

    @Test
    void missingProductRendersFriendly404Page() throws Exception {
        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("We could not find that page")));
    }

    @Test
    void errorEndpointRendersGenericErrorPage() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr("jakarta.servlet.error.status_code", 500)
                        .requestAttr("jakarta.servlet.error.request_uri", "/broken"))
                .andExpect(status().is5xxServerError())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Something went wrong")));
    }

    private void assertPage(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String viewName, String expectedText) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name(viewName))
                .andExpect(content().string(containsString(expectedText)));
    }

    private MockHttpSession cartSession() {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(new CartItem(21L, product.getName(), "DHK-HOME-M", SizeOption.M, 2,
                new BigDecimal("2490.00"), PrintingType.CUSTOM, "RAHMAN", "10",
                CartService.CUSTOM_PRINTING_CHARGE));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("shoppingCart", cart);
        return session;
    }

    private Category category() {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", 7L);
        category.setName("Club Jerseys");
        category.setDescription("Official-inspired club shirts.");
        return category;
    }

    private Product product(Category category) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 11L);
        product.setCategory(category);
        product.setName("Dhaka Strikers Home Jersey");
        product.setDescription("A breathable match-day jersey with a sharp navy finish.");
        product.setBrand("JerseySee");
        product.setProductType(ProductType.JERSEY);
        product.setClubOrCountry("Dhaka Strikers");
        product.setSeason("2026/27");
        product.setBasePrice(new BigDecimal("2490.00"));
        product.setStoredImageName("dhaka-home.webp");
        product.setOriginalImageName("dhaka-home.webp");
        product.setImageContentType("image/webp");
        product.setImageSize(2048L);
        product.setFeatured(true);
        product.setActive(true);
        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", 21L);
        variant.setSize(SizeOption.M);
        variant.setSku("DHK-HOME-M");
        variant.setStockQuantity(8);
        variant.setPriceAdjustment(BigDecimal.ZERO);
        product.addVariant(variant);
        return product;
    }

    private CustomerOrder order(ProductVariant variant) {
        CustomerOrder order = new CustomerOrder();
        ReflectionTestUtils.setField(order, "id", 31L);
        order.setCustomer(user(Role.CUSTOMER));
        order.setDeliveryRecipientName("Amina Rahman");
        order.setDeliveryPhone("01700000000");
        order.setDeliveryAddress("Dhanmondi, Dhaka");
        order.setSubtotal(new BigDecimal("2790.00"));
        order.setDeliveryFee(new BigDecimal("100.00"));
        order.setTotal(new BigDecimal("2890.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 1, 10, 30));
        OrderItem item = new OrderItem();
        ReflectionTestUtils.setField(item, "id", 41L);
        item.setProductVariant(variant);
        item.setProductName(product.getName());
        item.setSku(variant.getSku());
        item.setSize(variant.getSize());
        item.setUnitPrice(product.getBasePrice());
        item.setPrintingType(PrintingType.CUSTOM);
        item.setCustomName("RAHMAN");
        item.setCustomNumber("10");
        item.setPrintingCharge(new BigDecimal("300.00"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("2790.00"));
        order.addItem(item);
        Payment payment = new Payment();
        ReflectionTestUtils.setField(payment, "id", 51L);
        payment.setMethod(PaymentMethod.BKASH);
        payment.setAmount(order.getTotal());
        payment.setTransactionId("TXN-2026-001");
        payment.setStatus(PaymentStatus.PENDING);
        order.setPayment(payment);
        return order;
    }

    private EmployeeProfile employee() {
        EmployeeProfile profile = new EmployeeProfile();
        ReflectionTestUtils.setField(profile, "id", 61L);
        profile.setEmployeeCode("EMP-100");
        profile.setPosition("Sales associate");
        profile.setSalary(new BigDecimal("30000.00"));
        profile.setJoiningDate(LocalDate.of(2026, 1, 15));
        profile.setActive(true);
        User user = user(Role.SALESMAN);
        user.setEmployeeProfile(profile);
        return profile;
    }

    private User user(Role role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", (long) role.ordinal() + 1);
        user.setName(role == Role.CUSTOMER ? "Amina Rahman" : role.name().charAt(0) + role.name().substring(1).toLowerCase());
        user.setEmail(role.name().toLowerCase() + "@example.com");
        user.setPhone("01700000000");
        user.setAddress("Dhaka");
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        return user;
    }

    private bd.edu.seu.jerseysee.dto.ProfileDTO profile() {
        bd.edu.seu.jerseysee.dto.ProfileDTO profile = new bd.edu.seu.jerseysee.dto.ProfileDTO();
        profile.setName("Amina Rahman");
        profile.setPhone("01700000000");
        profile.setAddress("Dhanmondi, Dhaka");
        return profile;
    }

    private ProductDTO productDto() {
        ProductDTO dto = new ProductDTO();
        dto.setCategoryId(7L);
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setBrand(product.getBrand());
        dto.setProductType(product.getProductType());
        dto.setClubOrCountry(product.getClubOrCountry());
        dto.setSeason(product.getSeason());
        dto.setBasePrice(product.getBasePrice());
        dto.setFeatured(true);
        dto.setActive(true);
        return dto;
    }
}
