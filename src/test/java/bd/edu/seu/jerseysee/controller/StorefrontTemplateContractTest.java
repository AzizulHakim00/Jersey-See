package bd.edu.seu.jerseysee.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorefrontTemplateContractTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path STATIC = Path.of("src/main/resources/static");

    @Test
    void progressiveEnhancementsKeepCoreStorefrontControlsAvailableWithoutJavaScript() throws IOException {
        String detail = read(TEMPLATES.resolve("catalog/detail.html"));
        String catalog = read(TEMPLATES.resolve("catalog/list.html"));
        String navigation = read(TEMPLATES.resolve("fragments/navigation.html"));
        String sidebar = read(TEMPLATES.resolve("fragments/admin-sidebar.html"));
        String javascript = read(STATIC.resolve("js/app.js"));
        String css = read(STATIC.resolve("css/jerseysee.css"));

        assertThat(detail).contains("data-printing-fields").doesNotContain("data-printing-fields hidden");
        assertThat(navigation).contains("data-logout-form", "th:action=\"@{/logout}\"", "method=\"post\"");
        assertThat(navigation).contains("class=\"mobile-nav-logout\"");
        assertThat(sidebar).contains("data-logout-form", "th:action=\"@{/logout}\"", "method=\"post\"");
        assertThat(catalog).contains("data-filter-card", "id=\"catalogFilters\"");
        assertThat(javascript).contains("is-collapsible", "data-logout-form", "hasValidationErrors");
        assertThat(css).contains(".filter-card.is-collapsible", ".sidebar-scrim.is-open",
                "html.js .staff-sidebar", "html.js .staff-mobile-bar", ".mobile-nav-logout");
    }

    @Test
    void renderedActionsAndPaginationRemainRoleAndInputAware() throws IOException {
        String catalog = read(TEMPLATES.resolve("catalog/list.html"));
        String dashboard = read(TEMPLATES.resolve("dashboard/index.html"));
        String order = read(TEMPLATES.resolve("orders/detail.html"));
        String payments = read(TEMPLATES.resolve("staff/payments/list.html"));

        assertThat(catalog).contains("keyword=${filter.keyword}", "categoryId=${filter.categoryId}",
                "productType=${filter.productType}", "clubOrCountry=${filter.clubOrCountry}",
                "edition=${filter.edition}", "kitType=${filter.kitType}", "size=${filter.size}",
                "available=${filter.available}", "minimumPrice=${filter.minimumPrice}",
                "maximumPrice=${filter.maximumPrice}");
        assertThat(dashboard).contains("currentRole.name() == 'MANAGER' or currentRole.name() == 'ADMIN'",
                "th:href=\"@{/staff/products/{id}/edit");
        assertThat(order).contains("currentRole != null and currentRole.name() == 'CUSTOMER'");
        assertThat(payments).contains("#fields.hasErrors('transactionId')", "th:errors=\"*{transactionId}\"");
    }

    @Test
    void publicJourneyUsesThePremiumCommerceStructure() throws IOException {
        String home = read(TEMPLATES.resolve("home/index.html"));
        String navigation = read(TEMPLATES.resolve("fragments/navigation.html"));
        String catalog = read(TEMPLATES.resolve("catalog/list.html"));
        String detail = read(TEMPLATES.resolve("catalog/detail.html"));
        String cart = read(TEMPLATES.resolve("cart/view.html"));
        String checkout = read(TEMPLATES.resolve("orders/checkout.html"));

        assertThat(home).contains("data-campaign-hero", "data-service-strip", "data-commerce-card", "data-product-rail");
        assertThat(navigation).contains("data-storefront-header", "data-mobile-menu", "aria-controls=\"mobileMenu\"");
        assertThat(catalog).contains("data-filter-drawer", "data-commerce-grid", "data-commerce-card");
        assertThat(detail).contains("data-product-stage", "data-purchase-panel", "data-size-selector");
        assertThat(cart).contains("data-cart-lines", "data-order-summary");
        assertThat(checkout).contains("data-checkout-layout", "data-order-review");
    }

    @Test
    void commerceShellRetainsAccessibleProgressiveEnhancementHooks() throws IOException {
        String navigation = read(TEMPLATES.resolve("fragments/navigation.html"));
        String javascript = read(STATIC.resolve("js/app.js"));
        String css = read(STATIC.resolve("css/jerseysee.css"));

        assertThat(navigation).contains("aria-expanded=\"false\"", "aria-label=\"Primary navigation\"");
        assertThat(javascript).contains("data-mobile-menu-toggle", "aria-expanded", "Escape");
        assertThat(css).contains(":focus-visible", "prefers-reduced-motion", ".commerce-product-card");
    }

    @Test
    void brandMarkUsesApprovedPremiumPaletteInsteadOfLegacyLime() throws IOException {
        String brandMark = read(STATIC.resolve("images/brand-mark.svg"));

        assertThat(brandMark)
                .contains("#07142c", "#1646c8", "#b99552", "#f7f3ea")
                .doesNotContain("#c7ff35");
    }

    @Test
    void accountJourneyUsesTheRetailShellWithoutWeakeningOrderActions() throws IOException {
        String login = read(TEMPLATES.resolve("auth/login.html"));
        String registration = read(TEMPLATES.resolve("auth/register.html"));
        String profile = read(TEMPLATES.resolve("profile/edit.html"));
        String orders = read(TEMPLATES.resolve("orders/list.html"));
        String order = read(TEMPLATES.resolve("orders/detail.html"));

        assertThat(login).contains("data-auth-campaign", "data-auth-panel");
        assertThat(registration).contains("data-auth-campaign", "data-auth-panel");
        assertThat(profile).contains("data-account-layout");
        assertThat(orders).contains("data-order-history", "fragments/footer :: footer");
        assertThat(order).contains("data-order-detail", "/invoice", "currentRole != null and currentRole.name() == 'CUSTOMER'");
    }

    @Test
    void premiumV2ReplacesTheRetiredCarouselAndOneClickDemoContract() throws IOException {
        String navigation = read(TEMPLATES.resolve("fragments/navigation.html"));
        String home = read(TEMPLATES.resolve("home/index.html"));
        String login = read(TEMPLATES.resolve("auth/login.html"));
        String footer = read(TEMPLATES.resolve("fragments/footer.html"));
        String premiumJavascript = read(STATIC.resolve("js/storefront-premium-v2.js"));

        assertThat(navigation)
                .contains("data-storefront-header", "data-primary-shop-nav", "BOOTS")
                .doesNotContain("class=\"category-nav\"", "class=\"store-promise\"");
        assertThat(home).contains("FOOTBALL LIVES HERE", "Wear The Passion", "data-product-rail",
                "data-product-rail-next", "data-product-rail-prev", "storefront-premium-v2.css");
        assertThat(login)
                .contains("Demo credentials (for testing only)", "customer@demo.local", "admin@demo.local", "Demo123!")
                .doesNotContain("/demo-login/customer", "/demo-login/admin", "data-demo-entry");
        assertThat(footer).contains("© 2026 Azizul Hakim Omor. All rights reserved.");
        assertThat(premiumJavascript).contains("data-product-rail", "scrollBy", "prefers-reduced-motion");
    }

    @Test
    void renderedAssociationFetchIntentIsExplicit() throws IOException {
        String products = Files.readString(Path.of("src/main/java/bd/edu/seu/jerseysee/repository/ProductRepository.java"));
        String employees = Files.readString(Path.of("src/main/java/bd/edu/seu/jerseysee/repository/EmployeeProfileRepository.java"));
        String employeeService = Files.readString(Path.of("src/main/java/bd/edu/seu/jerseysee/service/EmployeeService.java"));

        assertThat(products).contains("attributePaths = {\"variants\", \"category\"}");
        assertThat(employees).contains("@EntityGraph(attributePaths = \"user\")", "findAllWithUser()");
        assertThat(employeeService).contains("employeeProfileRepository.findAllWithUser()");
    }

    private String read(Path path) throws IOException {
        return Files.readString(path);
    }
}
