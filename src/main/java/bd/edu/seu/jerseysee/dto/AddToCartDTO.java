package bd.edu.seu.jerseysee.dto;

import bd.edu.seu.jerseysee.model.enums.PrintingType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AddToCartDTO {

    @NotNull(message = "Please select a product variant.")
    private Long variantId;

    @Min(value = 1, message = "Quantity must be at least 1.")
    @Max(value = 10, message = "Quantity cannot exceed 10 per cart line.")
    private int quantity = 1;

    @NotNull(message = "Please select a printing option.")
    private PrintingType printingType = PrintingType.NONE;

    @Size(max = 50, message = "Printing name is too long.")
    private String printingName;

    @Size(max = 2, message = "Printing number must be between 0 and 99.")
    private String printingNumber;

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public PrintingType getPrintingType() { return printingType; }
    public void setPrintingType(PrintingType printingType) { this.printingType = printingType; }
    public String getPrintingName() { return printingName; }
    public void setPrintingName(String printingName) { this.printingName = printingName; }
    public String getPrintingNumber() { return printingNumber; }
    public void setPrintingNumber(String printingNumber) { this.printingNumber = printingNumber; }
}
