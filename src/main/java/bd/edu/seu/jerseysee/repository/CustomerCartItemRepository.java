package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.CustomerCartItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerCartItemRepository extends JpaRepository<CustomerCartItem, Long> {

    @EntityGraph(attributePaths = {"productVariant", "productVariant.product"})
    @Query("select item from CustomerCartItem item where item.customer.id = :customerId order by item.id")
    List<CustomerCartItem> findDetailedByCustomerId(@Param("customerId") Long customerId);

    @EntityGraph(attributePaths = {"productVariant", "productVariant.product"})
    Optional<CustomerCartItem> findByLineIdAndCustomerId(String lineId, Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from CustomerCartItem item where item.customer.id = :customerId order by item.id")
    List<CustomerCartItem> findByCustomerIdForUpdate(@Param("customerId") Long customerId);

    @Query("select coalesce(sum(item.quantity), 0) from CustomerCartItem item where item.customer.id = :customerId")
    int totalQuantityForCustomer(@Param("customerId") Long customerId);

    @Modifying
    @Query("delete from CustomerCartItem item where item.customer.id = :customerId")
    int deleteAllByCustomerId(@Param("customerId") Long customerId);
}
