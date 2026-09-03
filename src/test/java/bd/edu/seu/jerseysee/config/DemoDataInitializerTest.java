package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.EmployeeProfile;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.EmployeeProfileRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jerseysee-demo-seed;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DemoDataInitializerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    @Autowired
    private EntityManager entityManager;

    @Test
    void demoProfileSeedsLocalAccountsWithEveryRoleAndSecurePasswords() {
        assertThat(userRepository.count()).isEqualTo(5);
        assertThat(userRepository.findAll()).extracting(user -> user.getRole())
                .containsExactlyInAnyOrder(Role.CUSTOMER, Role.SALESMAN, Role.CASHIER, Role.MANAGER, Role.ADMIN);
        assertThat(employeeProfileRepository.count()).isEqualTo(4);
        assertThat(userRepository.findAll())
                .allSatisfy(user -> assertThat(passwordEncoder.matches("Demo123!", user.getPassword())).isTrue());
        assertThat(userRepository.findAll()).allSatisfy(user -> assertThat(user.getEmail()).endsWith("@demo.local"));
    }

    @Test
    void demoProfileSeedsDistinctSkusStockStatesAndRepresentativeCatalog() {
        assertThat(categoryRepository.count()).isGreaterThanOrEqualTo(5);
        assertThat(productRepository.count()).isGreaterThanOrEqualTo(10);
        assertThat(categoryRepository.findAll()).extracting(category -> category.getName())
                .contains("Jerseys", "Training & Outerwear", "Footwear", "Footballs", "Accessories");

        Set<String> skus = new HashSet<>();
        java.util.List<ProductVariant> variants = variantRepository.findAll();
        for (ProductVariant variant : variants) {
            assertThat(skus.add(variant.getSku())).as("SKU %s must be unique", variant.getSku()).isTrue();
        }
        assertThat(variants).anySatisfy(variant -> assertThat(variant.getStockQuantity()).isZero());
        assertThat(variants).anySatisfy(variant -> assertThat(variant.getStockQuantity())
                .isBetween(1, 3));
        Map<Long, Long> variantsPerProduct = variants.stream()
                .collect(Collectors.groupingBy(variant -> variant.getProduct().getId(), Collectors.counting()));
        assertThat(variantsPerProduct.values()).allSatisfy(count -> assertThat(count).isGreaterThanOrEqualTo(2));
    }

    @Test
    void rerunningDemoInitializerDoesNotDuplicateSeedRows() throws Exception {
        long usersBefore = userRepository.count();
        long categoriesBefore = categoryRepository.count();
        long productsBefore = productRepository.count();
        long variantsBefore = variantRepository.count();

        demoDataInitializer.run();

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(categoryRepository.count()).isEqualTo(categoriesBefore);
        assertThat(productRepository.count()).isEqualTo(productsBefore);
        assertThat(variantRepository.count()).isEqualTo(variantsBefore);
    }

    @Test
    void rerunRestoresDesiredValuesForASeedSkuAlreadyOwnedByItsProduct() throws Exception {
        ProductVariant changed = variantRepository.findByDemoSeedKey("demo.variant.metro-city-home-fan.mc-hf-s")
                .orElseThrow();
        changed.setSize(SizeOption.XL);
        changed.setStockQuantity(99);
        changed.setPriceAdjustment(new BigDecimal("700.00"));
        variantRepository.saveAndFlush(changed);

        demoDataInitializer.run();

        ProductVariant restored = variantRepository.findByDemoSeedKey("demo.variant.metro-city-home-fan.mc-hf-s")
                .orElseThrow();
        assertThat(restored.getSize()).isEqualTo(SizeOption.S);
        assertThat(restored.getStockQuantity()).isEqualTo(12);
        assertThat(restored.getPriceAdjustment()).isEqualByComparingTo("0.00");
    }

    @Test
    void rerunSkipsCanonicalSkuOwnerWhenCreatingAnAbsentKeyedDemoVariant() throws Exception {
        ProductVariant seeded = variantRepository.findByDemoSeedKey("demo.variant.metro-city-home-fan.mc-hf-m")
                .orElseThrow();
        variantRepository.delete(seeded);
        variantRepository.flush();
        Product other = newProduct("Collision Product", "Collision", "1500.00");
        ProductVariant claimed = newVariant("MC-HF-M", SizeOption.XL, 91, "19.00");
        other.addVariant(claimed);
        other = productRepository.saveAndFlush(other);
        claimed.setSize(SizeOption.XL);
        claimed.setStockQuantity(91);
        claimed.setPriceAdjustment(new BigDecimal("19.00"));
        entityManager.clear();

        demoDataInitializer.run();

        ProductVariant fallback = variantRepository.findByDemoSeedKey("demo.variant.metro-city-home-fan.mc-hf-m")
                .orElseThrow();
        Long fallbackId = fallback.getId();
        assertThat(fallback.getSku()).isEqualTo("MC-HF-M-DEMO-2");
        assertThat(fallback.getSize()).isEqualTo(SizeOption.M);
        assertThat(fallback.getStockQuantity()).isEqualTo(18);
        assertThat(fallback.getPriceAdjustment()).isEqualByComparingTo("0.00");
        ProductVariant preserved = variantRepository.findBySku("MC-HF-M").orElseThrow();
        assertThat(preserved.getProduct().getId()).isEqualTo(other.getId());
        assertThat(preserved.getStockQuantity()).isEqualTo(91);

        long variantsAfterFirstRerun = variantRepository.count();
        demoDataInitializer.run();
        assertThat(variantRepository.count()).isEqualTo(variantsAfterFirstRerun);
        assertThat(variantRepository.findByDemoSeedKey("demo.variant.metro-city-home-fan.mc-hf-m").orElseThrow()
                .getId()).isEqualTo(fallbackId);
    }

    @Test
    void rerunKeepsExistingDemoStaffCodeWhenTheRequestedCodeBelongsToAnotherProfile() throws Exception {
        User salesman = userRepository.findByEmail("salesman@demo.local").orElseThrow();
        EmployeeProfile salesmanProfile = salesman.getEmployeeProfile();
        salesmanProfile.setEmployeeCode("LOCAL-SALES-77");
        salesmanProfile.setSalary(new BigDecimal("1.00"));
        userRepository.saveAndFlush(salesman);
        userRepository.saveAndFlush(userWithProfile("Outside Staff", "outside@demo.local", "DEMO-SLS-001"));
        entityManager.clear();

        demoDataInitializer.run();

        EmployeeProfile reconciled = userRepository.findByEmail("salesman@demo.local").orElseThrow()
                .getEmployeeProfile();
        assertThat(reconciled.getEmployeeCode()).isEqualTo("LOCAL-SALES-77");
        assertThat(reconciled.getPosition()).isEqualTo("Salesman");
        assertThat(reconciled.getSalary()).isEqualByComparingTo("28000.00");
        assertThat(employeeProfileRepository.findByEmployeeCode("DEMO-SLS-001").orElseThrow().getUser().getEmail())
                .isEqualTo("outside@demo.local");
    }

    @Test
    void rerunCreatesStableAlternativeEmployeeCodeForNewDemoStaffProfile() throws Exception {
        User salesman = userRepository.findByEmail("salesman@demo.local").orElseThrow();
        userRepository.delete(salesman);
        userRepository.flush();
        userRepository.saveAndFlush(userWithProfile("Outside Staff", "outside@demo.local", "DEMO-SLS-001"));
        entityManager.clear();

        demoDataInitializer.run();

        EmployeeProfile created = userRepository.findByEmail("salesman@demo.local").orElseThrow()
                .getEmployeeProfile();
        assertThat(created.getEmployeeCode()).isEqualTo("DEMO-SLS-001-DEMO-2");
        assertThat(created.getPosition()).isEqualTo("Salesman");
    }

    @Test
    void rerunCreatesKeyedDemoProductWithoutMutatingUnkeyedSameNameProduct() throws Exception {
        Product seeded = productRepository.findByDemoSeedKey("demo.product.metro-city-home-fan").orElseThrow();
        productRepository.delete(seeded);
        productRepository.flush();
        Product unrelated = productRepository.saveAndFlush(newProduct("Metro City Home Fan Jersey", "Duplicate", "1.00"));

        assertThatCode(() -> demoDataInitializer.run()).doesNotThrowAnyException();

        Product recreated = productRepository.findByDemoSeedKey("demo.product.metro-city-home-fan").orElseThrow();
        assertThat(recreated.getId()).isNotEqualTo(unrelated.getId());
        assertThat(recreated.getBasePrice()).isEqualByComparingTo("4200.00");
        assertThat(productRepository.findById(unrelated.getId()).orElseThrow().getBasePrice()).isEqualByComparingTo("1.00");
    }

    @Test
    void rerunDoesNotAdoptFallbackLookingVariantWithoutDemoSeedKey() throws Exception {
        Product product = productRepository.findByDemoSeedKey("demo.product.metro-city-home-fan").orElseThrow();
        ProductVariant seeded = variantRepository.findByDemoSeedKey("demo.variant.metro-city-home-fan.mc-hf-m")
                .orElseThrow();
        Long seededId = seeded.getId();
        ProductVariant unrelatedFallback = newVariant("MC-HF-M-DEMO-2", SizeOption.XL, 66, "13.00");
        product.addVariant(unrelatedFallback);
        productRepository.saveAndFlush(product);
        seeded.setSku("MANUALLY-MOVED-M-SKU");
        variantRepository.saveAndFlush(seeded);
        entityManager.clear();

        demoDataInitializer.run();

        ProductVariant reconciled = variantRepository.findByDemoSeedKey("demo.variant.metro-city-home-fan.mc-hf-m")
                .orElseThrow();
        assertThat(reconciled.getId()).isEqualTo(seededId);
        assertThat(reconciled.getSku()).isEqualTo("MANUALLY-MOVED-M-SKU");
        ProductVariant preserved = variantRepository.findBySku("MC-HF-M-DEMO-2").orElseThrow();
        assertThat(preserved.getDemoSeedKey()).isNull();
        assertThat(preserved.getSize()).isEqualTo(SizeOption.XL);
        assertThat(preserved.getStockQuantity()).isEqualTo(66);
        assertThat(preserved.getPriceAdjustment()).isEqualByComparingTo("13.00");
    }

    @Test
    void rerunRefusesToOverwriteAnUnkeyedUserWithADemoEmail() {
        User seeded = userRepository.findByDemoSeedKey("demo.user.customer").orElseThrow();
        userRepository.delete(seeded);
        userRepository.flush();
        User unrelated = new User();
        unrelated.setName("Existing Customer");
        unrelated.setEmail("customer@demo.local");
        unrelated.setPassword("existing-password-hash");
        unrelated.setPhone("01900000000");
        unrelated.setAddress("Existing customer address");
        unrelated.setRole(Role.CUSTOMER);
        userRepository.saveAndFlush(unrelated);
        entityManager.clear();

        assertThatThrownBy(() -> demoDataInitializer.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("customer@demo.local");

        User preserved = userRepository.findByEmail("customer@demo.local").orElseThrow();
        assertThat(preserved.getDemoSeedKey()).isNull();
        assertThat(preserved.getName()).isEqualTo("Existing Customer");
        assertThat(preserved.getPassword()).isEqualTo("existing-password-hash");
    }

    @Test
    void rerunRefusesToOverwriteAnUnkeyedCategoryWithADemoName() {
        Category seeded = categoryRepository
                .findByDemoSeedKey("demo.category.jerseys").orElseThrow();
        categoryRepository.delete(seeded);
        categoryRepository.flush();
        Category unrelated = new Category();
        unrelated.setName("Jerseys");
        unrelated.setDescription("Existing category description");
        categoryRepository.saveAndFlush(unrelated);
        entityManager.clear();

        assertThatThrownBy(() -> demoDataInitializer.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Jerseys");

        Category preserved = categoryRepository.findByName("Jerseys").orElseThrow();
        assertThat(preserved.getDemoSeedKey()).isNull();
        assertThat(preserved.getDescription()).isEqualTo("Existing category description");
    }

    @Test
    void localDemoDatabaseGuardAcceptsH2AndLoopbackMysql() {
        assertThatCode(() -> DemoDataInitializer.requireLocalDemoDatabase(
                "jdbc:h2:mem:jerseysee_demo;MODE=MySQL"))
                .doesNotThrowAnyException();
        assertThatCode(() -> DemoDataInitializer.requireLocalDemoDatabase(
                "jdbc:mysql://localhost:3306/jerseysee"))
                .doesNotThrowAnyException();
        assertThatCode(() -> DemoDataInitializer.requireLocalDemoDatabase(
                "jdbc:mysql://127.0.0.1:3306/jerseysee"))
                .doesNotThrowAnyException();
    }

    @Test
    void localDemoDatabaseGuardRejectsRemoteAndLookalikeHosts() {
        assertThatThrownBy(() -> DemoDataInitializer.requireLocalDemoDatabase(
                "jdbc:mysql://db.example.com:3306/jerseysee"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("local database");
        assertThatThrownBy(() -> DemoDataInitializer.requireLocalDemoDatabase(
                "jdbc:mysql://localhost.example.com:3306/jerseysee"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("local database");
    }

    private Product newProduct(String name, String brand, String price) {
        Product product = new Product();
        product.setCategory(categoryRepository.findByName("Accessories").orElseThrow());
        product.setName(name);
        product.setDescription("Test product");
        product.setBrand(brand);
        product.setProductType(ProductType.ACCESSORY);
        product.setBasePrice(new BigDecimal(price));
        product.setActive(true);
        return product;
    }

    private ProductVariant newVariant(String sku, SizeOption size, int stockQuantity, String priceAdjustment) {
        ProductVariant variant = new ProductVariant();
        variant.setSku(sku);
        variant.setSize(size);
        variant.setStockQuantity(stockQuantity);
        variant.setPriceAdjustment(new BigDecimal(priceAdjustment));
        return variant;
    }

    private User userWithProfile(String name, String email, String employeeCode) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("test-password");
        user.setPhone("01700000001");
        user.setAddress("Test address");
        user.setRole(Role.SALESMAN);
        EmployeeProfile profile = new EmployeeProfile();
        profile.setEmployeeCode(employeeCode);
        profile.setPosition("External");
        profile.setSalary(new BigDecimal("100.00"));
        profile.setJoiningDate(java.time.LocalDate.of(2026, 9, 1));
        profile.setActive(true);
        user.setEmployeeProfile(profile);
        return user;
    }
}
