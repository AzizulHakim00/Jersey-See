package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.ProductImage;
import bd.edu.seu.jerseysee.repository.ProductImageRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
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
    private ProductImageRepository imageRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    void suppliedKitPhotoIsPersistedAndRerunIsIdempotent() {
        DemoProductImageInitializer initializer = new DemoProductImageInitializer(productRepository, imageRepository, resourceLoader);

        initializer.seedImages();
        var product = productRepository.findByDemoSeedKey("public.product.barcelona-home-fan").orElseThrow();
        String storedName = product.getStoredImageName();

        assertThat(storedName).isEqualTo("demo-barcelona-home-fan.jpg");
        assertThat(product.getOriginalImageName()).isEqualTo("barcelona-home.jpg");
        assertThat(product.getImageContentType()).isEqualTo("image/jpeg");
        assertThat(product.getImageSize()).isPositive();
        ProductImage image = imageRepository.findById(storedName).orElseThrow();
        assertThat(image.getContent()).isNotEmpty();

        long imageCount = imageRepository.count();
        initializer.seedImages();
        assertThat(imageRepository.count()).isEqualTo(imageCount);
    }

    @Test
    void existingAdministratorImageIsNeverOverwritten() {
        var product = productRepository.findByDemoSeedKey("public.product.real-madrid-home-fan").orElseThrow();
        product.setStoredImageName("custom-owner-image.jpg");
        product.setOriginalImageName("owner-upload.jpg");
        product.setImageContentType("image/jpeg");
        product.setImageSize(99L);
        productRepository.saveAndFlush(product);
        imageRepository.saveAndFlush(new ProductImage("custom-owner-image.jpg", new byte[]{1, 2, 3}));

        new DemoProductImageInitializer(productRepository, imageRepository, resourceLoader).seedImages();

        var preserved = productRepository.findByDemoSeedKey("public.product.real-madrid-home-fan").orElseThrow();
        assertThat(preserved.getStoredImageName()).isEqualTo("custom-owner-image.jpg");
        assertThat(imageRepository.findById("custom-owner-image.jpg")).isPresent();
    }
}
