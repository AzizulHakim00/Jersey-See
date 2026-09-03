package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
}
