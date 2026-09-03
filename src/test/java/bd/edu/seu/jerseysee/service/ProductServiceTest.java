package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.ProductDTO;
import bd.edu.seu.jerseysee.dto.ProductVariantDTO;
import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    private ProductService productService;
    private Category category;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, variantRepository, categoryRepository,
                fileStorageService);
        category = new Category();
        category.setName("Jerseys");
        lenient().when(categoryRepository.findById(7L)).thenReturn(Optional.of(category));
        lenient().when(productRepository.saveAndFlush(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void createPersistsPerSizeStockAndBigDecimalPriceAdjustment() {
        ProductDTO product = product();
        ProductVariantDTO medium = variant(SizeOption.M, " jsy-m ", 9, "12.50");
        ProductVariantDTO large = variant(SizeOption.L, "JSY-L", 0, null);
        product.setVariants(List.of(medium, large));
        when(variantRepository.existsBySkuIgnoreCase(any())).thenReturn(false);

        Product created = productService.create(product, null);

        assertThat(created.getVariants()).hasSize(2);
        assertThat(created.getVariants().get(0).getSku()).isEqualTo("JSY-M");
        assertThat(created.getVariants().get(0).getStockQuantity()).isEqualTo(9);
        assertThat(productService.priceFor(created.getVariants().get(0))).isEqualByComparingTo("92.50");
        assertThat(productService.priceFor(created.getVariants().get(1))).isEqualByComparingTo("80.00");
    }

    @Test
    void createRejectsDuplicateSkuBeforeSavingProduct() {
        ProductDTO product = product();
        product.setVariants(List.of(variant(SizeOption.M, "JSY-M", 2, "0.00")));
        when(variantRepository.existsBySkuIgnoreCase("JSY-M")).thenReturn(true);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> productService.create(product, null))
                .withMessage("Variant SKU already exists: JSY-M");
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsMissingProductDataWithoutNullPointerException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> productService.create(null, null))
                .withMessage("Product data is incomplete.");
    }

    @Test
    void createStoresValidatedImageMetadata() {
        ProductDTO product = product();
        MockMultipartFile image = new MockMultipartFile("image", "shirt.png", "image/png", new byte[] {1});
        when(fileStorageService.store(image)).thenReturn(
                new ProductImageStorage.StoredFile("uuid.png", "shirt.png", "image/png", 1));

        Product created = productService.create(product, image);

        assertThat(created.getStoredImageName()).isEqualTo("uuid.png");
        assertThat(created.getOriginalImageName()).isEqualTo("shirt.png");
        assertThat(created.getImageContentType()).isEqualTo("image/png");
        assertThat(created.getImageSize()).isEqualTo(1L);
    }

    @Test
    void updateDeletesOldImageOnlyAfterTransactionCommit() {
        Product existing = existingProduct(true);
        existing.setStoredImageName("old.png");
        when(productRepository.findWithVariantsById(15L)).thenReturn(Optional.of(existing));
        MockMultipartFile replacement = new MockMultipartFile("image", "new.png", "image/png", new byte[] {2});
        when(fileStorageService.store(replacement)).thenReturn(
                new ProductImageStorage.StoredFile("new.png", "new.png", "image/png", 1));
        beginTransactionSynchronization();

        Product updated = productService.update(15L, product(), replacement);

        assertThat(updated.getStoredImageName()).isEqualTo("new.png");
        verify(productRepository).saveAndFlush(existing);
        verify(fileStorageService, never()).delete("old.png");
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);
        verify(fileStorageService).delete("old.png");
        verify(fileStorageService, never()).delete("new.png");
    }

    @Test
    void updateDeletesNewImageOnTransactionRollbackAndKeepsOldImage() {
        Product existing = existingProduct(true);
        existing.setStoredImageName("old.png");
        when(productRepository.findWithVariantsById(15L)).thenReturn(Optional.of(existing));
        MockMultipartFile replacement = new MockMultipartFile("image", "new.png", "image/png", new byte[] {2});
        when(fileStorageService.store(replacement)).thenReturn(
                new ProductImageStorage.StoredFile("new.png", "new.png", "image/png", 1));
        beginTransactionSynchronization();

        productService.update(15L, product(), replacement);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileStorageService).delete("new.png");
        verify(fileStorageService, never()).delete("old.png");
    }

    @Test
    void updateDeletesNewImageImmediatelyWhenSaveAndFlushFails() {
        Product existing = existingProduct(true);
        existing.setStoredImageName("old.png");
        when(productRepository.findWithVariantsById(15L)).thenReturn(Optional.of(existing));
        MockMultipartFile replacement = new MockMultipartFile("image", "new.png", "image/png", new byte[] {2});
        when(fileStorageService.store(replacement)).thenReturn(
                new ProductImageStorage.StoredFile("new.png", "new.png", "image/png", 1));
        DataIntegrityViolationException failure = new DataIntegrityViolationException("category_id cannot be null");
        when(productRepository.saveAndFlush(existing)).thenThrow(failure);
        beginTransactionSynchronization();

        assertThatThrownBy(() -> productService.update(15L, product(), replacement)).isSameAs(failure);

        verify(fileStorageService).delete("new.png");
        verify(fileStorageService, never()).delete("old.png");
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    void failedProductSaveLetsTransactionalImageStorageRollBackWithoutASecondDelete() {
        Product existing = existingProduct(true);
        existing.setStoredImageName("old.png");
        when(productRepository.findWithVariantsById(15L)).thenReturn(Optional.of(existing));
        MockMultipartFile replacement = new MockMultipartFile("image", "new.png", "image/png", new byte[] {2});
        when(fileStorageService.store(replacement)).thenReturn(
                new ProductImageStorage.StoredFile("new.png", "new.png", "image/png", 1));
        when(fileStorageService.participatesInProductTransaction()).thenReturn(true);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("category_id cannot be null");
        when(productRepository.saveAndFlush(existing)).thenThrow(failure);
        beginTransactionSynchronization();

        assertThatThrownBy(() -> productService.update(15L, product(), replacement)).isSameAs(failure);

        verify(fileStorageService, never()).delete("new.png");
        verify(fileStorageService, never()).delete("old.png");
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    void createTranslatesIdentifiableSkuIntegrityFailure() {
        ProductDTO product = product();
        product.setVariants(List.of(variant(SizeOption.M, "JSY-M", 2, "0.00")));
        when(variantRepository.existsBySkuIgnoreCase("JSY-M")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate product_variant sku"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> productService.create(product, null))
                .withMessage("A variant SKU already exists.");
    }

    @Test
    void createPreservesUnrelatedIntegrityFailure() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException("category_id cannot be null");
        when(productRepository.saveAndFlush(any(Product.class))).thenThrow(failure);

        assertThatThrownBy(() -> productService.create(product(), null)).isSameAs(failure);
    }

    @Test
    void publicLookupRejectsInactiveProductButManagementLookupReturnsIt() {
        Product inactive = existingProduct(false);
        when(productRepository.findWithVariantsByIdAndActiveTrue(15L)).thenReturn(Optional.empty());
        when(productRepository.findWithVariantsById(15L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> productService.getActive(15L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found.");
        assertThat(productService.getForManagement(15L)).isSameAs(inactive);
    }

    @Test
    void addingVariantRejectsNegativeStock() {
        Product existing = existingProduct(true);
        when(productRepository.findWithVariantsById(15L)).thenReturn(Optional.of(existing));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> productService.addVariant(15L,
                        variant(SizeOption.M, "JSY-M", -1, "0.00")))
                .withMessage("Stock quantity cannot be negative.");
    }

    private ProductDTO product() {
        ProductDTO product = new ProductDTO();
        product.setCategoryId(7L);
        product.setName("National Home Jersey");
        product.setDescription("Official-style jersey");
        product.setBrand("JerseySee");
        product.setProductType(ProductType.JERSEY);
        product.setBasePrice(new BigDecimal("80.00"));
        product.setActive(true);
        return product;
    }

    private ProductVariantDTO variant(SizeOption size, String sku, int stock, String adjustment) {
        ProductVariantDTO variant = new ProductVariantDTO();
        variant.setSize(size);
        variant.setSku(sku);
        variant.setStockQuantity(stock);
        variant.setPriceAdjustment(adjustment == null ? null : new BigDecimal(adjustment));
        return variant;
    }

    private Product existingProduct(boolean active) {
        Product product = new Product();
        product.setCategory(category);
        product.setName("Existing");
        product.setDescription("Existing description");
        product.setBrand("JerseySee");
        product.setProductType(ProductType.JERSEY);
        product.setBasePrice(new BigDecimal("50.00"));
        product.setActive(active);
        return product;
    }

    private void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private void completeTransaction(int status) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        if (status == TransactionSynchronization.STATUS_COMMITTED) {
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        }
        synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }
}
