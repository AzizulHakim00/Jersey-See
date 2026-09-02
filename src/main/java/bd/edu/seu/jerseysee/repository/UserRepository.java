package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByDemoSeedKey(String demoSeedKey);

    boolean existsByEmail(String email);
}
