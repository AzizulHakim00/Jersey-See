package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.cart.CartItem;
import bd.edu.seu.jerseysee.cart.ShoppingCart;
import bd.edu.seu.jerseysee.dto.AddToCartDTO;
import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    public static final BigDecimal PLAYER_PRINTING_CHARGE = new BigDecimal("200.00");
    public static final BigDecimal CUSTOM_PRINTING_CHARGE = new BigDecimal("300.00");

    private final ProductVariantRepository variantRepository;

    public CartService(ProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    @Transactional(readOnly = true)
    public CartItem add(ShoppingCart cart, AddToCartDTO input) {
        if (cart == null || input == null || input.getVariantId() == null) {
            throw new IllegalArgumentException("Cart item data is incomplete.");
        }
        requireLineQuantity(input.getQuantity());
        PrintingSelection printing = validatePrinting(input.getPrintingType(), input.getPrintingName(),
                input.getPrintingNumber());
        ProductVariant variant = availableVariant(input.getVariantId());
        CartItem matching = cart.getItems().stream()
                .filter(item -> item.matches(input.getVariantId(), printing.type(), printing.name(), printing.number()))
                .findFirst()
                .orElse(null);
        int mergedQuantity = input.getQuantity() + (matching == null ? 0 : matching.getQuantity());
        requireLineQuantity(mergedQuantity);
        requireStock(cart.quantityForVariant(input.getVariantId()), input.getQuantity(), variant);

        BigDecimal unitPrice = unitPrice(variant);
        BigDecimal printingCharge = printingCharge(printing.type());
        if (matching != null) {
            matching.setQuantity(mergedQuantity);
            matching.refreshPrice(unitPrice, printingCharge);
            return matching;
        }
        CartItem item = new CartItem(input.getVariantId(), variant.getProduct().getName(),
                variant.getProduct().getStoredImageName(), variant.getSku(), variant.getSize(), input.getQuantity(),
                unitPrice, printing.type(), printing.name(), printing.number(), printingCharge);
        cart.addItem(item);
        return item;
    }

    @Transactional(readOnly = true)
    public CartItem updateQuantity(ShoppingCart cart, String lineId, int quantity) {
        if (cart == null || lineId == null) {
            throw new IllegalArgumentException("Cart line was not found.");
        }
        requireLineQuantity(quantity);
        CartItem item = cart.getItems().stream()
                .filter(candidate -> lineId.equals(candidate.getLineId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cart line was not found."));
        ProductVariant variant = availableVariant(item.getVariantId());
        int otherQuantity = cart.quantityForVariant(item.getVariantId()) - item.getQuantity();
        requireStock(otherQuantity, quantity, variant);
        item.setQuantity(quantity);
        item.refreshPrice(unitPrice(variant), printingCharge(item.getPrintingType()));
        return item;
    }

    public void remove(ShoppingCart cart, String lineId) {
        if (cart == null || lineId == null || !cart.removeItem(lineId)) {
            throw new IllegalArgumentException("Cart line was not found.");
        }
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
