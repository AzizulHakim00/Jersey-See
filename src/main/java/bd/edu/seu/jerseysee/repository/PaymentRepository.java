package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.Payment;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"customerOrder", "customerOrder.customer"})
    @Query("select payment from Payment payment where payment.id = :id")
    Optional<Payment> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"customerOrder", "customerOrder.customer"})
    @Query("select payment from Payment payment where payment.id = :id")
    Optional<Payment> findDetailedByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customerOrder", "customerOrder.customer"})
    @Query("select payment from Payment payment order by payment.id desc")
    List<Payment> findAllDetailed();
}
