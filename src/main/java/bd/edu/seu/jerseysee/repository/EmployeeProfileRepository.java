package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.EmployeeProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
    Optional<EmployeeProfile> findByEmployeeCode(String employeeCode);

    @EntityGraph(attributePaths = "user")
    @Query("select employee from EmployeeProfile employee")
    List<EmployeeProfile> findAllWithUser();
}
