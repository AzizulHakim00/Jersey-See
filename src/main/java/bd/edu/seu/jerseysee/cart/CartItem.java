package bd.edu.seu.jerseysee.cart;

import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class CartItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String lineId;
    private final Long variantId;
    private final String productName;
    private final String sku;
    private final SizeOption size;
    private final PrintingType printingType;
    private final String printingName;
    private final String printingNumber;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal printingCharge;
    private BigDecimal lineSubtotal;

    public CartItem(Long variantId, String productName, String sku, SizeOption size, int quantity,
            BigDecimal unitPrice, PrintingType printingType, String printingName, String printingNumber,
            BigDecimal printingCharge) {
        this.lineId = UUID.randomUUID().toString();
        this.variantId = variantId;
        this.productName = productName;
        this.sku = sku;
        this.size = size;
        this.printingType = printingType;
        this.printingName = printingName;
        this.printingNumber = printingNumber;
        setQuantity(quantity);
        refreshPrice(unitPrice, printingCharge);
    }

    public boolean matches(Long candidateVariantId, PrintingType candidateType, String candidateName,
            String candidateNumber) {
        return Objects.equals(variantId, candidateVariantId)
                && printingType == candidateType
                && Objects.equals(printingName, candidateName)
                && Objects.equals(printingNumber, candidateNumber);
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        recalculateSubtotal();
    }

    public void refreshPrice(BigDecimal unitPrice, BigDecimal printingCharge) {
        this.unitPrice = unitPrice;
        this.printingCharge = printingCharge;
        recalculateSubtotal();
    }

    private void recalculateSubtotal() {
        if (unitPrice != null && printingCharge != null) {
            lineSubtotal = unitPrice.add(printingCharge).multiply(BigDecimal.valueOf(quantity));
        }
    }

    public String getLineId() { return lineId; }
    public Long getVariantId() { return variantId; }
    public String getProductName() { return productName; }
    public String getSku() { return sku; }
    public SizeOption getSize() { return size; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public PrintingType getPrintingType() { return printingType; }
    public String getPrintingName() { return printingName; }
    public String getPrintingNumber() { return printingNumber; }
    public BigDecimal getPrintingCharge() { return printingCharge; }
    public BigDecimal getLineSubtotal() { return lineSubtotal; }
}
