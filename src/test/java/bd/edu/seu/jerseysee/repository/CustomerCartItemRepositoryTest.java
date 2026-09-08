package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.CustomerCartItem;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerCartItemRepositoryTest {

    @Autowired private CustomerCartItemRepository cartRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;

    @Test
    @Transactional
    void persistsStableOwnershipScopedCartLinesAndTotals() {
        User customer = customer("cart-one@example.com");
        User other = customer("cart-two@example.com");
        ProductVariant variant = variant("CART-TEST-SKU");

        CustomerCartItem item = new CustomerCartItem();
        item.setCustomer(customer);
        item.setProductVariant(variant);
        item.setQuantity(3);
        item.setPrintingType(PrintingType.NONE);
        item = cartRepository.saveAndFlush(item);

        String lineId = item.getLineId();
        assertThat(lineId).isNotBlank();
        assertThat(cartRepository.findByLineIdAndCustomerId(lineId, customer.getId())).isPresent();
        assertThat(cartRepository.findByLineIdAndCustomerId(lineId, other.getId())).isEmpty();
        assertThat(cartRepository.totalQuantityForCustomer(customer.getId())).isEqualTo(3);
        assertThat(cartRepository.totalQuantityForCustomer(other.getId())).isZero();

        var loaded = cartRepository.findDetailedByCustomerId(customer.getId());
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getLineId()).isEqualTo(lineId);
        assertThat(loaded.get(0).getProductVariant().getProduct().getName()).isEqualTo("Persistent Cart Test Product");
        assertThat(cartRepository.findByCustomerIdForUpdate(customer.getId())).hasSize(1);
    }

    private User customer(String email) {
        User user = new User();
        user.setName("Cart Customer");
        user.setEmail(email);
        user.setPassword("encoded-test-password");
        user.setPhone("01700000000");
        user.setAddress("Dhaka");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private ProductVariant variant(String sku) {
        Category category = new Category();
        category.setName("Persistent Cart Test Category " + sku);
        category = categoryRepository.saveAndFlush(category);

        Product product = new Product();
        product.setCategory(category);
        product.setName("Persistent Cart Test Product");
        product.setDescription("Persistent cart repository test product");
        product.setBrand("Test");
        product.setProductType(ProductType.JERSEY);
        product.setBasePrice(new BigDecimal("750.00"));
        product.setActive(true);
        product = productRepository.saveAndFlush(product);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSize(SizeOption.M);
        variant.setSku(sku);
        variant.setStockQuantity(20);
        variant.setPriceAdjustment(BigDecimal.ZERO);
        return variantRepository.saveAndFlush(variant);
    }
}
