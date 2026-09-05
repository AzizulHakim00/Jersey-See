package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import bd.edu.seu.jerseysee.service.DatabaseProductImageStorage;
import bd.edu.seu.jerseysee.service.ProductImageStorage;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jerseysee-production-catalog;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.demo-data.enabled=false",
        "app.demo-catalog.enabled=true",
        "app.public-demo.enabled=false",
        "app.seed-admin.enabled=false"
})
@ActiveProfiles("production")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductionCatalogInitializationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    @Autowired
    private ProductImageStorage imageStorage;

    @Test
    void productionCanSeedCatalogWithoutKnownPasswordAccounts() {
        assertThat(productRepository.count()).isGreaterThanOrEqualTo(10);
        assertThat(userRepository.count()).isZero();
        assertThat(imageStorage).isInstanceOf(DatabaseProductImageStorage.class);
    }

    @Test
    void productionCatalogRerunPreservesInventoryAndAdministratorEdits() throws Exception {
        Product product = productRepository.findByDemoSeedKey("demo.product.metro-city-home-fan").orElseThrow();
        ProductVariant variant = product.getVariants().stream()
                .filter(candidate -> "demo.variant.metro-city-home-fan.mc-hf-s".equals(candidate.getDemoSeedKey()))
                .findFirst()
                .orElseThrow();
        product.setName("Administrator-edited home jersey");
        product.setBasePrice(new BigDecimal("4999.00"));
        product.setFeatured(false);
        product.setActive(false);
        variant.setStockQuantity(2);
        variant.setPriceAdjustment(new BigDecimal("175.00"));
        productRepository.saveAndFlush(product);

        demoDataInitializer.run();

        Product preserved = productRepository.findByDemoSeedKey("demo.product.metro-city-home-fan").orElseThrow();
        ProductVariant preservedVariant = variantRepository
                .findByDemoSeedKey("demo.variant.metro-city-home-fan.mc-hf-s").orElseThrow();
        assertThat(preserved.getName()).isEqualTo("Administrator-edited home jersey");
        assertThat(preserved.getBasePrice()).isEqualByComparingTo("4999.00");
        assertThat(preserved.isFeatured()).isFalse();
        assertThat(preserved.isActive()).isFalse();
        assertThat(preservedVariant.getStockQuantity()).isEqualTo(2);
        assertThat(preservedVariant.getPriceAdjustment()).isEqualByComparingTo("175.00");
    }
}
