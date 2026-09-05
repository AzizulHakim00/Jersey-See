package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.enums.JerseyEdition;
import bd.edu.seu.jerseysee.model.enums.KitType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.model.enums.SleeveType;
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"demo", "production"})
@ConditionalOnProperty(name = "app.public-demo.enabled", havingValue = "true")
public class PublicDemoCatalogInitializer {

    private static final List<JerseySeed> JERSEYS = List.of(
            jersey("barcelona-home-fan", "Barcelona Home Fan Jersey", "Barcelona", KitType.HOME, JerseyEdition.FAN, "750.00", true),
            jersey("barcelona-away-player", "Barcelona Away Player Jersey", "Barcelona", KitType.AWAY, JerseyEdition.PLAYER, "1100.00", true),
            jersey("barcelona-third-player", "Barcelona Third Player Jersey", "Barcelona", KitType.THIRD, JerseyEdition.PLAYER, "1100.00", false),
            jersey("real-madrid-home-fan", "Real Madrid Home Fan Jersey", "Real Madrid", KitType.HOME, JerseyEdition.FAN, "750.00", true),
            jersey("real-madrid-away-player", "Real Madrid Away Player Jersey", "Real Madrid", KitType.AWAY, JerseyEdition.PLAYER, "1100.00", true),
            jersey("real-madrid-third-player", "Real Madrid Third Player Jersey", "Real Madrid", KitType.THIRD, JerseyEdition.PLAYER, "1100.00", false),
            jersey("real-madrid-retro", "Real Madrid Retro Jersey", "Real Madrid", KitType.RETRO, JerseyEdition.RETRO, "1299.00", true),
            jersey("arsenal-home-fan", "Arsenal Home Fan Jersey", "Arsenal", KitType.HOME, JerseyEdition.FAN, "750.00", true),
            jersey("arsenal-away-player", "Arsenal Away Player Jersey", "Arsenal", KitType.AWAY, JerseyEdition.PLAYER, "1100.00", false),
            jersey("arsenal-third-player", "Arsenal Third Player Jersey", "Arsenal", KitType.THIRD, JerseyEdition.PLAYER, "1100.00", false),
            jersey("chelsea-home-fan", "Chelsea Home Fan Jersey", "Chelsea", KitType.HOME, JerseyEdition.FAN, "750.00", true),
            jersey("chelsea-away-player", "Chelsea Away Player Jersey", "Chelsea", KitType.AWAY, JerseyEdition.PLAYER, "1100.00", false),
            jersey("chelsea-third-player", "Chelsea Third Player Jersey", "Chelsea", KitType.THIRD, JerseyEdition.PLAYER, "1100.00", false),
            jersey("liverpool-home-fan", "Liverpool Home Fan Jersey", "Liverpool", KitType.HOME, JerseyEdition.FAN, "750.00", true),
            jersey("liverpool-away-player", "Liverpool Away Player Jersey", "Liverpool", KitType.AWAY, JerseyEdition.PLAYER, "1100.00", false),
            jersey("manchester-city-home-fan", "Manchester City Home Fan Jersey", "Manchester City", KitType.HOME, JerseyEdition.FAN, "750.00", true),
            jersey("manchester-city-away-player", "Manchester City Away Player Jersey", "Manchester City", KitType.AWAY, JerseyEdition.PLAYER, "1100.00", false),
            jersey("manchester-city-third-player", "Manchester City Third Player Jersey", "Manchester City", KitType.THIRD, JerseyEdition.PLAYER, "1100.00", false),
            jersey("manchester-united-away-player", "Manchester United Away Player Jersey", "Manchester United", KitType.AWAY, JerseyEdition.PLAYER, "1100.00", true),
            jersey("manchester-united-third-player", "Manchester United Third Player Jersey", "Manchester United", KitType.THIRD, JerseyEdition.PLAYER, "1100.00", false),
            jersey("ac-milan-retro", "AC Milan Retro Jersey", "AC Milan", KitType.RETRO, JerseyEdition.RETRO, "1299.00", true),
            jersey("juventus-94-95-retro", "Juventus 94/95 Retro Jersey", "Juventus", KitType.RETRO, JerseyEdition.RETRO, "1299.00", true)
    );

    private static final List<SimpleSeed> ESSENTIALS = List.of(
            simple("training-top", "Training Top", ProductType.ACCESSORY, "Training", "899.00", true, SizeOption.S, SizeOption.M, SizeOption.L),
            simple("training-trousers", "Training Trousers", ProductType.ACCESSORY, "Training", "999.00", false, SizeOption.S, SizeOption.M, SizeOption.L),
            simple("coach-jacket", "Coach Jacket", ProductType.ACCESSORY, "Training", "1850.00", false, SizeOption.M, SizeOption.L, SizeOption.XL),
            simple("premium-football-boots", "Premium Football Boots", ProductType.BOOTS, "Football", "3499.00", true, SizeOption.S, SizeOption.M, SizeOption.L),
            simple("indoor-futsal-shoes", "Indoor Futsal Shoes", ProductType.BOOTS, "Futsal", "2299.00", false, SizeOption.S, SizeOption.M, SizeOption.L),
            simple("training-sneakers", "Training Sneakers", ProductType.BOOTS, "Training", "2650.00", false, SizeOption.S, SizeOption.M, SizeOption.L),
            simple("match-football", "Match Football", ProductType.FOOTBALL, "Match", "1299.00", true, SizeOption.ONE_SIZE, SizeOption.ONE_SIZE),
            simple("training-football", "Training Football", ProductType.FOOTBALL, "Training", "899.00", false, SizeOption.ONE_SIZE, SizeOption.ONE_SIZE),
            simple("mini-supporter-ball", "Mini Supporter Ball", ProductType.FOOTBALL, "Supporter", "499.00", false, SizeOption.ONE_SIZE, SizeOption.ONE_SIZE),
            simple("supporter-cap", "Supporter Cap", ProductType.ACCESSORY, "Matchday", "450.00", false, SizeOption.ONE_SIZE, SizeOption.ONE_SIZE),
            simple("club-wristband", "Club Wristband", ProductType.ACCESSORY, "Matchday", "199.00", false, SizeOption.ONE_SIZE, SizeOption.ONE_SIZE),
            simple("gym-sack", "Gym Sack", ProductType.ACCESSORY, "Training", "399.00", false, SizeOption.ONE_SIZE, SizeOption.ONE_SIZE),
            simple("shin-guard", "Shin Guard", ProductType.ACCESSORY, "Football", "550.00", false, SizeOption.S, SizeOption.M, SizeOption.L)
    );

    private static final List<String> LEGACY_GENERIC_PRODUCTS = List.of(
            "demo.product.metro-city-home-fan", "demo.product.metro-city-away-player",
            "demo.product.heritage-united-retro", "demo.product.national-stars-home-fan",
            "demo.product.academy-training-top", "demo.product.touchline-coach-jacket",
            "demo.product.academy-training-trousers", "demo.product.striker-pro-boots",
            "demo.product.street-five-sneakers", "demo.product.matchday-pro-football",
            "demo.product.supporter-cap", "demo.product.tactics-board-gadget"
    );

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    public PublicDemoCatalogInitializer(CategoryRepository categoryRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    @Transactional
    public void initialize() {
        Category jerseys = ensureCategory("public.category.jerseys", "Club Jerseys", "Fan, player and retro shirts.");
        Category training = ensureCategory("public.category.training", "Training Wear", "Training tops, trousers and jackets.");
        Category footwear = ensureCategory("public.category.footwear", "Football Footwear", "Boots, futsal shoes and sneakers.");
        Category footballs = ensureCategory("public.category.footballs", "Football Equipment", "Match, training and mini footballs.");
        Category accessories = ensureCategory("public.category.accessories", "Supporter Accessories", "Affordable match-day accessories.");

        JERSEYS.forEach(seed -> ensureJersey(jerseys, seed));
        ESSENTIALS.forEach(seed -> ensureEssential(categoryFor(seed, training, footwear, footballs, accessories), seed));
        hideLegacyGenericCatalog();
    }

    private Category categoryFor(SimpleSeed seed, Category training, Category footwear, Category footballs, Category accessories) {
        if (seed.type() == ProductType.BOOTS) return footwear;
        if (seed.type() == ProductType.FOOTBALL) return footballs;
        if (seed.slug().startsWith("training-") || seed.slug().equals("coach-jacket")) return training;
        return accessories;
    }

    private Category ensureCategory(String seedKey, String name, String description) {
        Category category = categoryRepository.findByDemoSeedKey(seedKey).orElse(null);
        if (category == null) category = categoryRepository.findByName(name).orElse(null);
        if (category == null) category = new Category();
        category.setDemoSeedKey(seedKey);
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    private void ensureJersey(Category category, JerseySeed seed) {
        ensureProduct(category, "public.product." + seed.slug(), seed.name(), ProductType.JERSEY,
                seed.club(), "2025/26", seed.kitType(), seed.edition(), SleeveType.SHORT,
                seed.price(), seed.featured(), List.of(SizeOption.S, SizeOption.M, SizeOption.L, SizeOption.XL));
    }

    private void ensureEssential(Category category, SimpleSeed seed) {
        ensureProduct(category, "public.product." + seed.slug(), seed.name(), seed.type(), seed.group(),
                "2026", null, null, null, seed.price(), seed.featured(), seed.sizes());
    }

    private void ensureProduct(Category category, String seedKey, String name, ProductType type,
            String clubOrCountry, String season, KitType kitType, JerseyEdition edition,
            SleeveType sleeveType, String price, boolean featured, List<SizeOption> sizes) {
        Product product = productRepository.findByDemoSeedKey(seedKey).orElseGet(Product::new);
        product.setDemoSeedKey(seedKey);
        product.setCategory(category);
        product.setName(name);
        product.setDescription("JerseySee public portfolio demo listing. Product images and prices are presented for demonstration purposes.");
        product.setBrand("JerseySee Demo");
        product.setProductType(type);
        product.setClubOrCountry(clubOrCountry);
        product.setSeason(season);
        product.setKitType(kitType);
        product.setJerseyEdition(edition);
        product.setSleeveType(sleeveType);
        product.setBasePrice(new BigDecimal(price));
        product.setFeatured(featured);
        product.setActive(true);

        for (int index = 0; index < sizes.size(); index++) {
            ensureVariant(product, seedKey + ".variant." + index, skuFor(seedKey, index), sizes.get(index),
                    index == sizes.size() - 1 ? 4 : 12 + (index * 3));
        }
        productRepository.save(product);
    }

    private void ensureVariant(Product product, String seedKey, String sku, SizeOption size, int stock) {
        ProductVariant variant = variantRepository.findByDemoSeedKey(seedKey).orElse(null);
        if (variant == null) {
            variant = new ProductVariant();
            variant.setDemoSeedKey(seedKey);
            variant.setSku(availableSku(sku));
            product.addVariant(variant);
        }
        variant.setSize(size);
        variant.setStockQuantity(stock);
        variant.setPriceAdjustment(BigDecimal.ZERO);
    }

    private String availableSku(String requestedSku) {
        for (int index = 1; index <= 99; index++) {
            String candidate = index == 1 ? requestedSku : requestedSku + "-" + index;
            if (variantRepository.findBySku(candidate).isEmpty()) return candidate;
        }
        throw new IllegalStateException("Could not allocate public demo SKU.");
    }

    private String skuFor(String seedKey, int index) {
        return "JS-" + seedKey.replace("public.product.", "")
                .replaceAll("[^A-Za-z0-9]", "-")
                .toUpperCase(Locale.ROOT) + "-" + (index + 1);
    }

    private void hideLegacyGenericCatalog() {
        LEGACY_GENERIC_PRODUCTS.forEach(seedKey -> productRepository.findByDemoSeedKey(seedKey).ifPresent(product -> {
            product.setFeatured(false);
            product.setActive(false);
            productRepository.save(product);
        }));
    }

    private static JerseySeed jersey(String slug, String name, String club, KitType kitType,
            JerseyEdition edition, String price, boolean featured) {
        return new JerseySeed(slug, name, club, kitType, edition, price, featured);
    }

    private static SimpleSeed simple(String slug, String name, ProductType type, String group,
            String price, boolean featured, SizeOption... sizes) {
        return new SimpleSeed(slug, name, type, group, price, featured, List.of(sizes));
    }

    private record JerseySeed(String slug, String name, String club, KitType kitType,
            JerseyEdition edition, String price, boolean featured) { }

    private record SimpleSeed(String slug, String name, ProductType type, String group,
            String price, boolean featured, List<SizeOption> sizes) { }
}
