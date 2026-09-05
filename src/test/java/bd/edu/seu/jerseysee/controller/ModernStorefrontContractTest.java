package bd.edu.seu.jerseysee.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModernStorefrontContractTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path STATIC = Path.of("src/main/resources/static");

    @Test
    void premiumV2UsesOneCommerceHeaderAndApprovedHomeComposition() throws IOException {
        String navigation = read(TEMPLATES.resolve("fragments/navigation.html"));
        String home = read(TEMPLATES.resolve("home/index.html"));
        String footer = read(TEMPLATES.resolve("fragments/footer.html"));

        assertThat(navigation)
                .contains("data-storefront-header", "data-primary-shop-nav", "Home", "Shop", "Player edition", "Retro", "BOOTS")
                .doesNotContain("storefront-modern.css", "storefront-final.css", "class=\"category-nav\"", "class=\"store-promise\"");
        assertThat(home)
                .contains("FOOTBALL LIVES HERE", "Wear The Passion", "data-campaign-hero", "data-service-strip",
                        "Shop by category", "Featured jerseys", "data-product-rail")
                .contains("storefront-premium-v2.css", "storefront-premium-v2.js");
        assertThat(footer).contains("© 2026 Azizul Hakim Omor. All rights reserved.", "৳5,000");
    }

    @Test
    void loginUsesVisibleManualDemoCredentialsAndNoOneClickDemoUi() throws IOException {
        String login = read(TEMPLATES.resolve("auth/login.html"));
        String javascript = read(STATIC.resolve("js/app.js"));

        assertThat(login)
                .contains("Demo credentials (for testing only)", "customer@demo.local", "admin@demo.local", "Demo123!",
                        "storefront-premium-v2.css", "© 2026 Azizul Hakim Omor. All rights reserved.")
                .doesNotContain("/demo-login/customer", "/demo-login/admin", "data-demo-entry", "data-demo-account",
                        "storefront-modern.css", "storefront-final.css");
        assertThat(javascript)
                .doesNotContain("data-demo-account", "demoEmail", "demoPassword");
    }

    @Test
    void premiumAssetsAreResponsiveStableAndRespectReducedMotion() throws IOException {
        String premiumCss = read(STATIC.resolve("css/storefront-premium-v2.css"));
        String premiumJs = read(STATIC.resolve("js/storefront-premium-v2.js"));
        String productCard = read(TEMPLATES.resolve("fragments/product-card.html"));

        assertThat(premiumCss)
                .contains("@media (max-width: 1220px)", "@media (max-width: 980px)", "@media (max-width: 720px)",
                        "@media (max-width: 480px)", "@media (max-width: 360px)", "prefers-reduced-motion: reduce",
                        "aspect-ratio", "overflow-x: clip", ".premium-auth-card", ".manual-demo-credentials",
                        ".premium-auth-credit", ".premium-account-hero")
                .doesNotContain("linear-gradient", "radial-gradient");
        assertThat(premiumJs)
                .contains("IntersectionObserver", "data-product-rail", "scrollBy", "prefers-reduced-motion")
                .doesNotContain("Demo123!", "demoEmail", "demoPassword");
        assertThat(productCard)
                .contains("loading=\"lazy\"", "decoding=\"async\"", "width=\"480\"", "height=\"600\"");
    }

    @Test
    void customerAccountStaysInRetailShellWhileStaffKeepsOperationalShell() throws IOException {
        String dashboard = read(TEMPLATES.resolve("dashboard/index.html"));
        String profile = read(TEMPLATES.resolve("profile/edit.html"));
        String orders = read(TEMPLATES.resolve("orders/list.html"));

        assertThat(dashboard)
                .contains("fragments/navigation :: navigation('dashboard')", "account-tabs", "Customer dashboard")
                .contains("fragments/admin-sidebar :: sidebar('dashboard')", "Admin dashboard");
        assertThat(profile).contains("account-tabs", "storefront-premium-v2.css");
        assertThat(orders).contains("account-tabs", "storefront-premium-v2.css");
    }

    private String read(Path path) throws IOException {
        return Files.readString(path);
    }
}
