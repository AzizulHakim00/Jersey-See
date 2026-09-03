package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.cart.CartItem;
import bd.edu.seu.jerseysee.cart.ShoppingCart;
import bd.edu.seu.jerseysee.dto.AddToCartDTO;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private ProductVariantRepository variantRepository;

    private CartService cartService;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        cartService = new CartService(variantRepository);
        variant = variant("National Home Jersey", "NAT-L", "1000.00", "50.00", 12);
        when(variantRepository.findWithProductById(7L)).thenReturn(Optional.of(variant));
    }

    @Test
    void addCalculatesCustomPrintingPerUnitFromPersistedVariantPrice() {
        ShoppingCart cart = new ShoppingCart();

        CartItem item = cartService.add(cart, add(2, PrintingType.CUSTOM, "  jamal bhuyan  ", "6"));

        assertThat(item.getUnitPrice()).isEqualByComparingTo("1050.00");
        assertThat(item.getPrintingCharge()).isEqualByComparingTo("300.00");
        assertThat(item.getLineSubtotal()).isEqualByComparingTo("2700.00");
        assertThat(item.getPrintingName()).isEqualTo("JAMAL BHUYAN");
        assertThat(item.getPrintingNumber()).isEqualTo("6");
        assertThat(cart.getSubtotal()).isEqualByComparingTo("2700.00");
    }

    @Test
    void addCarriesThePersistedProductImageIntoTheCustomerCart() {
        ShoppingCart cart = new ShoppingCart();

        CartItem item = cartService.add(cart, add(1, PrintingType.NONE, null, null));

        assertThat(item.getStoredImageName()).isEqualTo("national-home.png");
    }

    @Test
    void addMergesOnlyIdenticalVariantAndNormalizedPrinting() {
        ShoppingCart cart = new ShoppingCart();

        CartItem first = cartService.add(cart, add(1, PrintingType.PLAYER, "  Messi ", "10"));
        CartItem merged = cartService.add(cart, add(2, PrintingType.PLAYER, "MESSI", "10"));
        cartService.add(cart, add(1, PrintingType.PLAYER, "MESSI", "9"));

        assertThat(merged).isSameAs(first);
        assertThat(first.getQuantity()).isEqualTo(3);
        assertThat(first.getLineSubtotal()).isEqualByComparingTo("3750.00");
        assertThat(cart.getItems()).hasSize(2);
    }

    @Test
    void addRejectsInvalidCustomNameAndNumberWithoutMutatingCart() {
        ShoppingCart cart = new ShoppingCart();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> cartService.add(cart, add(1, PrintingType.CUSTOM, "A7", "100")))
                .withMessage("Custom printing name must contain 2 to 20 letters and spaces.");
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void addRequiresMeaningfulPlayerNameAndNumber() {
        ShoppingCart cart = new ShoppingCart();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> cartService.add(cart, add(1, PrintingType.PLAYER, "   ", null)))
                .withMessage("Player printing requires a player name and number.");
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void addRejectsAggregateVariantQuantityAboveCurrentStock() {
        ShoppingCart cart = new ShoppingCart();
        variant.setStockQuantity(3);
        cartService.add(cart, add(2, PrintingType.NONE, null, null));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> cartService.add(cart, add(2, PrintingType.CUSTOM, "JAMAL", "6")))
                .withMessage("Only 1 item(s) remain in stock for NAT-L.");
        assertThat(cart.getItems()).singleElement().extracting(CartItem::getQuantity).isEqualTo(2);
    }

    @Test
    void addRejectsMergedLineAboveTenEvenWhenStockIsAvailable() {
        ShoppingCart cart = new ShoppingCart();
        cartService.add(cart, add(6, PrintingType.NONE, null, null));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> cartService.add(cart, add(5, PrintingType.NONE, null, null)))
                .withMessage("Quantity must be between 1 and 10 per cart line.");
    }

    private AddToCartDTO add(int quantity, PrintingType printingType, String name, String number) {
        AddToCartDTO input = new AddToCartDTO();
        input.setVariantId(7L);
        input.setQuantity(quantity);
        input.setPrintingType(printingType);
        input.setPrintingName(name);
        input.setPrintingNumber(number);
        return input;
    }

    private ProductVariant variant(String name, String sku, String basePrice, String adjustment, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setBasePrice(new BigDecimal(basePrice));
        product.setActive(true);
        product.setStoredImageName("national-home.png");
        ProductVariant result = new ProductVariant();
        result.setProduct(product);
        result.setSku(sku);
        result.setSize(SizeOption.L);
        result.setPriceAdjustment(new BigDecimal(adjustment));
        result.setStockQuantity(stock);
        return result;
    }
}
