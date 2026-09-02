package bd.edu.seu.jerseysee.model;

import bd.edu.seu.jerseysee.model.enums.SizeOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SizeOption size;

    @Column(nullable = false, unique = true)
    private String sku;

    /** Internal nullable key used only to identify deterministic demo seed rows. */
    @Column(unique = true, length = 160)
    private String demoSeedKey;

    @Column(nullable = false)
    private int stockQuantity;

    @Column(precision = 12, scale = 2)
    private BigDecimal priceAdjustment;

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public SizeOption getSize() { return size; }
    public void setSize(SizeOption size) { this.size = size; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getDemoSeedKey() { return demoSeedKey; }
    public void setDemoSeedKey(String demoSeedKey) { this.demoSeedKey = demoSeedKey; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public BigDecimal getPriceAdjustment() { return priceAdjustment; }
    public void setPriceAdjustment(BigDecimal priceAdjustment) { this.priceAdjustment = priceAdjustment; }
}
