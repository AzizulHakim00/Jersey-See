package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.CatalogFilter;
import bd.edu.seu.jerseysee.dto.ProductDTO;
import bd.edu.seu.jerseysee.dto.ProductVariantDTO;
import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageStorage imageStorage;

    public ProductService(ProductRepository productRepository, ProductVariantRepository variantRepository,
            CategoryRepository categoryRepository, ProductImageStorage imageStorage) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorage = imageStorage;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Product create(ProductDTO input, MultipartFile image) {
        requireProductInput(input);
        Category category = category(input.getCategoryId());
        Product product = new Product();
        apply(input, product, category);
        addInitialVariants(input.getVariants(), product);

        ProductImageStorage.StoredFile stored = null;
        if (hasUpload(image)) {
            stored = imageStorage.store(image);
            applyImage(product, stored);
        }
        try {
            Product saved = productRepository.saveAndFlush(product);
            if (stored != null) {
                deleteOnRollback(stored.storedName());
            }
            return saved;
        } catch (RuntimeException exception) {
            if (stored != null && !imageStorage.participatesInProductTransaction()) {
                imageStorage.delete(stored.storedName());
            }
            throw translateDuplicateSku(exception);
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Product update(Long id, ProductDTO input, MultipartFile replacementImage) {
        requireProductInput(input);
        Product product = getForManagement(id);
        Category category = category(input.getCategoryId());
        apply(input, product, category);

        ProductImageStorage.StoredFile replacement = null;
        String oldStoredName = product.getStoredImageName();
        if (hasUpload(replacementImage)) {
            replacement = imageStorage.store(replacementImage);
            applyImage(product, replacement);
        }
        try {
            Product saved = productRepository.saveAndFlush(product);
            if (replacement != null) {
                deleteOnRollback(replacement.storedName());
                deleteAfterCommit(oldStoredName);
            }
            return saved;
        } catch (RuntimeException exception) {
            if (replacement != null && !imageStorage.participatesInProductTransaction()) {
                imageStorage.delete(replacement.storedName());
            }
            throw translateDuplicateSku(exception);
        }
    }

    @Transactional(readOnly = true)
    public Page<Product> search(CatalogFilter filter, Pageable pageable) {
        CatalogFilter effectiveFilter = filter == null ? new CatalogFilter() : filter;
        if (effectiveFilter.getMinimumPrice() != null && effectiveFilter.getMaximumPrice() != null
                && effectiveFilter.getMinimumPrice().compareTo(effectiveFilter.getMaximumPrice()) > 0) {
            throw new IllegalArgumentException("Minimum price cannot exceed maximum price.");
        }
        return initializeVariants(productRepository.findAll(catalogSpecification(effectiveFilter), pageable));
    }

    @Transactional(readOnly = true)
    public Product getActive(Long id) {
        return productRepository.findWithVariantsByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    @Transactional(readOnly = true)
    public Product getActiveByStoredImageName(String storedName) {
        return productRepository.findByStoredImageNameAndActiveTrue(storedName)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found."));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Product getForManagement(Long id) {
        return productRepository.findWithVariantsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Page<Product> listForManagement(Pageable pageable) {
        return initializeVariants(productRepository.findAll(pageable));
    }

    @Transactional(readOnly = true)
    public List<Product> featuredProducts() {
        return initializeVariants(productRepository.findTop8ByActiveTrueAndFeaturedTrueOrderByIdDesc());
    }

    @Transactional(readOnly = true)
    public List<Category> categories() {
        return categoryRepository.findAll();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ProductVariant addVariant(Long productId, ProductVariantDTO input) {
        Product product = getForManagement(productId);
        validateVariant(input);
        String sku = normalizeSku(input.getSku());
        if (variantRepository.existsBySkuIgnoreCase(sku)) {
            throw new IllegalArgumentException("Variant SKU already exists: " + sku);
        }
        ProductVariant variant = mapVariant(input, sku);
        product.addVariant(variant);
        try {
            productRepository.saveAndFlush(product);
            return variant;
        } catch (DataIntegrityViolationException exception) {
            throw translateDuplicateSku(exception, "Variant SKU already exists: " + sku);
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ProductVariant updateVariant(Long productId, Long variantId, ProductVariantDTO input) {
        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found."));
        validateVariant(input);
        String sku = normalizeSku(input.getSku());
        if (variantRepository.existsBySkuIgnoreCaseAndIdNot(sku, variantId)) {
            throw new IllegalArgumentException("Variant SKU already exists: " + sku);
        }
        variant.setSize(input.getSize());
        variant.setSku(sku);
        variant.setStockQuantity(input.getStockQuantity());
        variant.setPriceAdjustment(normalizeAdjustment(input.getPriceAdjustment()));
        try {
            return variantRepository.saveAndFlush(variant);
        } catch (DataIntegrityViolationException exception) {
            throw translateDuplicateSku(exception, "Variant SKU already exists: " + sku);
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void deleteVariant(Long productId, Long variantId) {
        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found."));
        variantRepository.delete(variant);
        variantRepository.flush();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void delete(Long id) {
        Product product = getForManagement(id);
        String storedName = product.getStoredImageName();
        productRepository.delete(product);
        productRepository.flush();
        deleteAfterCommit(storedName);
    }

    public BigDecimal priceFor(ProductVariant variant) {
        if (variant == null || variant.getProduct() == null || variant.getProduct().getBasePrice() == null) {
            throw new IllegalArgumentException("Variant must belong to a priced product.");
        }
        return variant.getProduct().getBasePrice().add(normalizeAdjustment(variant.getPriceAdjustment()));
    }

    public ProductDTO toDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setCategoryId(product.getCategory().getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setBrand(product.getBrand());
        dto.setProductType(product.getProductType());
        dto.setClubOrCountry(product.getClubOrCountry());
        dto.setSeason(product.getSeason());
        dto.setKitType(product.getKitType());
        dto.setJerseyEdition(product.getJerseyEdition());
        dto.setSleeveType(product.getSleeveType());
        dto.setBasePrice(product.getBasePrice());
        dto.setFeatured(product.isFeatured());
        dto.setActive(product.isActive());
        return dto;
    }

    private Page<Product> initializeVariants(Page<Product> products) {
        products.forEach(product -> product.getVariants().size());
        return products;
    }

    private List<Product> initializeVariants(List<Product> products) {
        products.forEach(product -> product.getVariants().size());
        return products;
    }

    private Specification<Product> catalogSpecification(CatalogFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            if (hasText(filter.getKeyword())) {
                String pattern = "%" + filter.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("clubOrCountry")), pattern)));
            }
            if (filter.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), filter.getCategoryId()));
            }
            if (hasText(filter.getCategory())) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category").get("name")),
                        filter.getCategory().trim().toLowerCase(Locale.ROOT)));
            }
            if (filter.getProductType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productType"), filter.getProductType()));
            }
            if (hasText(filter.getClubOrCountry())) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("clubOrCountry")),
                        filter.getClubOrCountry().trim().toLowerCase(Locale.ROOT)));
            }
            if (filter.getEdition() != null) {
                predicates.add(criteriaBuilder.equal(root.get("jerseyEdition"), filter.getEdition()));
            }
            if (filter.getKitType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("kitType"), filter.getKitType()));
            }
            if (filter.getSize() != null) {
                Join<Product, ProductVariant> variants = root.join("variants", JoinType.INNER);
                query.distinct(true);
                predicates.add(criteriaBuilder.equal(variants.get("size"), filter.getSize()));
                if (Boolean.TRUE.equals(filter.getAvailable())) {
                    predicates.add(criteriaBuilder.greaterThan(variants.get("stockQuantity"), 0));
                } else if (Boolean.FALSE.equals(filter.getAvailable())) {
                    Subquery<Integer> stockedSize = query.subquery(Integer.class);
                    Root<ProductVariant> sizedVariant = stockedSize.from(ProductVariant.class);
                    stockedSize.select(criteriaBuilder.literal(1));
                    stockedSize.where(
                            criteriaBuilder.equal(sizedVariant.get("product"), root),
                            criteriaBuilder.equal(sizedVariant.get("size"), filter.getSize()),
                            criteriaBuilder.greaterThan(sizedVariant.get("stockQuantity"), 0));
                    predicates.add(criteriaBuilder.not(criteriaBuilder.exists(stockedSize)));
                }
            } else if (filter.getAvailable() != null) {
                Subquery<Integer> stockedVariant = query.subquery(Integer.class);
                Root<ProductVariant> variantRoot = stockedVariant.from(ProductVariant.class);
                stockedVariant.select(criteriaBuilder.literal(1));
                stockedVariant.where(
                        criteriaBuilder.equal(variantRoot.get("product"), root),
                        criteriaBuilder.greaterThan(variantRoot.get("stockQuantity"), 0));
                Predicate hasPositiveStock = criteriaBuilder.exists(stockedVariant);
                predicates.add(Boolean.TRUE.equals(filter.getAvailable())
                        ? hasPositiveStock : criteriaBuilder.not(hasPositiveStock));
            }
            if (filter.getMinimumPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("basePrice"), filter.getMinimumPrice()));
            }
            if (filter.getMaximumPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("basePrice"), filter.getMaximumPrice()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void apply(ProductDTO input, Product product, Category category) {
        requireProductInput(input);
        if (input.getBasePrice().signum() < 0) {
            throw new IllegalArgumentException("Base price cannot be negative.");
        }
        product.setCategory(category);
        product.setName(input.getName().trim());
        product.setDescription(input.getDescription().trim());
        product.setBrand(input.getBrand().trim());
        product.setProductType(input.getProductType());
        product.setClubOrCountry(trimToNull(input.getClubOrCountry()));
        product.setSeason(trimToNull(input.getSeason()));
        product.setKitType(input.getKitType());
        product.setJerseyEdition(input.getJerseyEdition());
        product.setSleeveType(input.getSleeveType());
        product.setBasePrice(input.getBasePrice());
        product.setFeatured(input.isFeatured());
        product.setActive(input.isActive());
    }

    private void requireProductInput(ProductDTO input) {
        if (input == null || !hasText(input.getName()) || !hasText(input.getDescription())
                || !hasText(input.getBrand()) || input.getProductType() == null || input.getBasePrice() == null) {
            throw new IllegalArgumentException("Product data is incomplete.");
        }
    }

    private void addInitialVariants(List<ProductVariantDTO> inputs, Product product) {
        if (inputs == null) {
            return;
        }
        Set<String> requestSkus = new HashSet<>();
        for (ProductVariantDTO input : inputs) {
            validateVariant(input);
            String sku = normalizeSku(input.getSku());
            if (!requestSkus.add(sku) || variantRepository.existsBySkuIgnoreCase(sku)) {
                throw new IllegalArgumentException("Variant SKU already exists: " + sku);
            }
            product.addVariant(mapVariant(input, sku));
        }
    }

    private ProductVariant mapVariant(ProductVariantDTO input, String sku) {
        ProductVariant variant = new ProductVariant();
        variant.setSize(input.getSize());
        variant.setSku(sku);
        variant.setStockQuantity(input.getStockQuantity());
        variant.setPriceAdjustment(normalizeAdjustment(input.getPriceAdjustment()));
        return variant;
    }

    private void validateVariant(ProductVariantDTO input) {
        if (input == null || input.getSize() == null || !hasText(input.getSku())) {
            throw new IllegalArgumentException("Variant size and SKU are required.");
        }
        if (input.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        if (input.getPriceAdjustment() != null && input.getPriceAdjustment().signum() < 0) {
            throw new IllegalArgumentException("Price adjustment cannot be negative.");
        }
    }

    private Category category(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Select a category.");
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
    }

    private void applyImage(Product product, ProductImageStorage.StoredFile stored) {
        product.setStoredImageName(stored.storedName());
        product.setOriginalImageName(stored.originalName());
        product.setImageContentType(stored.contentType());
        product.setImageSize(stored.size());
    }

    private RuntimeException translateDuplicateSku(RuntimeException exception) {
        return translateDuplicateSku(exception, "A variant SKU already exists.");
    }

    private RuntimeException translateDuplicateSku(RuntimeException exception, String message) {
        if (!(exception instanceof DataIntegrityViolationException) || !causeMentionsSku(exception)) {
            return exception;
        }
        return new IllegalArgumentException(message, exception);
    }

    private boolean causeMentionsSku(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().toLowerCase(Locale.ROOT).contains("sku")) {
                return true;
            }
        }
        return false;
    }

    private void deleteAfterCommit(String storedName) {
        if (!hasText(storedName)) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    imageStorage.delete(storedName);
                }
            });
        } else {
            imageStorage.delete(storedName);
        }
    }

    private void deleteOnRollback(String storedName) {
        if (imageStorage.participatesInProductTransaction()) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        imageStorage.delete(storedName);
                    }
                }
            });
        }
    }

    private BigDecimal normalizeAdjustment(BigDecimal adjustment) {
        return adjustment == null ? BigDecimal.ZERO : adjustment;
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasUpload(MultipartFile image) {
        return image != null && !image.isEmpty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
