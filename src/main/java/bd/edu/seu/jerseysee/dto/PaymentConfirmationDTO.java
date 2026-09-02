package bd.edu.seu.jerseysee.dto;

import jakarta.validation.constraints.Size;

public class PaymentConfirmationDTO {

    @Size(max = 100, message = "Transaction ID is too long.")
    private String transactionId;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
