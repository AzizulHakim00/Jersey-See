package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.ProductVariant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);
    Optional<ProductVariant> findByDemoSeedKey(String demoSeedKey);
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

    @EntityGraph(attributePaths = "product")
    List<ProductVariant> findTop8ByStockQuantityLessThanEqualOrderByStockQuantityAsc(int maximumStock);

    @EntityGraph(attributePaths = "product")
    @Query("select variant from ProductVariant variant where variant.id = :id")
    Optional<ProductVariant> findWithProductById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select variant from ProductVariant variant join fetch variant.product where variant.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);
}
