package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jerseysee-database-demo-opt-in;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("intellij")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DatabaseDemoDataOptInTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void directIntellijProfileSeedsAccountsAndCatalog() {
        assertThat(userRepository.count()).isEqualTo(5);
        assertThat(productRepository.count()).isGreaterThanOrEqualTo(10);
    }
}
