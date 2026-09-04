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
    void modernStorefrontUsesSingleNavigationProductCarouselAndProfessionalFooter() throws IOException {
        String navigation = read(TEMPLATES.resolve("fragments/navigation.html"));
        String home = read(TEMPLATES.resolve("home/index.html"));
        String login = read(TEMPLATES.resolve("auth/login.html"));
        String footer = read(TEMPLATES.resolve("fragments/footer.html"));
        String javascript = read(STATIC.resolve("js/app.js"));

        assertThat(navigation)
                .contains("data-storefront-header", "data-primary-shop-nav")
                .doesNotContain("class=\"category-nav\"");
        assertThat(home)
                .contains("data-product-carousel", "data-carousel-slide", "data-carousel-next", "data-carousel-prev")
                .doesNotContain("collection-grid");
        assertThat(login).contains("data-demo-account", "data-demo-email", "data-demo-password");
        assertThat(footer).contains("© 2026 JerseySee. Designed &amp; developed by Azizul Hakim Omor.");
        assertThat(javascript).contains("data-product-carousel", "data-demo-account", "prefers-reduced-motion");
    }

    @Test
    void modernStorefrontLoadsDedicatedResponsiveOverridesAndUsesOriginalCrest() throws IOException {
        String navigation = read(TEMPLATES.resolve("fragments/navigation.html"));
        String login = read(TEMPLATES.resolve("auth/login.html"));
        String brand = read(STATIC.resolve("images/brand-mark.svg"));

        assertThat(navigation).contains("storefront-modern.css");
        assertThat(login).contains("storefront-modern.css");
        assertThat(brand)
                .contains("#07142c", "#1646c8", "#b99552", "#f7f3ea")
                .doesNotContain("jersey");
    }

    private String read(Path path) throws IOException {
        return Files.readString(path);
    }
}
