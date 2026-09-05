package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.repository.ProductImageRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jerseysee-demo-images;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.public-demo.enabled=true",
        "app.public-demo.password=Demo123!"
})
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoProductImageInitializerTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Test
    void publicCatalogCanAttachPackagedProductPhotographyToDatabaseBackedImages() {
        DemoProductImageInitializer initializer = new DemoProductImageInitializer(
                productRepository, productImageRepository, new DefaultResourceLoader());
        initializer.seedImages();

        List<Product> publicProducts = productRepository.findAll().stream()
                .filter(product -> product.getDemoSeedKey() != null
                        && product.getDemoSeedKey().startsWith("public.product."))
                .toList();

        assertThat(publicProducts).hasSizeGreaterThanOrEqualTo(20);
        assertThat(publicProducts)
                .allSatisfy(product -> {
                    assertThat(product.getStoredImageName()).isNotBlank();
                    assertThat(product.getOriginalImageName()).isNotBlank();
                    assertThat(product.getImageContentType()).isNotBlank();
                    assertThat(product.getImageSize()).isPositive();
                    assertThat(productImageRepository.findById(product.getStoredImageName()))
                            .isPresent()
                            .get()
                            .satisfies(image -> assertThat(image.getContent()).isNotEmpty());
                });
        assertThat(publicProducts.stream()
                .map(Product::getStoredImageName)
                .distinct()
                .count()).isGreaterThanOrEqualTo(20);
    }
}
