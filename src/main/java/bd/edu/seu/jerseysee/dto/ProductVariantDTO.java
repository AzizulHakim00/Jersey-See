package bd.edu.seu.jerseysee.dto;

import bd.edu.seu.jerseysee.model.enums.SizeOption;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ProductVariantDTO {

    private Long id;

    @NotNull(message = "Select a size.")
    private SizeOption size;

    @NotBlank(message = "SKU is required.")
    @Size(max = 100, message = "SKU must be at most 100 characters.")
    private String sku;

    @Min(value = 0, message = "Stock quantity cannot be negative.")
    private int stockQuantity;

    @DecimalMin(value = "0.00", message = "Price adjustment cannot be negative.")
    private BigDecimal priceAdjustment;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SizeOption getSize() { return size; }
    public void setSize(SizeOption size) { this.size = size; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public BigDecimal getPriceAdjustment() { return priceAdjustment; }
    public void setPriceAdjustment(BigDecimal priceAdjustment) { this.priceAdjustment = priceAdjustment; }
}
