package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.CatalogFilter;
import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.enums.JerseyEdition;
import bd.edu.seu.jerseysee.model.enums.KitType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import java.math.BigDecimal;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
class ProductCatalogFilterTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void combinesAllPublicCatalogFiltersAndAlwaysExcludesInactiveProducts() {
        Category category = new Category();
        category.setName("National Jerseys");
        category = categoryRepository.saveAndFlush(category);
        productRepository.saveAndFlush(product(category, "Bangladesh National Home", true, SizeOption.M, 4));
        productRepository.saveAndFlush(product(category, "Bangladesh National Home Archive", false, SizeOption.M, 4));
        productRepository.saveAndFlush(product(category, "Bangladesh National Home Sold Out", true, SizeOption.M, 0));

        CatalogFilter filter = new CatalogFilter();
        filter.setKeyword("national home");
        filter.setCategoryId(category.getId());
        filter.setProductType(ProductType.JERSEY);
        filter.setClubOrCountry("Bangladesh");
        filter.setEdition(JerseyEdition.PLAYER);
        filter.setKitType(KitType.HOME);
        filter.setSize(SizeOption.M);
        filter.setAvailable(true);
        filter.setMinimumPrice(new BigDecimal("70.00"));
        filter.setMaximumPrice(new BigDecimal("90.00"));
        ProductService service = new ProductService(productRepository, variantRepository, categoryRepository,
                mock(FileStorageService.class));

        Page<Product> matches = service.search(filter, PageRequest.of(0, 12));

        assertThat(matches.getContent()).extracting(Product::getName)
                .containsExactly("Bangladesh National Home");
    }

    @Test
    void unavailableWithoutSizeExcludesMixedStockAndIncludesProductWithNoVariants() {
        Category category = category("Availability");
        Product mixedStock = product(category, "Mixed stock", true, SizeOption.M, 0);
        mixedStock.addVariant(variant(SizeOption.L, "MIXED-L", 5));
        productRepository.saveAndFlush(mixedStock);
        productRepository.saveAndFlush(product(category, "Sold out", true, SizeOption.M, 0));
        productRepository.saveAndFlush(productWithoutVariants(category, "No variants"));
        CatalogFilter filter = new CatalogFilter();
        filter.setAvailable(false);

        Page<Product> matches = service().search(filter, PageRequest.of(0, 12));

        assertThat(matches.getContent()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Sold out", "No variants");
    }

    @Test
    void unavailableWithSizeUsesStockForThatSize() {
        Category category = category("Size Availability");
        Product mixedStock = product(category, "Mixed sizes", true, SizeOption.M, 0);
        mixedStock.addVariant(variant(SizeOption.L, "MIXED-SIZES-L", 5));
        productRepository.saveAndFlush(mixedStock);

        CatalogFilter mediumUnavailable = new CatalogFilter();
        mediumUnavailable.setSize(SizeOption.M);
        mediumUnavailable.setAvailable(false);
        CatalogFilter largeUnavailable = new CatalogFilter();
        largeUnavailable.setSize(SizeOption.L);
        largeUnavailable.setAvailable(false);

        assertThat(service().search(mediumUnavailable, PageRequest.of(0, 12)).getContent())
                .extracting(Product::getName).containsExactly("Mixed sizes");
        assertThat(service().search(largeUnavailable, PageRequest.of(0, 12))).isEmpty();
    }

    @Test
    void searchInitializesVariantsForTheNonEmptyCatalogPage() {
        Category category = category("Rendered catalog");
        productRepository.saveAndFlush(product(category, "Rendered product", true, SizeOption.M, 4));

        Page<Product> products = service().search(new CatalogFilter(), PageRequest.of(0, 12));

        assertThat(products.getContent()).isNotEmpty();
        assertThat(Hibernate.isInitialized(products.getContent().get(0).getVariants())).isTrue();
    }

    @Test
    void managementListInitializesVariantsForTheNonEmptyPage() {
        Category category = category("Rendered management");
        productRepository.saveAndFlush(product(category, "Managed product", true, SizeOption.M, 4));

        Page<Product> products = service().listForManagement(PageRequest.of(0, 20));

        assertThat(products.getContent()).isNotEmpty();
        assertThat(Hibernate.isInitialized(products.getContent().get(0).getVariants())).isTrue();
    }

    private Product product(Category category, String name, boolean active, SizeOption size, int stock) {
        Product product = new Product();
        product.setCategory(category);
        product.setName(name);
        product.setDescription("Official home football shirt");
        product.setBrand("JerseySee");
        product.setProductType(ProductType.JERSEY);
        product.setClubOrCountry("Bangladesh");
        product.setSeason("2026/27");
        product.setKitType(KitType.HOME);
        product.setJerseyEdition(JerseyEdition.PLAYER);
        product.setBasePrice(new BigDecimal("80.00"));
        product.setActive(active);
        product.addVariant(variant(size, name.replace(' ', '-').toUpperCase(), stock));
        return product;
    }

    private Product productWithoutVariants(Category category, String name) {
        Product product = product(category, name, true, SizeOption.M, 0);
        product.removeVariant(product.getVariants().get(0));
        return product;
    }

    private ProductVariant variant(SizeOption size, String sku, int stock) {
        ProductVariant variant = new ProductVariant();
        variant.setSize(size);
        variant.setSku(sku);
        variant.setStockQuantity(stock);
        variant.setPriceAdjustment(new BigDecimal("5.00"));
        return variant;
    }

    private Category category(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.saveAndFlush(category);
    }

    private ProductService service() {
        return new ProductService(productRepository, variantRepository, categoryRepository,
                mock(FileStorageService.class));
    }
}
