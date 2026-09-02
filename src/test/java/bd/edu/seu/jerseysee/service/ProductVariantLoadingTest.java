package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
class ProductVariantLoadingTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void activeDetailExposesVariantsAfterEntityIsDetached() {
        Product persisted = persistProduct();
        ProductService service = service();
        entityManager.clear();

        Product detail = service.getActive(persisted.getId());
        entityManager.detach(detail);

        assertThat(Hibernate.isInitialized(detail.getVariants())).isTrue();
        assertThat(detail.getVariants()).extracting(ProductVariant::getSku).containsExactly("DETAIL-M");
    }

    @Test
    void managementDetailExposesVariantsAfterEntityIsDetached() {
        Product persisted = persistProduct();
        ProductService service = service();
        entityManager.clear();

        Product detail = service.getForManagement(persisted.getId());
        entityManager.detach(detail);

        assertThat(Hibernate.isInitialized(detail.getVariants())).isTrue();
        assertThat(detail.getVariants()).extracting(ProductVariant::getSku).containsExactly("DETAIL-M");
    }

    private Product persistProduct() {
        Category category = new Category();
        category.setName("Detail category");
        category = categoryRepository.saveAndFlush(category);
        Product product = new Product();
        product.setCategory(category);
        product.setName("Detail product");
        product.setDescription("Detail description");
        product.setBrand("JerseySee");
        product.setProductType(ProductType.JERSEY);
        product.setBasePrice(new BigDecimal("50.00"));
        product.setActive(true);
        ProductVariant variant = new ProductVariant();
        variant.setSize(SizeOption.M);
        variant.setSku("DETAIL-M");
        variant.setStockQuantity(3);
        variant.setPriceAdjustment(BigDecimal.ZERO);
        product.addVariant(variant);
        return productRepository.saveAndFlush(product);
    }

    private ProductService service() {
        return new ProductService(productRepository, variantRepository, categoryRepository,
                mock(FileStorageService.class));
    }
}
