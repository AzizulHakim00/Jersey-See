package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.CustomerOrder;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = {"customer", "items", "items.productVariant", "payment"})
    @Query("select distinct customerOrder from CustomerOrder customerOrder where customerOrder.id = :id")
    Optional<CustomerOrder> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select customerOrder from CustomerOrder customerOrder where customerOrder.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "items", "payment"})
    @Query("select distinct customerOrder from CustomerOrder customerOrder order by customerOrder.createdAt desc")
    List<CustomerOrder> findAllDetailed();

    @EntityGraph(attributePaths = {"customer", "items", "payment"})
    @Query("select distinct customerOrder from CustomerOrder customerOrder "
            + "where lower(customerOrder.customer.email) = lower(:email) order by customerOrder.createdAt desc")
    List<CustomerOrder> findDetailedByCustomerEmail(@Param("email") String email);
}
