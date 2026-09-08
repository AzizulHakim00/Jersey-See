package bd.edu.seu.jerseysee.model;

import bd.edu.seu.jerseysee.model.enums.PrintingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "customer_cart_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_cart_item_line_id", columnNames = "line_id"),
        indexes = {
                @Index(name = "idx_customer_cart_item_customer", columnList = "customer_id"),
                @Index(name = "idx_customer_cart_item_variant", columnList = "product_variant_id")
        })
public class CustomerCartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "line_id", nullable = false, length = 36, unique = true)
    private String lineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "printing_type", nullable = false, length = 32)
    private PrintingType printingType = PrintingType.NONE;

    @Column(name = "printing_name", length = 50)
    private String printingName;

    @Column(name = "printing_number", length = 2)
    private String printingNumber;

    @PrePersist
    void ensureLineId() {
        if (lineId == null || lineId.isBlank()) {
            lineId = UUID.randomUUID().toString();
        }
    }

    public Long getId() { return id; }
    public String getLineId() { return lineId; }
    public void setLineId(String lineId) { this.lineId = lineId; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public ProductVariant getProductVariant() { return productVariant; }
    public void setProductVariant(ProductVariant productVariant) { this.productVariant = productVariant; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public PrintingType getPrintingType() { return printingType; }
    public void setPrintingType(PrintingType printingType) { this.printingType = printingType; }
    public String getPrintingName() { return printingName; }
    public void setPrintingName(String printingName) { this.printingName = printingName; }
    public String getPrintingNumber() { return printingNumber; }
    public void setPrintingNumber(String printingNumber) { this.printingNumber = printingNumber; }
}
