package bd.edu.seu.jerseysee.dto;

import bd.edu.seu.jerseysee.model.Product;
import java.math.BigDecimal;
import java.util.List;

/** Detached, immutable public data only. Never cache managed JPA entities or customer data. */
public record ProductCard(Long id, String name, String brand, String clubOrCountry,
        BigDecimal basePrice, boolean featured, String storedImageName,
        Enum<?> jerseyEdition, Enum<?> productType, List<Variant> variants) {
    public static ProductCard from(Product product) {
        return new ProductCard(product.getId(), product.getName(), product.getBrand(), product.getClubOrCountry(),
                product.getBasePrice(), product.isFeatured(), product.getStoredImageName(),
                product.getJerseyEdition(), product.getProductType(), product.getVariants().stream()
                    .map(v -> new Variant(v.getSize(), v.getStockQuantity())).toList());
    }
    public record Variant(Enum<?> size, int stockQuantity) {}
}
