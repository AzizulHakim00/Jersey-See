package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.cart.ShoppingCart;
import bd.edu.seu.jerseysee.dto.AddToCartDTO;
import bd.edu.seu.jerseysee.dto.CheckoutDTO;
import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.OrderItem;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.OrderStatus;
import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import bd.edu.seu.jerseysee.model.enums.PaymentStatus;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.repository.CustomerOrderRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private CustomerOrderRepository orderRepository;

    private CartService cartService;
    private OrderService orderService;
    private ProductVariant variant;
    private User customer;

    @BeforeEach
    void setUp() {
        cartService = new CartService(variantRepository);
        orderService = new OrderService(orderRepository, variantRepository);
        variant = variant("National Home Jersey", "NAT-L", "1000.00", "50.00", 5);
        ReflectionTestUtils.setField(variant, "id", 7L);
        when(variantRepository.findWithProductById(7L)).thenReturn(Optional.of(variant));
        customer = user("customer@example.com", Role.CUSTOMER);
    }

    @Test
    void checkoutRepricesFromLockedRowsDecrementsStockAndCreatesPendingPayment() {
        ShoppingCart cart = cart(2, PrintingType.CUSTOM, "  jamal bhuyan ", "6");
        variant.getProduct().setBasePrice(new BigDecimal("1100.00"));
        variant.setPriceAdjustment(new BigDecimal("100.00"));
        when(variantRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(variant));
        when(orderRepository.saveAndFlush(any(CustomerOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CheckoutDTO checkout = checkout(PaymentMethod.BKASH, " txn-123 ");

        CustomerOrder order = orderService.checkout(customer, cart, checkout);

        assertThat(variant.getStockQuantity()).isEqualTo(3);
        assertThat(order.getSubtotal()).isEqualByComparingTo("3000.00");
        assertThat(order.getDeliveryFee()).isEqualByComparingTo("100.00");
        assertThat(order.getTotal()).isEqualByComparingTo("3100.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductName()).isEqualTo("National Home Jersey");
            assertThat(item.getSku()).isEqualTo("NAT-L");
            assertThat(item.getUnitPrice()).isEqualByComparingTo("1200.00");
            assertThat(item.getPrintingCharge()).isEqualByComparingTo("300.00");
            assertThat(item.getSubtotal()).isEqualByComparingTo("3000.00");
        });
        assertThat(order.getPayment().getCustomerOrder()).isSameAs(order);
        assertThat(order.getPayment().getMethod()).isEqualTo(PaymentMethod.BKASH);
        assertThat(order.getPayment().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getPayment().getAmount()).isEqualByComparingTo("3100.00");
        assertThat(order.getPayment().getTransactionId()).isEqualTo("txn-123");
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void checkoutRejectsFreshStockShortageWithoutSavingOrDecrementing() {
        ShoppingCart cart = cart(2, PrintingType.NONE, null, null);
        variant.setStockQuantity(1);
        when(variantRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(variant));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> orderService.checkout(customer, cart,
                        checkout(PaymentMethod.CASH_ON_DELIVERY, null)))
                .withMessage("Insufficient stock for NAT-L.");

        assertThat(variant.getStockQuantity()).isEqualTo(1);
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void checkoutRequiresTransactionIdForElectronicPaymentsBeforeChangingStock() {
        ShoppingCart cart = cart(1, PrintingType.NONE, null, null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> orderService.checkout(customer, cart, checkout(PaymentMethod.NAGAD, "  ")))
                .withMessage("Transaction ID is required for NAGAD payments.");

        verify(variantRepository, never()).findByIdForUpdate(any());
        verify(orderRepository, never()).saveAndFlush(any());
        assertThat(variant.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void checkoutRejectsMalformedDeliveryPhoneBeforeLockingStock() {
        ShoppingCart cart = cart(1, PrintingType.NONE, null, null);
        CheckoutDTO checkout = checkout(PaymentMethod.CASH_ON_DELIVERY, null);
        checkout.setDeliveryPhone("not a phone");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> orderService.checkout(customer, cart, checkout))
                .withMessage("Enter a valid delivery phone number.");

        verify(variantRepository, never()).findByIdForUpdate(any());
        verify(orderRepository, never()).saveAndFlush(any());
        assertThat(variant.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void customerCancellationRestoresStockOnceForOwnedPendingOrder() {
        CustomerOrder order = orderWithItem(customer, OrderStatus.PENDING, 2);
        variant.setStockQuantity(1);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        when(variantRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(variant));

        CustomerOrder cancelled = orderService.cancel(21L, customer);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(variant.getStockQuantity()).isEqualTo(3);
        verify(orderRepository).findByIdForUpdate(21L);
        verify(orderRepository, never()).findDetailedById(21L);
        verify(orderRepository).saveAndFlush(order);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> orderService.cancel(21L, customer))
                .withMessage("This order can no longer be cancelled.");
        assertThat(variant.getStockQuantity()).isEqualTo(3);
    }

    @Test
    void workflowMutationLoadsRootOrderWithWriteLockBeforeChangingStatus() {
        CustomerOrder order = orderWithItem(customer, OrderStatus.PENDING, 1);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        User salesman = user("sales@example.com", Role.SALESMAN);

        CustomerOrder updated = orderService.updateStatus(21L, OrderStatus.CONFIRMED, salesman);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).findByIdForUpdate(21L);
        verify(orderRepository, never()).findDetailedById(21L);
    }

    @Test
    void anotherCustomerCannotViewOrCancelAnOrder() {
        CustomerOrder order = orderWithItem(customer, OrderStatus.PENDING, 1);
        when(orderRepository.findDetailedById(21L)).thenReturn(Optional.of(order));
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        User intruder = user("intruder@example.com", Role.CUSTOMER);

        assertThatThrownBy(() -> orderService.getAccessible(21L, intruder))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> orderService.cancel(21L, intruder))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void orderWorkflowAllowsSalesmanButRejectsCashierAndIllegalSkips() {
        CustomerOrder order = orderWithItem(customer, OrderStatus.PENDING, 1);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        User salesman = user("sales@example.com", Role.SALESMAN);

        assertThat(orderService.updateStatus(21L, OrderStatus.CONFIRMED, salesman).getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> orderService.updateStatus(21L, OrderStatus.SHIPPED, salesman))
                .withMessage("Order cannot move from CONFIRMED to SHIPPED.");
        assertThatThrownBy(() -> orderService.updateStatus(21L, OrderStatus.PROCESSING,
                user("cashier@example.com", Role.CASHIER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private ShoppingCart cart(int quantity, PrintingType printingType, String name, String number) {
        AddToCartDTO input = new AddToCartDTO();
        input.setVariantId(7L);
        input.setQuantity(quantity);
        input.setPrintingType(printingType);
        input.setPrintingName(name);
        input.setPrintingNumber(number);
        ShoppingCart cart = new ShoppingCart();
        cartService.add(cart, input);
        return cart;
    }

    private CheckoutDTO checkout(PaymentMethod method, String transactionId) {
        CheckoutDTO input = new CheckoutDTO();
        input.setDeliveryRecipientName(" Amina Rahman ");
        input.setDeliveryPhone(" 01700000000 ");
        input.setDeliveryAddress(" Dhaka ");
        input.setPaymentMethod(method);
        input.setTransactionId(transactionId);
        return input;
    }

    private CustomerOrder orderWithItem(User owner, OrderStatus status, int quantity) {
        CustomerOrder order = new CustomerOrder();
        order.setCustomer(owner);
        order.setStatus(status);
        OrderItem item = new OrderItem();
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        order.addItem(item);
        return order;
    }

    private User user(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private ProductVariant variant(String name, String sku, String basePrice, String adjustment, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setBasePrice(new BigDecimal(basePrice));
        product.setActive(true);
        ProductVariant result = new ProductVariant();
        result.setProduct(product);
        result.setSku(sku);
        result.setSize(SizeOption.L);
        result.setPriceAdjustment(new BigDecimal(adjustment));
        result.setStockQuantity(stock);
        return result;
    }
}
