package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jerseysee-no-demo-seed;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoDataProfileIsolationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void defaultProfileDoesNotCreateDemoAccountsOrCatalog() {
        assertThat(userRepository.count()).isZero();
        assertThat(categoryRepository.count()).isZero();
    }
}
