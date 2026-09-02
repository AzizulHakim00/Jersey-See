package bd.edu.seu.jerseysee.dto;

import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CheckoutDTO {

    @NotBlank(message = "Delivery recipient name is required.")
    @Size(max = 255, message = "Delivery recipient name is too long.")
    private String deliveryRecipientName;

    @NotBlank(message = "Delivery phone is required.")
    @Pattern(regexp = "^[0-9+() -]{7,25}$", message = "Enter a valid delivery phone number.")
    private String deliveryPhone;

    @NotBlank(message = "Delivery address is required.")
    @Size(max = 1000, message = "Delivery address is too long.")
    private String deliveryAddress;

    @NotNull(message = "Please select a payment method.")
    private PaymentMethod paymentMethod;

    @Size(max = 100, message = "Transaction ID is too long.")
    private String transactionId;

    public String getDeliveryRecipientName() { return deliveryRecipientName; }
    public void setDeliveryRecipientName(String deliveryRecipientName) {
        this.deliveryRecipientName = deliveryRecipientName;
    }
    public String getDeliveryPhone() { return deliveryPhone; }
    public void setDeliveryPhone(String deliveryPhone) { this.deliveryPhone = deliveryPhone; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
