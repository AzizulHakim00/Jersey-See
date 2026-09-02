package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.PaymentConfirmationDTO;
import bd.edu.seu.jerseysee.model.Payment;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import bd.edu.seu.jerseysee.model.enums.PaymentStatus;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository);
    }

    @Test
    void managerCanViewPaymentsButCannotConfirmThem() {
        User manager = user(Role.MANAGER);
        when(paymentRepository.findAllDetailed()).thenReturn(List.of(new Payment()));

        assertThat(paymentService.listFor(manager)).hasSize(1);
        assertThatThrownBy(() -> paymentService.confirm(3L, new PaymentConfirmationDTO(), manager))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cashierConfirmationMarksPendingPaymentPaidAndSetsDate() {
        Payment payment = new Payment();
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findDetailedByIdForUpdate(3L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        PaymentConfirmationDTO confirmation = new PaymentConfirmationDTO();
        confirmation.setTransactionId(" counter-42 ");

        Payment confirmed = paymentService.confirm(3L, confirmation, user(Role.CASHIER));

        assertThat(confirmed.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(confirmed.getPaymentDate()).isNotNull();
        assertThat(confirmed.getTransactionId()).isEqualTo("counter-42");
        verify(paymentRepository).save(payment);
    }

    @Test
    void confirmingAlreadyPaidPaymentLoadsWithWriteLockAndDoesNotOverwriteTransactionOrDate() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PAID);
        LocalDateTime paidAt = LocalDateTime.of(2026, 9, 1, 10, 30);
        payment.setPaymentDate(paidAt);
        payment.setTransactionId("original-transaction");
        when(paymentRepository.findDetailedByIdForUpdate(3L)).thenReturn(Optional.of(payment));
        PaymentConfirmationDTO confirmation = new PaymentConfirmationDTO();
        confirmation.setTransactionId("replacement-transaction");

        Payment confirmed = paymentService.confirm(3L, confirmation, user(Role.ADMIN));

        assertThat(confirmed.getPaymentDate()).isEqualTo(paidAt);
        assertThat(confirmed.getTransactionId()).isEqualTo("original-transaction");
        verify(paymentRepository).findDetailedByIdForUpdate(3L);
        verify(paymentRepository, never()).save(payment);
    }

    private User user(Role role) {
        User user = new User();
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}
