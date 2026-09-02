package bd.edu.seu.jerseysee.cart;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShoppingCart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<CartItem> items = new ArrayList<>();

    public void addItem(CartItem item) {
        items.add(item);
    }

    public boolean removeItem(String lineId) {
        return items.removeIf(item -> item.getLineId().equals(lineId));
    }

    public void clear() {
        items.clear();
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getTotalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public int quantityForVariant(Long variantId) {
        return items.stream()
                .filter(item -> item.getVariantId().equals(variantId))
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public BigDecimal getSubtotal() {
        return items.stream()
                .map(CartItem::getLineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
