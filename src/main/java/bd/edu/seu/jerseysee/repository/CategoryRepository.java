package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    Optional<Category> findByDemoSeedKey(String demoSeedKey);
}
