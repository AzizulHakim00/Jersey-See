package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.enums.JerseyEdition;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jerseysee-modern-demo;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.public-demo.enabled=true"
})
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ModernDemoCatalogTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void publicDemoAccountsUseTheDocumentedManualLoginCredentials() {
        var customer = userRepository.findByEmail("customer@demo.local");
        var administrator = userRepository.findByEmail("admin@demo.local");

        assertThat(customer).isPresent();
        assertThat(administrator).isPresent();
        assertThat(passwordEncoder.matches("Demo123!", customer.orElseThrow().getPassword())).isTrue();
        assertThat(passwordEncoder.matches("Demo123!", administrator.orElseThrow().getPassword())).isTrue();
    }

    @Test
    void demoCatalogUsesRequestedBangladeshJerseyPricesAndRecognizableClubProducts() {
        var products = productRepository.findAll();

        assertThat(products).anySatisfy(product -> {
            assertThat(product.getName()).isEqualTo("Barcelona Home Fan Jersey");
            assertThat(product.getJerseyEdition()).isEqualTo(JerseyEdition.FAN);
            assertThat(product.getBasePrice()).isEqualByComparingTo("750.00");
        });
        assertThat(products).anySatisfy(product -> {
            assertThat(product.getJerseyEdition()).isEqualTo(JerseyEdition.PLAYER);
            assertThat(product.getBasePrice()).isEqualByComparingTo("1100.00");
        });
        assertThat(products).anySatisfy(product -> {
            assertThat(product.getJerseyEdition()).isEqualTo(JerseyEdition.RETRO);
            assertThat(product.getBasePrice()).isEqualByComparingTo("1299.00");
        });
    }

    @Test
    void demoCatalogIncludesFootballBootsAndTrainingEssentials() {
        assertThat(productRepository.findAll())
                .extracting(product -> product.getName())
                .contains("Match Football", "Premium Football Boots", "Training Top", "Training Trousers");
    }
}
