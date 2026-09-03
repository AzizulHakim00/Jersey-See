package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.EmployeeProfile;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.JerseyEdition;
import bd.edu.seu.jerseysee.model.enums.KitType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.model.enums.SleeveType;
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.EmployeeProfileRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import bd.edu.seu.jerseysee.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Optional demonstration data. Local demo profiles can create known-password
 * accounts and the catalog; production may opt in to the catalog only.
 */
@Component
@Profile({"demo", "mysql-demo", "intellij", "production"})
public class DemoDataInitializer implements CommandLineRunner {

    static final String DEMO_PASSWORD = "Demo123!";
    private static final String DEMO_PRODUCT_DESCRIPTION =
            "Local JerseySee demo product. Placeholder artwork is used instead of external images.";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dataSourceUrl;
    private final boolean demoDataEnabled;
    private final boolean demoCatalogEnabled;
    private final boolean adoptLegacyData;

    public DemoDataInitializer(UserRepository userRepository, CategoryRepository categoryRepository,
            EmployeeProfileRepository employeeProfileRepository,
            ProductRepository productRepository, ProductVariantRepository variantRepository,
            PasswordEncoder passwordEncoder,
            @Value("${spring.datasource.url}") String dataSourceUrl,
            @Value("${app.demo-data.enabled:false}") boolean demoDataEnabled,
            @Value("${app.demo-catalog.enabled:false}") boolean demoCatalogEnabled,
            @Value("${app.demo-data.adopt-legacy:false}") boolean adoptLegacyData) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataSourceUrl = dataSourceUrl;
        this.demoDataEnabled = demoDataEnabled;
        this.demoCatalogEnabled = demoCatalogEnabled;
        this.adoptLegacyData = adoptLegacyData;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (demoDataEnabled) {
            requireLocalDemoDatabase(dataSourceUrl);
            seedUsers();
            seedCatalog(true);
        } else if (demoCatalogEnabled) {
            seedCatalog(false);
        }
    }

    static void requireLocalDemoDatabase(String jdbcUrl) {
        LocalDemoDatabaseGuard.requireLocal(jdbcUrl);
    }

    private void seedUsers() {
        ensureCustomer("demo.user.customer", "Demo Customer", "customer@demo.local");
        ensureStaff("demo.user.salesman", "Demo Salesman", "salesman@demo.local", Role.SALESMAN,
                "DEMO-SLS-001", "Salesman", "28000.00");
        ensureStaff("demo.user.cashier", "Demo Cashier", "cashier@demo.local", Role.CASHIER,
                "DEMO-CSH-001", "Cashier", "30000.00");
        ensureStaff("demo.user.manager", "Demo Manager", "manager@demo.local", Role.MANAGER,
                "DEMO-MGR-001", "Store Manager", "45000.00");
        // This is trusted local setup, not the public staff-management workflow.
        ensureStaff("demo.user.admin", "Demo Administrator", "admin@demo.local", Role.ADMIN,
                "DEMO-ADM-001", "Administrator", "55000.00");
    }

    private void ensureCustomer(String seedKey, String name, String email) {
        User user = demoUser(seedKey, name, email, Role.CUSTOMER);
        applyUser(user, name, email, Role.CUSTOMER);
        userRepository.save(user);
    }

    private void ensureStaff(String seedKey, String name, String email, Role role, String employeeCode,
            String position, String salary) {
        User user = demoUser(seedKey, name, email, role);
        applyUser(user, name, email, role);
        EmployeeProfile profile = user.getEmployeeProfile();
        if (profile == null) {
            profile = new EmployeeProfile();
            user.setEmployeeProfile(profile);
        }
        if (profile.getEmployeeCode() == null || profile.getEmployeeCode().isBlank()) {
            profile.setEmployeeCode(availableEmployeeCode(employeeCode, user));
        }
        profile.setPosition(position);
        profile.setSalary(new BigDecimal(salary));
        profile.setJoiningDate(LocalDate.of(2026, 9, 1));
        profile.setActive(true);
        userRepository.save(user);
    }

    private User demoUser(String seedKey, String name, String email, Role role) {
        User user = userRepository.findByDemoSeedKey(seedKey).orElse(null);
        User emailOwner = userRepository.findByEmail(email).orElse(null);
        if (emailOwner != null && (user == null || !emailOwner.getId().equals(user.getId()))) {
            if (user == null && isRecognizableLegacyUser(emailOwner, name, role)) {
                user = emailOwner;
                user.setDemoSeedKey(seedKey);
            } else {
                throw new IllegalStateException("Demo data cannot use existing email: " + email);
            }
        }
        if (user == null) {
            user = new User();
            user.setDemoSeedKey(seedKey);
        }
        return user;
    }

    private boolean isRecognizableLegacyUser(User user, String name, Role role) {
        return adoptLegacyData && user.getDemoSeedKey() == null
                && Objects.equals(user.getName(), name) && user.getRole() == role
                && user.getEmail() != null && user.getEmail().endsWith("@demo.local");
    }

    private void applyUser(User user, String name, String email, Role role) {
        user.setName(name);
        user.setEmail(UserService.normalizeEmail(email));
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setPhone("01700000000");
        user.setAddress("Local demo account — not for real customer data.");
        user.setRole(role);
        user.setEnabled(true);
    }

    private void seedCatalog(boolean reconcileExisting) {
        Category jerseys = ensureCategory("demo.category.jerseys", "Jerseys",
                reconcileExisting,
                "Home, away, player and retro football shirts.");
        Category training = ensureCategory("demo.category.training-outerwear", "Training & Outerwear",
                reconcileExisting,
                "Training layers, jackets and trousers.");
        Category footwear = ensureCategory("demo.category.footwear", "Footwear",
                reconcileExisting,
                "Boots and football-inspired sneakers.");
        Category footballs = ensureCategory("demo.category.footballs", "Footballs",
                reconcileExisting,
                "Match and training footballs.");
        Category accessories = ensureCategory("demo.category.accessories", "Accessories",
                reconcileExisting,
                "Caps, gadgets and matchday accessories.");

        ensureProduct(jerseys, reconcileExisting, new ProductSpec("demo.product.metro-city-home-fan", "Metro City Home Fan Jersey", "JerseySee", ProductType.JERSEY,
                "Metro City", "2026/27", KitType.HOME, JerseyEdition.FAN, SleeveType.SHORT, "4200.00", true,
                variants("demo.variant.metro-city-home-fan", "MC-HF-S", SizeOption.S, 12, "0.00", "MC-HF-M", SizeOption.M, 18, "0.00",
                        "MC-HF-L", SizeOption.L, 4, "0.00")));
        ensureProduct(jerseys, reconcileExisting, new ProductSpec("demo.product.metro-city-away-player", "Metro City Away Player Jersey", "JerseySee", ProductType.JERSEY,
                "Metro City", "2026/27", KitType.AWAY, JerseyEdition.PLAYER, SleeveType.SHORT, "5800.00", true,
                variants("demo.variant.metro-city-away-player", "MC-AP-M", SizeOption.M, 8, "0.00", "MC-AP-L", SizeOption.L, 3, "0.00",
                        "MC-AP-XL", SizeOption.XL, 0, "0.00")));
        ensureProduct(jerseys, reconcileExisting, new ProductSpec("demo.product.heritage-united-retro", "Heritage United 1998 Retro Jersey", "JerseySee", ProductType.JERSEY,
                "Heritage United", "1998", KitType.RETRO, JerseyEdition.RETRO, SleeveType.LONG, "5100.00", true,
                variants("demo.variant.heritage-united-retro", "HU-R98-M", SizeOption.M, 6, "0.00", "HU-R98-L", SizeOption.L, 2, "0.00",
                        "HU-R98-XL", SizeOption.XL, 0, "0.00")));
        ensureProduct(jerseys, reconcileExisting, new ProductSpec("demo.product.national-stars-home-fan", "National Stars Home Fan Jersey", "JerseySee", ProductType.JERSEY,
                "National Stars", "2026", KitType.HOME, JerseyEdition.FAN, SleeveType.SHORT, "4000.00", false,
                variants("demo.variant.national-stars-home-fan", "NS-HF-S", SizeOption.S, 10, "0.00", "NS-HF-M", SizeOption.M, 14, "0.00",
                        "NS-HF-L", SizeOption.L, 5, "0.00")));
        ensureProduct(training, reconcileExisting, new ProductSpec("demo.product.academy-training-top", "Academy Training Top", "JerseySee", ProductType.ACCESSORY,
                "Training", "2026", null, null, null, "2900.00", false,
                variants("demo.variant.academy-training-top", "AT-TOP-S", SizeOption.S, 9, "0.00", "AT-TOP-M", SizeOption.M, 11, "0.00",
                        "AT-TOP-L", SizeOption.L, 3, "0.00")));
        ensureProduct(training, reconcileExisting, new ProductSpec("demo.product.touchline-coach-jacket", "Touchline Coach Jacket", "JerseySee", ProductType.ACCESSORY,
                "Training", "2026", null, null, null, "4600.00", true,
                variants("demo.variant.touchline-coach-jacket", "TC-JKT-M", SizeOption.M, 7, "0.00", "TC-JKT-L", SizeOption.L, 2, "0.00",
                        "TC-JKT-XL", SizeOption.XL, 0, "0.00")));
        ensureProduct(training, reconcileExisting, new ProductSpec("demo.product.academy-training-trousers", "Academy Training Trousers", "JerseySee", ProductType.ACCESSORY,
                "Training", "2026", null, null, null, "3200.00", false,
                variants("demo.variant.academy-training-trousers", "AT-TRS-S", SizeOption.S, 8, "0.00", "AT-TRS-M", SizeOption.M, 9, "0.00",
                        "AT-TRS-L", SizeOption.L, 2, "0.00")));
        ensureProduct(footwear, reconcileExisting, new ProductSpec("demo.product.striker-pro-boots", "Striker Pro Football Boots", "JerseySee", ProductType.BOOTS,
                "Football", "2026", null, null, null, "6900.00", true,
                variants("demo.variant.striker-pro-boots", "SPB-40", SizeOption.S, 5, "0.00", "SPB-42", SizeOption.M, 3, "0.00",
                        "SPB-44", SizeOption.L, 1, "0.00")));
        ensureProduct(footwear, reconcileExisting, new ProductSpec("demo.product.street-five-sneakers", "Street Five Sneakers", "JerseySee", ProductType.BOOTS,
                "Street Football", "2026", null, null, null, "5400.00", false,
                variants("demo.variant.street-five-sneakers", "SFS-40", SizeOption.S, 7, "0.00", "SFS-42", SizeOption.M, 6, "0.00",
                        "SFS-44", SizeOption.L, 2, "0.00")));
        ensureProduct(footballs, reconcileExisting, new ProductSpec("demo.product.matchday-pro-football", "Matchday Pro Football", "JerseySee", ProductType.FOOTBALL,
                "Match", "2026", null, null, null, "2400.00", true,
                variants("demo.variant.matchday-pro-football", "MPF-5A", SizeOption.ONE_SIZE, 15, "0.00", "MPF-5B", SizeOption.ONE_SIZE, 3, "0.00")));
        ensureProduct(accessories, reconcileExisting, new ProductSpec("demo.product.supporter-cap", "Supporter Cap", "JerseySee", ProductType.ACCESSORY,
                "Matchday", "2026", null, null, null, "1200.00", false,
                variants("demo.variant.supporter-cap", "SCAP-BLK", SizeOption.ONE_SIZE, 20, "0.00", "SCAP-NVY", SizeOption.ONE_SIZE, 5, "0.00")));
        ensureProduct(accessories, reconcileExisting, new ProductSpec("demo.product.tactics-board-gadget", "Tactics Board Gadget", "JerseySee", ProductType.ACCESSORY,
                "Training", "2026", null, null, null, "1800.00", false,
                variants("demo.variant.tactics-board-gadget", "TBG-STD", SizeOption.ONE_SIZE, 4, "0.00", "TBG-MINI", SizeOption.ONE_SIZE, 0, "0.00")));
    }

    private Category ensureCategory(String seedKey, String name, boolean reconcileExisting, String description) {
        Category category = categoryRepository.findByDemoSeedKey(seedKey).orElse(null);
        if (category != null && !reconcileExisting) {
            return category;
        }
        Category nameOwner = categoryRepository.findByName(name).orElse(null);
        if (nameOwner != null && (category == null || !nameOwner.getId().equals(category.getId()))) {
            if (category == null && adoptLegacyData && nameOwner.getDemoSeedKey() == null
                    && Objects.equals(nameOwner.getDescription(), description)) {
                category = nameOwner;
                category.setDemoSeedKey(seedKey);
            } else {
                throw new IllegalStateException("Demo data cannot use existing category: " + name);
            }
        }
        if (category == null) {
            category = new Category();
            category.setDemoSeedKey(seedKey);
        }
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    private void ensureProduct(Category category, boolean reconcileExisting, ProductSpec specification) {
        Product product = productRepository.findByDemoSeedKey(specification.demoSeedKey()).orElse(null);
        if (product == null) {
            product = recognizableLegacyProduct(category, specification);
        }
        boolean createProduct = product == null;
        if (product == null) {
            product = new Product();
        }
        if (createProduct || reconcileExisting) {
            product.setDemoSeedKey(specification.demoSeedKey());
            product.setCategory(category);
            product.setName(specification.name());
            product.setDescription(DEMO_PRODUCT_DESCRIPTION);
            product.setBrand(specification.brand());
            product.setProductType(specification.productType());
            product.setClubOrCountry(specification.clubOrCountry());
            product.setSeason(specification.season());
            product.setKitType(specification.kitType());
            product.setJerseyEdition(specification.jerseyEdition());
            product.setSleeveType(specification.sleeveType());
            product.setBasePrice(new BigDecimal(specification.basePrice()));
            product.setFeatured(specification.featured());
            product.setActive(true);
        }
        for (VariantSpec variantSpec : specification.variants()) {
            ProductVariant variant = variantRepository.findByDemoSeedKey(variantSpec.demoSeedKey()).orElse(null);
            if (variant == null) {
                variant = recognizableLegacyVariant(product, variantSpec);
            }
            boolean createVariant = variant == null;
            if (variant == null) {
                ProductVariant created = new ProductVariant();
                created.setDemoSeedKey(variantSpec.demoSeedKey());
                created.setSku(availableDemoSku(variantSpec.sku()));
                product.addVariant(created);
                variant = created;
            }
            if (!createVariant && !reconcileExisting) {
                continue;
            }
            if (variant.getProduct() == null) {
                product.addVariant(variant);
            }
            variant.setSize(variantSpec.size());
            variant.setStockQuantity(variantSpec.stockQuantity());
            variant.setPriceAdjustment(new BigDecimal(variantSpec.priceAdjustment()));
        }
        productRepository.save(product);
    }

    private Product recognizableLegacyProduct(Category category, ProductSpec specification) {
        if (!adoptLegacyData) {
            return null;
        }
        List<Product> matches = productRepository.findAllByName(specification.name()).stream()
                .filter(product -> product.getDemoSeedKey() == null)
                .filter(product -> product.getCategory() != null
                        && Objects.equals(product.getCategory().getId(), category.getId()))
                .filter(product -> Objects.equals(product.getDescription(), DEMO_PRODUCT_DESCRIPTION))
                .filter(product -> Objects.equals(product.getBrand(), specification.brand()))
                .filter(product -> product.getProductType() == specification.productType())
                .filter(product -> Objects.equals(product.getClubOrCountry(), specification.clubOrCountry()))
                .filter(product -> Objects.equals(product.getSeason(), specification.season()))
                .filter(product -> product.getKitType() == specification.kitType())
                .filter(product -> product.getJerseyEdition() == specification.jerseyEdition())
                .filter(product -> product.getSleeveType() == specification.sleeveType())
                .filter(product -> sameAmount(product.getBasePrice(), specification.basePrice()))
                .filter(product -> product.isFeatured() == specification.featured())
                .filter(Product::isActive)
                .filter(product -> product.getStoredImageName() == null
                        && product.getOriginalImageName() == null
                        && product.getImageContentType() == null
                        && product.getImageSize() == null)
                .filter(product -> hasExactLegacyVariants(product, specification.variants()))
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private boolean hasExactLegacyVariants(Product product, List<VariantSpec> expectedVariants) {
        if (product.getVariants().size() != expectedVariants.size()) {
            return false;
        }
        return expectedVariants.stream().allMatch(expected -> product.getVariants().stream()
                .anyMatch(actual -> actual.getDemoSeedKey() == null
                        && Objects.equals(actual.getSku(), expected.sku())
                        && actual.getSize() == expected.size()
                        && actual.getStockQuantity() == expected.stockQuantity()
                        && sameAmount(actual.getPriceAdjustment(), expected.priceAdjustment())));
    }

    private boolean sameAmount(BigDecimal actual, String expected) {
        return actual != null && actual.compareTo(new BigDecimal(expected)) == 0;
    }

    private ProductVariant recognizableLegacyVariant(Product product, VariantSpec specification) {
        if (!adoptLegacyData || product.getId() == null) {
            return null;
        }
        ProductVariant candidate = variantRepository.findBySku(specification.sku()).orElse(null);
        if (candidate == null || candidate.getDemoSeedKey() != null || candidate.getProduct() == null
                || !product.getId().equals(candidate.getProduct().getId())) {
            return null;
        }
        candidate.setDemoSeedKey(specification.demoSeedKey());
        return candidate;
    }

    private String availableEmployeeCode(String desiredCode, User user) {
        for (int index = 1; index <= 999; index++) {
            String candidate = collisionSafeValue(desiredCode, index);
            EmployeeProfile existing = employeeProfileRepository.findByEmployeeCode(candidate).orElse(null);
            if (existing == null || belongsTo(user, existing)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a safe demo employee code.");
    }

    private String availableDemoSku(String desiredSku) {
        for (int index = 1; index <= 999; index++) {
            String candidate = collisionSafeValue(desiredSku, index);
            ProductVariant existing = variantRepository.findBySku(candidate).orElse(null);
            if (existing == null) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a safe demo SKU.");
    }

    private boolean belongsTo(User user, EmployeeProfile profile) {
        return user.getId() != null && profile.getUser() != null && user.getId().equals(profile.getUser().getId());
    }

    private String collisionSafeValue(String desiredValue, int index) {
        return index == 1 ? desiredValue : desiredValue + "-DEMO-" + index;
    }

    private static List<VariantSpec> variants(String seedPrefix, Object... fields) {
        java.util.ArrayList<VariantSpec> variants = new java.util.ArrayList<>();
        for (int index = 0; index < fields.length; index += 4) {
            String sku = (String) fields[index];
            variants.add(new VariantSpec(seedPrefix + "." + sku.toLowerCase(java.util.Locale.ROOT), sku,
                    (SizeOption) fields[index + 1],
                    (Integer) fields[index + 2], (String) fields[index + 3]));
        }
        return variants;
    }

    private record ProductSpec(String demoSeedKey, String name, String brand, ProductType productType,
            String clubOrCountry, String season,
            KitType kitType, JerseyEdition jerseyEdition, SleeveType sleeveType, String basePrice, boolean featured,
            List<VariantSpec> variants) { }

    private record VariantSpec(String demoSeedKey, String sku, SizeOption size, int stockQuantity,
            String priceAdjustment) { }
}
