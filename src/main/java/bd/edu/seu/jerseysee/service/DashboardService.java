package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.Payment;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final PaymentRepository paymentRepository;
    private final EmployeeProfileRepository employeeRepository;

    public DashboardService(CustomerOrderRepository orderRepository, ProductRepository productRepository,
            ProductVariantRepository variantRepository, PaymentRepository paymentRepository,
            EmployeeProfileRepository employeeRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.paymentRepository = paymentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(User actor) {
        requireEnabledActor(actor);
        long catalogProducts = productRepository.countByActiveTrue();
        if (actor.getRole() == Role.CUSTOMER) {
            List<CustomerOrder> orders = orderRepository.findDetailedByCustomerEmail(actor.getEmail());
            return new DashboardSummary(orders.size(), activeOrders(orders), catalogProducts, 0, 0, 0,
                    moneyZero(), recent(orders), List.of());
        }
        if (!isStaff(actor.getRole())) {
            throw new AccessDeniedException("Dashboard access denied.");
        }

        List<CustomerOrder> orders = orderRepository.findAllDetailed();
        List<ProductVariant> lowStock = variantRepository
                .findTop8ByStockQuantityLessThanEqualOrderByStockQuantityAsc(LOW_STOCK_THRESHOLD);
        long staffCount = employeeRepository.count();
        if (!canViewPayments(actor.getRole())) {
            return new DashboardSummary(orders.size(), activeOrders(orders), catalogProducts, lowStock.size(), 0,
                    staffCount, moneyZero(), recent(orders), lowStock);
        }

        List<Payment> payments = paymentRepository.findAllDetailed();
        long pendingPayments = payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.PENDING).count();
        BigDecimal revenue = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID
                        || payment.getStatus() == PaymentStatus.CONFIRMED)
                .map(Payment::getAmount)
                .filter(amount -> amount != null)
                .reduce(moneyZero(), BigDecimal::add);
        return new DashboardSummary(orders.size(), activeOrders(orders), catalogProducts, lowStock.size(),
                pendingPayments, staffCount, revenue, recent(orders), lowStock);
    }

    private long activeOrders(List<CustomerOrder> orders) {
        return orders.stream().filter(order -> order.getStatus() != OrderStatus.DELIVERED
                && order.getStatus() != OrderStatus.CANCELLED).count();
    }

    private List<CustomerOrder> recent(List<CustomerOrder> orders) {
        return orders.stream().limit(5).toList();
    }

    private boolean isStaff(Role role) {
        return role == Role.SALESMAN || role == Role.CASHIER || role == Role.MANAGER || role == Role.ADMIN;
    }

    private boolean canViewPayments(Role role) {
        return role == Role.CASHIER || role == Role.MANAGER || role == Role.ADMIN;
    }

    private void requireEnabledActor(User actor) {
        if (actor == null || actor.getRole() == null || !actor.isEnabled()) {
            throw new AccessDeniedException("Dashboard access denied.");
        }
    }

    private BigDecimal moneyZero() {
        return new BigDecimal("0.00");
    }

    public record DashboardSummary(long orderCount, long activeOrderCount, long catalogProductCount,
            long lowStockCount, long pendingPaymentCount, long staffCount, BigDecimal revenue,
            List<CustomerOrder> recentOrders, List<ProductVariant> lowStockVariants) {

        public static DashboardSummary empty() {
            return new DashboardSummary(0, 0, 0, 0, 0, 0, new BigDecimal("0.00"), List.of(), List.of());
        }
    }
}
