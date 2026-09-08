package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jerseysee-production-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.demo-data.enabled=false",
        "app.demo-catalog.enabled=false",
        "app.seed-admin.enabled=false"
})
@ActiveProfiles("production")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductionPersistenceProtectionTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void normalProductionStartupDoesNotCreateAnyDemoResetInitializer() {
        assertThat(applicationContext.getBeansOfType(PublicDemoAccountInitializer.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PublicDemoCatalogInitializer.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(DemoProductImageInitializer.class)).isEmpty();
        assertThat(userRepository.count()).isZero();
        assertThat(productRepository.count()).isZero();
    }
}
