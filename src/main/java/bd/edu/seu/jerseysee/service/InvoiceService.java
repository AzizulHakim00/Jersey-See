package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.OrderItem;
import bd.edu.seu.jerseysee.model.Payment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] createTextInvoice(CustomerOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required for an invoice.");
        }
        StringBuilder invoice = new StringBuilder();
        invoice.append("JerseySee Invoice\n")
                .append("=================\n")
                .append("Order: ").append(order.getId() == null ? "Pending" : "#" + order.getId()).append('\n')
                .append("Created: ").append(order.getCreatedAt() == null ? "Pending" : DATE_TIME.format(order.getCreatedAt()))
                .append('\n')
                .append("Status: ").append(order.getStatus()).append('\n')
                .append("Customer: ").append(safe(order.getCustomer() == null ? null : order.getCustomer().getName()))
                .append('\n')
                .append("Customer email: ")
                .append(safe(order.getCustomer() == null ? null : order.getCustomer().getEmail())).append('\n')
                .append("Delivery recipient: ").append(safe(order.getDeliveryRecipientName())).append('\n')
                .append("Delivery phone: ").append(safe(order.getDeliveryPhone())).append('\n')
                .append("Delivery address: ").append(safe(order.getDeliveryAddress())).append("\n\n")
                .append("Items\n")
                .append("-----\n");
        int line = 1;
        for (OrderItem item : order.getItems()) {
            invoice.append(line++).append(". ").append(safe(item.getProductName()))
                    .append(" | SKU: ").append(safe(item.getSku()))
                    .append(" | Size: ").append(item.getSize()).append('\n')
                    .append("   Printing: ").append(item.getPrintingType())
                    .append(" | Name: ").append(safe(item.getCustomName()))
                    .append(" | Number: ").append(safe(item.getCustomNumber()))
                    .append(" | Charge/unit: BDT ").append(money(item.getPrintingCharge())).append('\n')
                    .append("   Unit price: BDT ").append(money(item.getUnitPrice()))
                    .append(" | Quantity: ").append(item.getQuantity())
                    .append(" | Line subtotal: BDT ").append(money(item.getSubtotal())).append('\n');
        }
        invoice.append('\n')
                .append("Subtotal: BDT ").append(money(order.getSubtotal())).append('\n')
                .append("Delivery fee: BDT ").append(money(order.getDeliveryFee())).append('\n')
                .append("Total: BDT ").append(money(order.getTotal())).append("\n\n");
        appendPayment(invoice, order.getPayment());
        return invoice.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendPayment(StringBuilder invoice, Payment payment) {
        invoice.append("Payment\n").append("-------\n");
        if (payment == null) {
            invoice.append("Payment information unavailable\n");
            return;
        }
        invoice.append("Payment method: ").append(payment.getMethod()).append('\n')
                .append("Payment status: ").append(payment.getStatus()).append('\n')
                .append("Payment amount: BDT ").append(money(payment.getAmount())).append('\n')
                .append("Transaction ID: ").append(safe(payment.getTransactionId())).append('\n')
                .append("Payment date: ")
                .append(payment.getPaymentDate() == null ? "Not paid" : DATE_TIME.format(payment.getPaymentDate()))
                .append('\n');
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.replaceAll("[\\r\\n]+", " ").trim();
    }
}
