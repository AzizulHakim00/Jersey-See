package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.PaymentConfirmationDTO;
import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.model.Payment;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import bd.edu.seu.jerseysee.model.enums.PaymentStatus;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public List<Payment> listFor(User actor) {
        requirePaymentViewer(actor);
        return paymentRepository.findAllDetailed();
    }

    @Transactional
    public Payment confirm(Long paymentId, PaymentConfirmationDTO confirmation, User actor) {
        requirePaymentConfirmer(actor);
        Payment payment = paymentRepository.findDetailedByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.CONFIRMED) {
            return payment;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Only pending payments can be confirmed.");
        }
        String submittedTransactionId = confirmation == null ? null : trimToNull(confirmation.getTransactionId());
        if (submittedTransactionId != null) {
            if (submittedTransactionId.length() > 100) {
                throw new IllegalArgumentException("Transaction ID cannot exceed 100 characters.");
            }
            payment.setTransactionId(submittedTransactionId);
        }
        if (requiresTransactionId(payment.getMethod()) && trimToNull(payment.getTransactionId()) == null) {
            throw new IllegalArgumentException("Transaction ID is required for " + payment.getMethod() + " payments.");
        }
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);
        return payment;
    }

    private void requirePaymentViewer(User actor) {
        requireEnabled(actor);
        if (actor.getRole() != Role.CASHIER && actor.getRole() != Role.MANAGER && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You cannot view payments.");
        }
    }

    private void requirePaymentConfirmer(User actor) {
        requireEnabled(actor);
        if (actor.getRole() != Role.CASHIER && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You cannot confirm payments.");
        }
    }

    private void requireEnabled(User actor) {
        if (actor == null || !actor.isEnabled() || actor.getRole() == null) {
            throw new AccessDeniedException("Payment access denied.");
        }
    }

    private boolean requiresTransactionId(PaymentMethod method) {
        return method == PaymentMethod.BKASH || method == PaymentMethod.NAGAD || method == PaymentMethod.CARD;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
