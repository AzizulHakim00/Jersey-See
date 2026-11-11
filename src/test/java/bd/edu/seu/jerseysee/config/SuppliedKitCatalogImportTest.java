package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.repository.ProductImageRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jerseysee-render-kits;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "RENDER=true",
        "app.public-demo.enabled=false",
        "app.demo-data.enabled=false",
        "app.seed-admin.enabled=false"
})
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SuppliedKitCatalogImportTest {
    @Autowired private PublicDemoCatalogInitializer catalogInitializer;
    @Autowired private DemoProductImageInitializer imageInitializer;
    @Autowired private ProductRepository products;
    @Autowired private ProductImageRepository images;
    @Autowired private UserRepository users;

    @Test
    void renderImportsOnlyTheSuppliedKitsAndPreservesStaffEditsOnRestart() {
        assertThat(users.count()).isZero();
        assertThat(products.findAll()).hasSize(22).allSatisfy(product -> {
            assertThat(product.getProductType()).isEqualTo(ProductType.JERSEY);
            assertThat(product.getStoredImageName()).endsWith("-supplied.jpg");
            assertThat(images.findById(product.getStoredImageName())).isPresent();
        });

        Product edited = products.findByDemoSeedKey("public.product.barcelona-home-fan").orElseThrow();
        edited.setBasePrice(new BigDecimal("1799.00"));
        edited.setStoredImageName("cloud-custom-photo.jpg");
        edited.setActive(false);
        edited.getVariants().get(0).setStockQuantity(2);
        products.saveAndFlush(edited);

        catalogInitializer.initialize();
        imageInitializer.onApplicationReady();

        assertThat(products.count()).isEqualTo(22);
        Product restored = products.findByDemoSeedKey(edited.getDemoSeedKey()).orElseThrow();
        assertThat(restored.getBasePrice()).isEqualByComparingTo("1799.00");
        assertThat(restored.getStoredImageName()).isEqualTo("cloud-custom-photo.jpg");
        assertThat(restored.isActive()).isFalse();
        assertThat(restored.getVariants()).anyMatch(variant -> variant.getStockQuantity() == 2);
    }
}
