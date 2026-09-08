package bd.edu.seu.jerseysee.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersistentCartLiveSmokeContractTest {

    @Test
    void liveSmokeVerifiesCustomerCartAcrossFreshLoginAndCleansItsOwnLine() throws IOException {
        String workflow = Files.readString(Path.of(".github/workflows/live-smoke.yml"));

        assertThat(workflow).contains(
                "customer@demo.local",
                "customer.cookies",
                "fresh-customer.cookies",
                "PERSISTENT_CART_SMOKE",
                "/cart/items",
                "/logout",
                "cart-before-logout.html",
                "cart-after-login.html",
                "persisted cart line missing after fresh login",
                "/remove");
        assertThat(workflow).doesNotContain("POST $BASE_URL/checkout", "--request POST \"$BASE_URL/checkout\"");
    }
}
