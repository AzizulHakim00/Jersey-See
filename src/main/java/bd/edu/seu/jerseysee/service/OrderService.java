package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.CheckoutDTO;
import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.model.CustomerCartItem;
import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.OrderItem;
import bd.edu.seu.jerseysee.model.Payment;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.OrderStatus;
import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import bd.edu.seu.jerseysee.model.enums.PaymentStatus;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.CustomerOrderRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    public static final BigDecimal DELIVERY_FEE = new BigDecimal("100.00");

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));

    private final CustomerOrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final CartService cartService;

    public OrderService(CustomerOrderRepository orderRepository, ProductVariantRepository variantRepository,
            CartService cartService) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.cartService = cartService;
    }

    @Transactional
    public CustomerOrder checkout(User customer, CheckoutDTO input) {
        requireCustomer(customer);
        CheckoutValues checkout = validateCheckout(input);
        List<CustomerCartItem> cartItems = cartService.lockCartRows(customer);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty.");
        }

        Map<Long, ProductVariant> lockedVariants = lockVariants(cartItems);
        validateStock(cartItems, lockedVariants);

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setDeliveryRecipientName(checkout.recipientName());
        order.setDeliveryPhone(checkout.phone());
        order.setDeliveryAddress(checkout.address());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CustomerCartItem cartItem : cartItems) {
            ProductVariant variant = lockedVariants.get(cartItem.getProductVariant().getId());
            CartService.PrintingSelection printing = CartService.validatePrinting(cartItem.getPrintingType(),
                    cartItem.getPrintingName(), cartItem.getPrintingNumber());
            BigDecimal unitPrice = CartService.unitPrice(variant);
            BigDecimal printingCharge = CartService.printingCharge(printing.type());
            BigDecimal lineSubtotal = unitPrice.add(printingCharge)
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem item = new OrderItem();
            item.setProductVariant(variant);
            item.setProductName(variant.getProduct().getName());
            item.setSku(variant.getSku());
            item.setSize(variant.getSize());
            item.setUnitPrice(unitPrice);
            item.setPrintingType(printing.type());
            item.setCustomName(printing.name());
            item.setCustomNumber(printing.number());
            item.setPrintingCharge(printingCharge);
            item.setQuantity(cartItem.getQuantity());
            item.setSubtotal(lineSubtotal);
            order.addItem(item);
            subtotal = subtotal.add(lineSubtotal);
        }

        lockedVariants.forEach((id, variant) -> variant.setStockQuantity(
                variant.getStockQuantity() - quantityForVariant(cartItems, id)));
        order.setSubtotal(subtotal);
        order.setDeliveryFee(DELIVERY_FEE);
        order.setTotal(subtotal.add(DELIVERY_FEE));

        Payment payment = new Payment();
        payment.setMethod(checkout.paymentMethod());
        payment.setAmount(order.getTotal());
        payment.setTransactionId(checkout.transactionId());
        payment.setStatus(PaymentStatus.PENDING);
        order.setPayment(payment);

        CustomerOrder saved = orderRepository.saveAndFlush(order);
        cartService.clearCart(customer);
        return saved;
    }

    @Transactional(readOnly = true)
    public CustomerOrder getAccessible(Long orderId, User requester) {
        CustomerOrder order = detailedOrder(orderId);
        requireOrderAccess(order, requester);
        return order;
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> listFor(User requester) {
        requireEnabled(requester);
        if (isStaff(requester.getRole())) {
            return orderRepository.findAllDetailed();
        }
        if (requester.getRole() == Role.CUSTOMER) {
            return orderRepository.findDetailedByCustomerEmail(requester.getEmail());
        }
        throw new AccessDeniedException("Order access denied.");
    }

    @Transactional
    public CustomerOrder updateStatus(Long orderId, OrderStatus targetStatus, User actor) {
        requireWorkflowStaff(actor);
        if (targetStatus == null) {
            throw new IllegalArgumentException("Order status is required.");
        }
        CustomerOrder order = lockedOrder(orderId);
        if (order.getStatus() == targetStatus) {
            return order;
        }
        if (!TRANSITIONS.getOrDefault(order.getStatus(), Set.of()).contains(targetStatus)) {
            throw new IllegalArgumentException(
                    "Order cannot move from " + order.getStatus() + " to " + targetStatus + ".");
        }
        if (targetStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
        }
        order.setStatus(targetStatus);
        orderRepository.saveAndFlush(order);
        return order;
    }

    @Transactional
    public CustomerOrder cancel(Long orderId, User customer) {
        requireCustomer(customer);
        CustomerOrder order = lockedOrder(orderId);
        requireOwner(order, customer);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("This order can no longer be cancelled.");
        }
        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.saveAndFlush(order);
        return order;
    }

    private Map<Long, ProductVariant> lockVariants(List<CustomerCartItem> items) {
        Map<Long, ProductVariant> locked = new LinkedHashMap<>();
        TreeSet<Long> variantIds = new TreeSet<>();
        for (CustomerCartItem item : items) {
            if (item.getProductVariant() == null || item.getProductVariant().getId() == null) {
                throw new IllegalStateException("Cart line is missing its product variant.");
            }
            variantIds.add(item.getProductVariant().getId());
        }
        for (Long variantId : variantIds) {
            ProductVariant variant = variantRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found."));
            if (variant.getProduct() == null || !variant.getProduct().isActive()) {
                throw new IllegalArgumentException("A product in your cart is no longer available.");
            }
            locked.put(variantId, variant);
        }
        return locked;
    }

    private void validateStock(List<CustomerCartItem> items, Map<Long, ProductVariant> lockedVariants) {
        for (CustomerCartItem item : items) {
            if (item.getQuantity() < 1 || item.getQuantity() > 10) {
                throw new IllegalArgumentException("Quantity must be between 1 and 10 per cart line.");
            }
            CartService.validatePrinting(item.getPrintingType(), item.getPrintingName(), item.getPrintingNumber());
        }
        for (Map.Entry<Long, ProductVariant> entry : lockedVariants.entrySet()) {
            ProductVariant variant = entry.getValue();
            if (quantityForVariant(items, entry.getKey()) > variant.getStockQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for " + variant.getSku() + ".");
            }
        }
    }

    private int quantityForVariant(List<CustomerCartItem> items, Long variantId) {
        return items.stream()
                .filter(item -> item.getProductVariant() != null
                        && variantId.equals(item.getProductVariant().getId()))
                .mapToInt(CustomerCartItem::getQuantity)
                .sum();
    }

    private void restoreStock(CustomerOrder order) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        List<OrderItem> items = List.copyOf(order.getItems());
        for (OrderItem item : items) {
            ProductVariant orderedVariant = item.getProductVariant();
            if (orderedVariant == null || orderedVariant.getId() == null) {
                throw new IllegalStateException("Order item is missing its product variant.");
            }
            quantities.merge(orderedVariant.getId(), item.getQuantity(), Integer::sum);
        }
        for (Long variantId : new TreeSet<>(quantities.keySet())) {
            ProductVariant variant = variantRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found."));
            variant.setStockQuantity(variant.getStockQuantity() + quantities.get(variantId));
        }
    }

    private CheckoutValues validateCheckout(CheckoutDTO input) {
        if (input == null) {
            throw new IllegalArgumentException("Checkout data is incomplete.");
        }
        String recipient = requireText(input.getDeliveryRecipientName(), 255,
                "Delivery recipient name is required.");
        String phone = requireText(input.getDeliveryPhone(), 25, "Delivery phone is required.");
        if (!phone.matches("^[0-9+() -]{7,25}$")) {
            throw new IllegalArgumentException("Enter a valid delivery phone number.");
        }
        String address = requireText(input.getDeliveryAddress(), 1000, "Delivery address is required.");
        PaymentMethod method = input.getPaymentMethod();
        if (method == null) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        String transactionId = trimToNull(input.getTransactionId());
        if (requiresTransactionId(method) && transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required for " + method + " payments.");
        }
        if (transactionId != null && transactionId.length() > 100) {
            throw new IllegalArgumentException("Transaction ID cannot exceed 100 characters.");
        }
        return new CheckoutValues(recipient, phone, address, method, transactionId);
    }

    private String requireText(String value, int maximumLength, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(message.replace(" is required", " is too long"));
        }
        return normalized;
    }

    private boolean requiresTransactionId(PaymentMethod method) {
        return method == PaymentMethod.BKASH || method == PaymentMethod.NAGAD || method == PaymentMethod.CARD;
    }

    private CustomerOrder detailedOrder(Long orderId) {
        return orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
    }

    private CustomerOrder lockedOrder(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
    }

    private void requireOrderAccess(CustomerOrder order, User requester) {
        requireEnabled(requester);
        if (!isStaff(requester.getRole())) {
            requireOwner(order, requester);
        }
    }

    private void requireOwner(CustomerOrder order, User requester) {
        if (requester.getRole() != Role.CUSTOMER || order.getCustomer() == null
                || requester.getEmail() == null || order.getCustomer().getEmail() == null
                || !requester.getEmail().equalsIgnoreCase(order.getCustomer().getEmail())) {
            throw new AccessDeniedException("Order access denied.");
        }
    }

    private void requireCustomer(User customer) {
        requireEnabled(customer);
        if (customer.getRole() != Role.CUSTOMER) {
            throw new AccessDeniedException("Only customers may complete this action.");
        }
    }

    private void requireWorkflowStaff(User actor) {
        requireEnabled(actor);
        if (actor.getRole() != Role.SALESMAN && actor.getRole() != Role.MANAGER && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You cannot update the order workflow.");
        }
    }

    private void requireEnabled(User user) {
        if (user == null || !user.isEnabled() || user.getRole() == null) {
            throw new AccessDeniedException("Order access denied.");
        }
    }

    private boolean isStaff(Role role) {
        return role == Role.SALESMAN || role == Role.CASHIER || role == Role.MANAGER || role == Role.ADMIN;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record CheckoutValues(String recipientName, String phone, String address, PaymentMethod paymentMethod,
            String transactionId) {
    }
}
