package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    long countByActiveTrue();

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"variants", "category"})
    @Query("select product from Product product where product.id = :id and product.active = true")
    Optional<Product> findWithVariantsByIdAndActiveTrue(@Param("id") Long id);
    @EntityGraph(attributePaths = {"variants", "category"})
    @Query("select product from Product product where product.id = :id")
    Optional<Product> findWithVariantsById(@Param("id") Long id);
    Optional<Product> findByStoredImageNameAndActiveTrue(String storedImageName);
    List<Product> findAllByName(String name);
    @EntityGraph(attributePaths = {"variants", "category"})
    Optional<Product> findByDemoSeedKey(String demoSeedKey);
    List<Product> findTop8ByActiveTrueAndFeaturedTrueOrderByIdDesc();
}
