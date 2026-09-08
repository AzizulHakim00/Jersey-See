package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.AddToCartDTO;
import bd.edu.seu.jerseysee.dto.CheckoutDTO;
import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.CustomerOrderRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersistentCartCheckoutTest {

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
