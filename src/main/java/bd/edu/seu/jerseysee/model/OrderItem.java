package bd.edu.seu.jerseysee.model;

import bd.edu.seu.jerseysee.model.enums.PrintingType;
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
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_order_id", nullable = false)
    private CustomerOrder customerOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SizeOption size;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrintingType printingType = PrintingType.NONE;

    private String customName;
    private String customNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal printingCharge = BigDecimal.ZERO;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    public Long getId() { return id; }
    public CustomerOrder getCustomerOrder() { return customerOrder; }
    public void setCustomerOrder(CustomerOrder customerOrder) { this.customerOrder = customerOrder; }
    public ProductVariant getProductVariant() { return productVariant; }
    public void setProductVariant(ProductVariant productVariant) { this.productVariant = productVariant; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public SizeOption getSize() { return size; }
    public void setSize(SizeOption size) { this.size = size; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public PrintingType getPrintingType() { return printingType; }
    public void setPrintingType(PrintingType printingType) { this.printingType = printingType; }
    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }
    public String getCustomNumber() { return customNumber; }
    public void setCustomNumber(String customNumber) { this.customNumber = customNumber; }
    public BigDecimal getPrintingCharge() { return printingCharge; }
    public void setPrintingCharge(BigDecimal printingCharge) { this.printingCharge = printingCharge; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
