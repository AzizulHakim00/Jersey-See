package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.Payment;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.OrderStatus;
import bd.edu.seu.jerseysee.model.enums.PaymentStatus;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.CustomerOrderRepository;
import bd.edu.seu.jerseysee.repository.EmployeeProfileRepository;
import bd.edu.seu.jerseysee.repository.PaymentRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private CustomerOrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EmployeeProfileRepository employeeRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void customerSummaryUsesOnlyTheCustomersOrdersAndHidesStaffMetrics() {
        User customer = user(Role.CUSTOMER, "customer@example.com");
        CustomerOrder pending = order(OrderStatus.PENDING, "1200.00");
        CustomerOrder delivered = order(OrderStatus.DELIVERED, "2500.00");
        when(orderRepository.findDetailedByCustomerEmail("customer@example.com"))
                .thenReturn(List.of(pending, delivered));
        when(productRepository.countByActiveTrue()).thenReturn(12L);

        DashboardService.DashboardSummary summary = dashboardService.summary(customer);

        assertThat(summary.orderCount()).isEqualTo(2);
        assertThat(summary.activeOrderCount()).isEqualTo(1);
        assertThat(summary.catalogProductCount()).isEqualTo(12);
        assertThat(summary.pendingPaymentCount()).isZero();
        assertThat(summary.revenue()).isEqualByComparingTo("0.00");
        assertThat(summary.recentOrders()).containsExactly(pending, delivered);
        verify(paymentRepository, never()).findAllDetailed();
        verify(employeeRepository, never()).count();
    }

    @Test
    void administratorSummaryCalculatesOperationalCountsAndConfirmedRevenue() {
        User administrator = user(Role.ADMIN, "admin@example.com");
        CustomerOrder pending = order(OrderStatus.PENDING, "1200.00");
        CustomerOrder processing = order(OrderStatus.PROCESSING, "2500.00");
        CustomerOrder delivered = order(OrderStatus.DELIVERED, "3200.00");
        ProductVariant low = lowStockVariant();
        Payment paid = payment(PaymentStatus.PAID, "3200.00");
        Payment confirmed = payment(PaymentStatus.CONFIRMED, "900.00");
        Payment awaiting = payment(PaymentStatus.PENDING, "1200.00");
        when(orderRepository.findAllDetailed()).thenReturn(List.of(pending, processing, delivered));
        when(productRepository.countByActiveTrue()).thenReturn(18L);
        when(variantRepository.findTop8ByStockQuantityLessThanEqualOrderByStockQuantityAsc(5))
                .thenReturn(List.of(low));
        when(paymentRepository.findAllDetailed()).thenReturn(List.of(paid, confirmed, awaiting));
        when(employeeRepository.count()).thenReturn(7L);

        DashboardService.DashboardSummary summary = dashboardService.summary(administrator);

        assertThat(summary.orderCount()).isEqualTo(3);
        assertThat(summary.activeOrderCount()).isEqualTo(2);
        assertThat(summary.catalogProductCount()).isEqualTo(18);
        assertThat(summary.lowStockCount()).isEqualTo(1);
        assertThat(summary.pendingPaymentCount()).isEqualTo(1);
        assertThat(summary.staffCount()).isEqualTo(7);
        assertThat(summary.revenue()).isEqualByComparingTo("4100.00");
        assertThat(summary.lowStockVariants()).containsExactly(low);
    }

    private User user(Role role, String email) {
        User user = new User();
        user.setRole(role);
        user.setEmail(email);
        user.setEnabled(true);
        return user;
    }

    private CustomerOrder order(OrderStatus status, String total) {
        CustomerOrder order = new CustomerOrder();
        order.setStatus(status);
        order.setTotal(new BigDecimal(total));
        return order;
    }

    private ProductVariant lowStockVariant() {
        Product product = new Product();
        product.setName("National Away Jersey");
        ProductVariant variant = new ProductVariant();
        variant.setSku("NAT-AWAY-M");
        variant.setStockQuantity(3);
        product.addVariant(variant);
        return variant;
    }

    private Payment payment(PaymentStatus status, String amount) {
        Payment payment = new Payment();
        payment.setStatus(status);
        payment.setAmount(new BigDecimal(amount));
        return payment;
    }
}
