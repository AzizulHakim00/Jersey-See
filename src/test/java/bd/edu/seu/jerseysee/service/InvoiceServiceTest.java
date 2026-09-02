package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.OrderItem;
import bd.edu.seu.jerseysee.model.Payment;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.OrderStatus;
import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import bd.edu.seu.jerseysee.model.enums.PaymentStatus;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceServiceTest {

    private final InvoiceService invoiceService = new InvoiceService();

    @Test
    void textInvoiceIsUtf8AndContainsLineTotalsDeliveryAndPaymentInformation() {
        CustomerOrder order = order();

        String invoice = new String(invoiceService.createTextInvoice(order), StandardCharsets.UTF_8);

        assertThat(invoice).contains(
                "JerseySee Invoice",
                "Customer: আমিনা রহমান",
                "National Home Jersey | SKU: NAT-L | Size: L",
                "Printing: CUSTOM | Name: JAMAL | Number: 6 | Charge/unit: BDT 300.00",
                "Unit price: BDT 1200.00 | Quantity: 2 | Line subtotal: BDT 3000.00",
                "Subtotal: BDT 3000.00",
                "Delivery fee: BDT 100.00",
                "Total: BDT 3100.00",
                "Payment method: BKASH",
                "Payment status: PENDING",
                "Transaction ID: txn-123");
    }

    private CustomerOrder order() {
        User customer = new User();
        customer.setName("আমিনা রহমান");
        customer.setEmail("amina@example.com");
        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setDeliveryRecipientName("আমিনা রহমান");
        order.setDeliveryPhone("01700000000");
        order.setDeliveryAddress("Dhaka");
        order.setSubtotal(new BigDecimal("3000.00"));
        order.setDeliveryFee(new BigDecimal("100.00"));
        order.setTotal(new BigDecimal("3100.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 1, 9, 0));
        OrderItem item = new OrderItem();
        item.setProductName("National Home Jersey");
        item.setSku("NAT-L");
        item.setSize(SizeOption.L);
        item.setUnitPrice(new BigDecimal("1200.00"));
        item.setPrintingType(PrintingType.CUSTOM);
        item.setCustomName("JAMAL");
        item.setCustomNumber("6");
        item.setPrintingCharge(new BigDecimal("300.00"));
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("3000.00"));
        order.addItem(item);
        Payment payment = new Payment();
        payment.setMethod(PaymentMethod.BKASH);
        payment.setAmount(new BigDecimal("3100.00"));
        payment.setTransactionId("txn-123");
        payment.setStatus(PaymentStatus.PENDING);
        order.setPayment(payment);
        return order;
    }
}
