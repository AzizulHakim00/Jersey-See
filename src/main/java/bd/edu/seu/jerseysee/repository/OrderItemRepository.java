package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
