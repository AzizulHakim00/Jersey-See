package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.AddToCartDTO;
import bd.edu.seu.jerseysee.dto.CheckoutDTO;
import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.CustomerOrder;
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
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.CustomerOrderRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersistentCartCheckoutTest {

    private static final String TEST_PASSWORD = "Checkout-demo-2026!";

    @LocalServerPort private int port;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private CustomerOrderRepository orderRepository;

    @Test
    void successfulCheckoutPersistsOrderDecrementsStockAndClearsPersistentCart() {
        User customer = customer("success");
        ProductVariant variant = variant("SUCCESS", 5);
        cartService.add(customer, add(variant.getId(), 2));
        long ordersBefore = orderRepository.count();

        CustomerOrder order = orderService.checkout(customer, checkout());

        assertThat(order.getId()).isNotNull();
        assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);
        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
        assertThat(cartService.getCart(customer).isEmpty()).isTrue();
        assertThat(cartService.getTotalQuantity(customer)).isZero();
    }

    @Test
    void insufficientStockRollsBackOrderAndLeavesCartAndStockUnchanged() {
        User customer = customer("shortage");
        ProductVariant variant = variant("SHORTAGE", 2);
        cartService.add(customer, add(variant.getId(), 2));
        variant.setStockQuantity(1);
        variantRepository.saveAndFlush(variant);
        long ordersBefore = orderRepository.count();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> orderService.checkout(customer, checkout()))
                .withMessage("Insufficient stock for " + variant.getSku() + ".");

        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(cartService.getTotalQuantity(customer)).isEqualTo(2);
    }

    @Test
    void invalidCheckoutLeavesPersistentCartUntouched() {
        User customer = customer("invalid");
        ProductVariant variant = variant("INVALID", 3);
        cartService.add(customer, add(variant.getId(), 1));
        CheckoutDTO invalid = checkout();
        invalid.setDeliveryPhone("bad phone");
        long ordersBefore = orderRepository.count();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> orderService.checkout(customer, invalid))
                .withMessage("Enter a valid delivery phone number.");

        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
        assertThat(cartService.getTotalQuantity(customer)).isEqualTo(1);
    }

    @Test
    void secondCheckoutAfterSuccessFailsAsEmptyWithoutCreatingDuplicateOrder() {
        User customer = customer("duplicate");
        ProductVariant variant = variant("DUPLICATE", 4);
        cartService.add(customer, add(variant.getId(), 1));
        long ordersBefore = orderRepository.count();

        orderService.checkout(customer, checkout());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> orderService.checkout(customer, checkout()))
                .withMessage("Your cart is empty.");
        assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);
        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
    }

    @Test
    void buyerCheckoutAndAdminFulfilmentWorkThroughBrowserForms() throws Exception {
        User buyer = browserAccount(Role.CUSTOMER);
        User admin = browserAccount(Role.ADMIN);
        ProductVariant variant = variant("BROWSER", 5);
        Browser buyerBrowser = new Browser(port);
        buyerBrowser.signIn(buyer.getEmail());

        String productPath = "/products/" + variant.getProduct().getId();
        HttpResponse<String> added = buyerBrowser.submit(productPath, "/cart/items", Map.of(
                "variantId", variant.getId().toString(),
                "quantity", "2",
                "printingType", "NONE"), 302);
        assertThat(location(added)).isEqualTo("/cart?added");

        HttpResponse<String> ordered = buyerBrowser.submit("/checkout", "/checkout", Map.of(
                "deliveryRecipientName", "Demo Order Recipient",
                "deliveryPhone", "01700000000",
                "deliveryAddress", "Demo delivery address in Dhaka",
                "paymentMethod", "CASH"), 302);
        String orderLocation = location(ordered);
        assertThat(orderLocation).matches("/orders/[0-9]+\\?created");
        String orderPath = orderLocation.substring(0, orderLocation.indexOf('?'));
        Long orderId = Long.valueOf(orderPath.substring("/orders/".length()));
        CustomerOrder order = orderRepository.findDetailedById(orderId).orElseThrow();
        assertThat(order.getCustomer().getEmail()).isEqualTo(buyer.getEmail());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotal()).isEqualByComparingTo("2200.00");
        assertThat(order.getPayment().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(cartService.getTotalQuantity(buyer)).isZero();
        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
        assertThat(buyerBrowser.get("/orders", 200).body()).contains("href=\"" + orderPath + "\"");
        buyerBrowser.get("/staff" + orderPath, 403);

        Browser adminBrowser = new Browser(port);
        adminBrowser.signIn(admin.getEmail());
        String staffOrderPath = "/staff" + orderPath;
        assertThat(adminBrowser.get("/staff/orders", 200).body()).contains("href=\"" + staffOrderPath + "\"");

        String paymentAction = "/staff/payments/" + order.getPayment().getId() + "/confirm";
        HttpResponse<String> paid = adminBrowser.submit("/staff/payments", paymentAction, Map.of(), 302);
        assertThat(location(paid)).isEqualTo("/staff/payments?confirmed");

        for (OrderStatus state : new OrderStatus[] {
                OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED}) {
            HttpResponse<String> updated = adminBrowser.submit(staffOrderPath, staffOrderPath + "/status",
                    Map.of("status", state.name()), 302);
            assertThat(location(updated)).isEqualTo(staffOrderPath + "?statusUpdated");
            assertThat(orderRepository.findDetailedById(orderId).orElseThrow().getStatus()).isEqualTo(state);
        }

        CustomerOrder delivered = orderRepository.findDetailedById(orderId).orElseThrow();
        assertThat(delivered.getPayment().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(buyerBrowser.get(orderPath, 200).body()).contains("DELIVERED", "PAID");
        HttpResponse<String> invoice = buyerBrowser.get(orderPath + "/invoice", 200);
        assertThat(invoice.headers().firstValue("Content-Disposition"))
                .hasValue("attachment; filename=\"jerseysee-invoice-" + orderId + ".txt\"");
        assertThat(invoice.body()).contains(variant.getSku(), "Demo Order Recipient");
    }

    private User browserAccount(Role role) {
        User user = new User();
        user.setName("Checkout " + role.name());
        user.setEmail(role.name().toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + "@example.com");
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setPhone("01700000000");
        user.setAddress("Dhaka");
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private static String location(HttpResponse<String> response) {
        URI target = URI.create(response.headers().firstValue("Location").orElseThrow());
        String query = target.getRawQuery();
        return target.getRawPath() + (query == null ? "" : "?" + query);
    }

    private static final class Browser {
        private static final Pattern CSRF_INPUT = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");

        private final URI origin;
        private final HttpClient http = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER))
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        private Browser(int port) {
            origin = URI.create("http://127.0.0.1:" + port);
        }

        private void signIn(String email) throws IOException, InterruptedException {
            HttpResponse<String> signedIn = submit("/login", "/login",
                    Map.of("username", email, "password", TEST_PASSWORD), 302);
            assertThat(location(signedIn)).isEqualTo("/dashboard");
        }

        private HttpResponse<String> get(String path, int status) throws IOException, InterruptedException {
            return send(request(path).GET().build(), status);
        }

        private HttpResponse<String> submit(String page, String action, Map<String, String> fields, int status)
                throws IOException, InterruptedException {
            String html = get(page, 200).body();
            var form = Pattern.compile("<form\\b[^>]*\\baction=\"" + Pattern.quote(action)
                    + "\"[^>]*>(.*?)</form>", Pattern.DOTALL).matcher(html);
            assertThat(form.find()).as("form %s on %s", action, page).isTrue();
            var csrf = CSRF_INPUT.matcher(form.group(1));
            assertThat(csrf.find()).as("CSRF input on %s", page).isTrue();

            String data = fields.entrySet().stream()
                    .map(field -> encode(field.getKey()) + "=" + encode(field.getValue()))
                    .collect(Collectors.joining("&"));
            String body = data.isEmpty() ? "_csrf=" + encode(csrf.group(1))
                    : data + "&_csrf=" + encode(csrf.group(1));
            return send(request(action).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), status);
        }

        private HttpRequest.Builder request(String path) {
            return HttpRequest.newBuilder(origin.resolve(path)).timeout(Duration.ofSeconds(10));
        }

        private HttpResponse<String> send(HttpRequest request, int status) throws IOException, InterruptedException {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).as("%s %s", request.method(), request.uri().getPath()).isEqualTo(status);
            return response;
        }

        private static String encode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
    }

    private User customer(String label) {
        User user = new User();
        user.setName("Checkout Customer");
        user.setEmail(label + "-" + UUID.randomUUID() + "@example.com");
        user.setPassword("encoded-test-password");
        user.setPhone("01700000000");
        user.setAddress("Dhaka");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private ProductVariant variant(String label, int stock) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Category category = new Category();
        category.setName("Persistent Checkout " + label + " " + suffix);
        category.setDescription("Persistent checkout test category");

        Product product = new Product();
        product.setName("Persistent Checkout Jersey " + label);
        product.setDescription("Persistent checkout test product");
        product.setBrand("JerseySee");
        product.setProductType(ProductType.JERSEY);
        product.setBasePrice(new BigDecimal("1000.00"));
        product.setActive(true);
        category.addProduct(product);

        ProductVariant variant = new ProductVariant();
        variant.setSize(SizeOption.M);
        variant.setSku("PC-" + label + "-" + suffix);
        variant.setStockQuantity(stock);
        variant.setPriceAdjustment(new BigDecimal("50.00"));
        product.addVariant(variant);

        categoryRepository.saveAndFlush(category);
        return variant;
    }

    private AddToCartDTO add(Long variantId, int quantity) {
        AddToCartDTO input = new AddToCartDTO();
        input.setVariantId(variantId);
        input.setQuantity(quantity);
        input.setPrintingType(PrintingType.NONE);
        return input;
    }

    private CheckoutDTO checkout() {
        CheckoutDTO input = new CheckoutDTO();
        input.setDeliveryRecipientName("Amina Rahman");
        input.setDeliveryPhone("01700000000");
        input.setDeliveryAddress("Dhaka");
        input.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        return input;
    }
}
