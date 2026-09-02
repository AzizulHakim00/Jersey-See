package bd.edu.seu.jerseysee.model;

import bd.edu.seu.jerseysee.cart.ShoppingCart;
import bd.edu.seu.jerseysee.dto.AddToCartDTO;
import bd.edu.seu.jerseysee.dto.CheckoutDTO;
import bd.edu.seu.jerseysee.dto.PaymentConfirmationDTO;
import bd.edu.seu.jerseysee.model.enums.OrderStatus;
import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import bd.edu.seu.jerseysee.model.enums.PaymentStatus;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.CustomerOrderRepository;
import bd.edu.seu.jerseysee.repository.PaymentRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import bd.edu.seu.jerseysee.service.CartService;
import bd.edu.seu.jerseysee.service.OrderService;
import bd.edu.seu.jerseysee.service.PaymentService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.transaction.TestTransaction;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({OrderService.class, PaymentService.class})
class CheckoutPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void checkoutPersistsAndReloadsOwningPaymentRelationship() {
        User customer = userRepository.saveAndFlush(customer());
        ProductVariant variant = persistVariant();
        AddToCartDTO add = new AddToCartDTO();
        add.setVariantId(variant.getId());
        add.setQuantity(1);
        add.setPrintingType(PrintingType.NONE);
        ShoppingCart cart = new ShoppingCart();
        new CartService(variantRepository).add(cart, add);
        CheckoutDTO checkout = new CheckoutDTO();
        checkout.setDeliveryRecipientName("Amina Rahman");
        checkout.setDeliveryPhone("01700000000");
        checkout.setDeliveryAddress("Dhaka");
        checkout.setPaymentMethod(PaymentMethod.CARD);
        checkout.setTransactionId("card-demo-1");

        CustomerOrder saved = orderService.checkout(customer, cart, checkout);
        Long orderId = saved.getId();
        entityManager.flush();
        entityManager.clear();

        CustomerOrder reloaded = orderRepository.findDetailedById(orderId).orElseThrow();
        assertThat(reloaded.getPayment()).isNotNull();
        assertThat(reloaded.getPayment().getCustomerOrder().getId()).isEqualTo(orderId);
        assertThat(reloaded.getPayment().getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(reloaded.getPayment().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(reloaded.getPayment().getAmount()).isEqualByComparingTo("1350.00");
        assertThat(reloaded.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductName()).isEqualTo("National Home Jersey");
            assertThat(item.getSku()).isEqualTo("NAT-L");
            assertThat(item.getUnitPrice()).isEqualByComparingTo("1250.00");
        });
    }

    @Test
    void rootLockedCancellationPersistsFinalStateAcrossSeparateTransactions() {
        User customer = userRepository.saveAndFlush(customer("mutation@example.com"));
        ProductVariant variant = persistVariant("Mutation Jerseys", "MUT-L");
        ShoppingCart cart = cartFor(variant);
        CheckoutDTO checkout = checkout(PaymentMethod.CARD, "card-demo-2");
        CustomerOrder created = orderService.checkout(customer, cart, checkout);
        Long orderId = created.getId();
        Long paymentId = created.getPayment().getId();

        entityManager.flush();
        TestTransaction.flagForCommit();
        TestTransaction.end();

        User committedCustomer = userRepository.findByEmail("mutation@example.com").orElseThrow();
        orderService.cancel(orderId, committedCustomer);
        PaymentConfirmationDTO confirmation = new PaymentConfirmationDTO();
        confirmation.setTransactionId("card-confirmed-2");
        paymentService.confirm(paymentId, confirmation, cashier());

        TestTransaction.start();
        entityManager.clear();

        CustomerOrder reloadedOrder = orderRepository.findDetailedById(orderId).orElseThrow();
        Payment reloadedPayment = paymentRepository.findDetailedById(paymentId).orElseThrow();
        ProductVariant reloadedVariant = variantRepository.findById(variant.getId()).orElseThrow();
        assertThat(reloadedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(reloadedVariant.getStockQuantity()).isEqualTo(4);
        assertThat(reloadedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(reloadedPayment.getTransactionId()).isEqualTo("card-confirmed-2");
        assertThat(reloadedPayment.getPaymentDate()).isNotNull();
    }

    private ShoppingCart cartFor(ProductVariant variant) {
        AddToCartDTO add = new AddToCartDTO();
        add.setVariantId(variant.getId());
        add.setQuantity(1);
        add.setPrintingType(PrintingType.NONE);
        ShoppingCart cart = new ShoppingCart();
        new CartService(variantRepository).add(cart, add);
        return cart;
    }

    private CheckoutDTO checkout(PaymentMethod method, String transactionId) {
        CheckoutDTO checkout = new CheckoutDTO();
        checkout.setDeliveryRecipientName("Amina Rahman");
        checkout.setDeliveryPhone("01700000000");
        checkout.setDeliveryAddress("Dhaka");
        checkout.setPaymentMethod(method);
        checkout.setTransactionId(transactionId);
        return checkout;
    }

    private User cashier() {
        User cashier = new User();
        cashier.setRole(Role.CASHIER);
        cashier.setEnabled(true);
        return cashier;
    }

    private User customer() {
        return customer("amina@example.com");
    }

    private User customer(String email) {
        User user = new User();
        user.setName("Amina Rahman");
        user.setEmail(email);
        user.setPassword("encoded");
        user.setPhone("01700000000");
        user.setAddress("Dhaka");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        return user;
    }

    private ProductVariant persistVariant() {
        return persistVariant("Jerseys", "NAT-L");
    }

    private ProductVariant persistVariant(String categoryName, String sku) {
        Category category = new Category();
        category.setName(categoryName);
        category.setDescription("Football jerseys");
        Product product = new Product();
        product.setName("National Home Jersey");
        product.setDescription("Home shirt");
        product.setBrand("JerseySee");
        product.setProductType(ProductType.JERSEY);
        product.setBasePrice(new BigDecimal("1200.00"));
        product.setActive(true);
        category.addProduct(product);
        ProductVariant variant = new ProductVariant();
        variant.setSize(SizeOption.L);
        variant.setSku(sku);
        variant.setStockQuantity(4);
        variant.setPriceAdjustment(new BigDecimal("50.00"));
        product.addVariant(variant);
        categoryRepository.saveAndFlush(category);
        return variant;
    }
}
