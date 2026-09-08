package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CartServiceTest {

    @Test
    void unitPriceUsesCurrentPersistedProductAndVariantValues() {
        Product product = new Product();
        product.setBasePrice(new BigDecimal("1000.00"));
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setPriceAdjustment(new BigDecimal("50.00"));

        assertThat(CartService.unitPrice(variant)).isEqualByComparingTo("1050.00");
    }

    @Test
    void printingChargesRemainStable() {
        assertThat(CartService.printingCharge(PrintingType.NONE)).isEqualByComparingTo("0.00");
        assertThat(CartService.printingCharge(PrintingType.PLAYER)).isEqualByComparingTo("200.00");
        assertThat(CartService.printingCharge(PrintingType.CUSTOM)).isEqualByComparingTo("300.00");
    }

    @Test
    void printingSelectionNormalizesPlayerAndCustomValues() {
        var player = CartService.validatePrinting(PrintingType.PLAYER, "  Messi  ", "010");
        assertThat(player.name()).isEqualTo("MESSI");
        assertThat(player.number()).isEqualTo("10");

        var custom = CartService.validatePrinting(PrintingType.CUSTOM, "  jamal bhuyan  ", "6");
        assertThat(custom.name()).isEqualTo("JAMAL BHUYAN");
        assertThat(custom.number()).isEqualTo("6");
    }

    @Test
    void invalidPrintingIsRejectedBeforeAnyDatabaseMutation() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CartService.validatePrinting(PrintingType.CUSTOM, "A7", "100"))
                .withMessage("Custom printing name must contain 2 to 20 letters and spaces.");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CartService.validatePrinting(PrintingType.PLAYER, "   ", null))
                .withMessage("Player printing requires a player name and number.");
    }
}
