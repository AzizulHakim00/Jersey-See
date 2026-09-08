package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.dto.AddToCartDTO;
import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.CustomerCartItemRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersistentCartServiceTest {

    @Autowired private CartService cartService;
    @Autowired private CustomerCartItemRepository cartRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;

    @Test
    void addUpdateRemoveAndReloadAreDatabaseBackedAndOwnershipSafe() {
        User customer = customer("persistent-cart@example.com");
        User other = customer("other-cart@example.com");
        ProductVariant variant = variant("PERSIST-CART-SKU", new BigDecimal("750.00"), 12);

        cartService.add(customer, add(variant.getId(), 2, PrintingType.PLAYER, " Messi ", "10"));

        var firstLoad = cartService.getCart(customer);
        assertThat(firstLoad.getItems()).hasSize(1);
        var line = firstLoad.getItems().get(0);
        assertThat(line.getQuantity()).isEqualTo(2);
        assertThat(line.getPrintingName()).isEqualTo("MESSI");
        assertThat(cartRepository.count()).isEqualTo(1);
        assertThat(cartService.getTotalQuantity(customer)).isEqualTo(2);

        cartService.updateQuantity(customer, line.getLineId(), 4);
        assertThat(cartService.getCart(customer).getItems().get(0).getQuantity()).isEqualTo(4);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> cartService.updateQuantity(other, line.getLineId(), 1))
                .withMessage("Cart line not found.");

        Product product = variant.getProduct();
        product.setBasePrice(new BigDecimal("900.00"));
        productRepository.saveAndFlush(product);
        assertThat(cartService.getCart(customer).getItems().get(0).getUnitPrice()).isEqualByComparingTo("900.00");

        cartService.remove(customer, line.getLineId());
        assertThat(cartService.getCart(customer).isEmpty()).isTrue();
        assertThat(cartRepository.count()).isZero();
    }

    private User customer(String email) {
        User user = new User();
        user.setName("Persistent Customer");
        user.setEmail(email);
        user.setPassword("encoded-test-password");
        user.setPhone("01700000000");
        user.setAddress("Dhaka");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private ProductVariant variant(String sku, BigDecimal price, int stock) {
        Category category = new Category();
        category.setName("Persistent Service Category " + sku);
        category = categoryRepository.saveAndFlush(category);

        Product product = new Product();
        product.setCategory(category);
        product.setName("Persistent Service Jersey");
        product.setDescription("Persistent cart service test product");
        product.setBrand("Test");
        product.setProductType(ProductType.JERSEY);
        product.setBasePrice(price);
        product.setActive(true);
        product = productRepository.saveAndFlush(product);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSize(SizeOption.M);
        variant.setSku(sku);
        variant.setStockQuantity(stock);
        variant.setPriceAdjustment(BigDecimal.ZERO);
        return variantRepository.saveAndFlush(variant);
    }

    private AddToCartDTO add(Long variantId, int quantity, PrintingType type, String name, String number) {
        AddToCartDTO input = new AddToCartDTO();
        input.setVariantId(variantId);
        input.setQuantity(quantity);
        input.setPrintingType(type);
        input.setPrintingName(name);
        input.setPrintingNumber(number);
        return input;
    }
}
