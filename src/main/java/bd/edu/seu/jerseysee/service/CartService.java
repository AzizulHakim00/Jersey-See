package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.cart.CartItem;
import bd.edu.seu.jerseysee.cart.ShoppingCart;
import bd.edu.seu.jerseysee.dto.AddToCartDTO;
import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.model.CustomerCartItem;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.CustomerCartItemRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    public static final BigDecimal PLAYER_PRINTING_CHARGE = new BigDecimal("200.00");
    public static final BigDecimal CUSTOM_PRINTING_CHARGE = new BigDecimal("300.00");

    private final CustomerCartItemRepository cartRepository;
    private final ProductVariantRepository variantRepository;

    public CartService(CustomerCartItemRepository cartRepository, ProductVariantRepository variantRepository) {
        this.cartRepository = cartRepository;
        this.variantRepository = variantRepository;
    }

    @Transactional(readOnly = true)
    public ShoppingCart getCart(User customer) {
        requireCustomer(customer);
        ShoppingCart cart = new ShoppingCart();
        for (CustomerCartItem persisted : cartRepository.findDetailedByCustomerId(customer.getId())) {
            cart.addItem(toViewItem(persisted));
        }
        return cart;
    }

    @Transactional
    public void add(User customer, AddToCartDTO input) {
        requireCustomer(customer);
        if (input == null || input.getVariantId() == null) {
            throw new IllegalArgumentException("Cart item data is incomplete.");
        }
        requireLineQuantity(input.getQuantity());
        PrintingSelection printing = validatePrinting(input.getPrintingType(), input.getPrintingName(),
                input.getPrintingNumber());
        ProductVariant variant = availableVariant(input.getVariantId());
        List<CustomerCartItem> existing = cartRepository.findDetailedByCustomerId(customer.getId());
        CustomerCartItem matching = existing.stream()
                .filter(item -> matches(item, input.getVariantId(), printing))
                .findFirst()
                .orElse(null);
        int mergedQuantity = input.getQuantity() + (matching == null ? 0 : matching.getQuantity());
        requireLineQuantity(mergedQuantity);
        int alreadyInCart = existing.stream()
                .filter(item -> input.getVariantId().equals(item.getProductVariant().getId()))
                .mapToInt(CustomerCartItem::getQuantity)
                .sum();
        requireStock(alreadyInCart, input.getQuantity(), variant);

        if (matching == null) {
            matching = new CustomerCartItem();
            matching.setCustomer(customer);
            matching.setProductVariant(variant);
            matching.setPrintingType(printing.type());
            matching.setPrintingName(printing.name());
            matching.setPrintingNumber(printing.number());
        }
        matching.setQuantity(mergedQuantity);
        cartRepository.saveAndFlush(matching);
    }

    @Transactional
    public void updateQuantity(User customer, String lineId, int quantity) {
        requireCustomer(customer);
        requireLineQuantity(quantity);
        if (lineId == null || lineId.isBlank()) {
            throw new IllegalArgumentException("Cart line not found.");
        }
        List<CustomerCartItem> existing = cartRepository.findDetailedByCustomerId(customer.getId());
        CustomerCartItem item = existing.stream()
                .filter(candidate -> lineId.equals(candidate.getLineId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cart line not found."));
        ProductVariant variant = availableVariant(item.getProductVariant().getId());
        int otherQuantity = existing.stream()
                .filter(candidate -> item.getProductVariant().getId().equals(candidate.getProductVariant().getId()))
                .filter(candidate -> !lineId.equals(candidate.getLineId()))
                .mapToInt(CustomerCartItem::getQuantity)
                .sum();
        requireStock(otherQuantity, quantity, variant);
        item.setQuantity(quantity);
        cartRepository.saveAndFlush(item);
    }

    @Transactional
    public void remove(User customer, String lineId) {
        requireCustomer(customer);
        if (lineId == null || lineId.isBlank()) {
            throw new IllegalArgumentException("Cart line not found.");
        }
        CustomerCartItem item = cartRepository.findByLineIdAndCustomerId(lineId, customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart line not found."));
        cartRepository.delete(item);
        cartRepository.flush();
    }

    @Transactional(readOnly = true)
    public int getTotalQuantity(User customer) {
        requireCustomer(customer);
        return cartRepository.totalQuantityForCustomer(customer.getId());
    }

    @Transactional
    public List<CustomerCartItem> lockCartRows(User customer) {
        requireCustomer(customer);
        return cartRepository.findByCustomerIdForUpdate(customer.getId());
    }

    @Transactional
    public void clearCart(User customer) {
        requireCustomer(customer);
        cartRepository.deleteAllByCustomerId(customer.getId());
        cartRepository.flush();
    }

    static BigDecimal unitPrice(ProductVariant variant) {
        if (variant == null || variant.getProduct() == null || variant.getProduct().getBasePrice() == null) {
            throw new IllegalArgumentException("Variant must belong to a priced product.");
        }
        BigDecimal adjustment = variant.getPriceAdjustment() == null ? BigDecimal.ZERO : variant.getPriceAdjustment();
        return variant.getProduct().getBasePrice().add(adjustment);
    }

    static BigDecimal printingCharge(PrintingType type) {
        if (type == PrintingType.PLAYER) {
            return PLAYER_PRINTING_CHARGE;
        }
        if (type == PrintingType.CUSTOM) {
            return CUSTOM_PRINTING_CHARGE;
        }
        return BigDecimal.ZERO.setScale(2);
    }

    static PrintingSelection validatePrinting(PrintingType type, String submittedName, String submittedNumber) {
        PrintingType effectiveType = type == null ? PrintingType.NONE : type;
        if (effectiveType == PrintingType.NONE) {
            return new PrintingSelection(PrintingType.NONE, null, null);
        }
        String name = normalizeName(submittedName);
        String number = normalizeNumber(submittedNumber);
        if (effectiveType == PrintingType.PLAYER) {
            if (name == null || number == null) {
                throw new IllegalArgumentException("Player printing requires a player name and number.");
            }
            if (name.length() > 50) {
                throw new IllegalArgumentException("Player name cannot exceed 50 characters.");
            }
            return new PrintingSelection(effectiveType, name, number);
        }
        if (name == null || !name.matches("[A-Z ]{2,20}")) {
            throw new IllegalArgumentException("Custom printing name must contain 2 to 20 letters and spaces.");
        }
        if (number == null) {
            throw new IllegalArgumentException("Custom printing number must be between 0 and 99.");
        }
        return new PrintingSelection(effectiveType, name, number);
    }

    private CartItem toViewItem(CustomerCartItem item) {
        ProductVariant variant = item.getProductVariant();
        if (variant == null || variant.getProduct() == null) {
            throw new IllegalStateException("Cart line is missing its product variant.");
        }
        return new CartItem(item.getLineId(), variant.getId(), variant.getProduct().getName(),
                variant.getProduct().getStoredImageName(), variant.getSku(), variant.getSize(), item.getQuantity(),
                unitPrice(variant), item.getPrintingType(), item.getPrintingName(), item.getPrintingNumber(),
                printingCharge(item.getPrintingType()));
    }

    private boolean matches(CustomerCartItem item, Long variantId, PrintingSelection printing) {
        return variantId.equals(item.getProductVariant().getId())
                && item.getPrintingType() == printing.type()
                && java.util.Objects.equals(item.getPrintingName(), printing.name())
                && java.util.Objects.equals(item.getPrintingNumber(), printing.number());
    }

    private ProductVariant availableVariant(Long variantId) {
        ProductVariant variant = variantRepository.findWithProductById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found."));
        if (variant.getProduct() == null || !variant.getProduct().isActive()) {
            throw new IllegalArgumentException("This product is not available.");
        }
        return variant;
    }

    private void requireStock(int alreadyInCart, int requested, ProductVariant variant) {
        int available = Math.max(0, variant.getStockQuantity() - alreadyInCart);
        if (requested > available) {
            throw new IllegalArgumentException(
                    "Only " + available + " item(s) remain in stock for " + variant.getSku() + ".");
        }
    }

    private void requireCustomer(User customer) {
        if (customer == null || customer.getId() == null || !customer.isEnabled() || customer.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("A valid customer account is required.");
        }
    }

    private static void requireLineQuantity(int quantity) {
        if (quantity < 1 || quantity > 10) {
            throw new IllegalArgumentException("Quantity must be between 1 and 10 per cart line.");
        }
    }

    private static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String normalizeNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.matches("\\d{1,2}")) {
            return null;
        }
        return Integer.toString(Integer.parseInt(trimmed));
    }

    record PrintingSelection(PrintingType type, String name, String number) {
    }
}
