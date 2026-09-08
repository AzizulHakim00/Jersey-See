package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductImage;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.ProductImageRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jerseysee-public-production;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.demo-data.enabled=false",
        "app.demo-catalog.enabled=false",
        "app.public-demo.enabled=true",
        "app.seed-admin.enabled=false"
})
@ActiveProfiles("production")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PublicDemoProductionInitializationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository imageRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PublicDemoAccountInitializer accountInitializer;

    @Autowired
    private PublicDemoCatalogInitializer catalogInitializer;

    @Autowired
    private DemoProductImageInitializer imageInitializer;

    @Test
    void productionStartupSeedsCatalogBeforeAttachingRealJerseyImages() {
        assertThat(productRepository.count()).isEqualTo(35);

        Product barcelona = productRepository.findByDemoSeedKey("public.product.barcelona-home-fan")
                .orElseThrow();
        assertThat(barcelona.getStoredImageName()).isNotBlank();
        assertThat(barcelona.getImageContentType()).isEqualTo("image/jpeg");
        assertThat(barcelona.getOriginalImageName()).isEqualTo("barcelona-home.jpg");

        ProductImage image = imageRepository.findById(barcelona.getStoredImageName()).orElseThrow();
        assertThat(image.getContent())
                .isNotNull()
                .hasSizeGreaterThan(100);
        assertThat(image.getContent()[0]).isEqualTo((byte) 0xFF);
        assertThat(image.getContent()[1]).isEqualTo((byte) 0xD8);
    }

    @Test
    void productionStartupSeedsVisibleManualDemoAccountsWithKnownDemoOnlyPassword() {
        User customer = userRepository.findByEmail("customer@demo.local").orElseThrow();
        assertThat(customer.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(passwordEncoder.matches("Demo123!", customer.getPassword())).isTrue();

        User admin = userRepository.findByEmail("admin@demo.local").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches("Demo123!", admin.getPassword())).isTrue();
    }

    @Test
    void rerunningStartupInitializersDoesNotOverwritePersistedAdminOrCustomerChanges() {
        User customer = userRepository.findByEmail("customer@demo.local").orElseThrow();
        customer.setName("Persisted Customer Name");
        customer.setPhone("01811112222");
        customer.setAddress("Persisted customer address");
        userRepository.saveAndFlush(customer);

        Product barcelona = productRepository.findByDemoSeedKey("public.product.barcelona-home-fan")
                .orElseThrow();
        Product imageDonor = productRepository.findByDemoSeedKey("public.product.premium-football-boots")
                .orElseThrow();
        String customStoredImageName = imageDonor.getStoredImageName();
        String customOriginalImageName = imageDonor.getOriginalImageName();
        String customImageContentType = imageDonor.getImageContentType();
        Long customImageSize = imageDonor.getImageSize();

        barcelona.setName("Admin Edited Barcelona");
        barcelona.setBasePrice(new BigDecimal("2222.00"));
        barcelona.setFeatured(false);
        barcelona.setActive(false);
        barcelona.setStoredImageName(customStoredImageName);
        barcelona.setOriginalImageName(customOriginalImageName);
        barcelona.setImageContentType(customImageContentType);
        barcelona.setImageSize(customImageSize);
        productRepository.saveAndFlush(barcelona);

        ProductVariant variant = variantRepository
                .findByDemoSeedKey("public.product.barcelona-home-fan.variant.0")
                .orElseThrow();
        variant.setStockQuantity(2);
        variantRepository.saveAndFlush(variant);

        accountInitializer.initialize();
        catalogInitializer.initialize();
        imageInitializer.seedImages();

        User reloadedCustomer = userRepository.findByEmail("customer@demo.local").orElseThrow();
        assertThat(reloadedCustomer.getName()).isEqualTo("Persisted Customer Name");
        assertThat(reloadedCustomer.getPhone()).isEqualTo("01811112222");
        assertThat(reloadedCustomer.getAddress()).isEqualTo("Persisted customer address");

        Product reloadedBarcelona = productRepository.findByDemoSeedKey("public.product.barcelona-home-fan")
                .orElseThrow();
        assertThat(reloadedBarcelona.getName()).isEqualTo("Admin Edited Barcelona");
        assertThat(reloadedBarcelona.getBasePrice()).isEqualByComparingTo("2222.00");
        assertThat(reloadedBarcelona.isFeatured()).isFalse();
        assertThat(reloadedBarcelona.isActive()).isFalse();
        assertThat(reloadedBarcelona.getStoredImageName()).isEqualTo(customStoredImageName);
        assertThat(reloadedBarcelona.getOriginalImageName()).isEqualTo(customOriginalImageName);

        ProductVariant reloadedVariant = variantRepository
                .findByDemoSeedKey("public.product.barcelona-home-fan.variant.0")
                .orElseThrow();
        assertThat(reloadedVariant.getStockQuantity()).isEqualTo(2);
    }
}
