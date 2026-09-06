package bd.edu.seu.jerseysee.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RealMediaDashboardPerformanceContractTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path STATIC = Path.of("src/main/resources/static");

    @Test
    void homepageAndLoginUseDedicatedPhotographicMediaInsteadOfSvgFootballers() throws IOException {
        String home = read(TEMPLATES.resolve("home/index.html"));
        String login = read(TEMPLATES.resolve("auth/login.html"));

        assertThat(home)
                .contains("/images/media/home-hero.webp", "/images/media/category-player.webp",
                        "/images/media/category-retro.webp", "/images/media/category-boots.webp",
                        "/images/media/category-new-arrivals.webp", "/images/media/campaign-retro.webp")
                .doesNotContain("auth-footballer.svg", "hero-kit.svg", "custom-printing.svg");
        assertThat(login)
                .contains("/images/media/login-player.webp")
                .doesNotContain("auth-footballer.svg");
    }

    @Test
    void suppliedMediaAssetsExistInTheStaticBundle() {
        assertThat(STATIC.resolve("images/media/home-hero.webp")).exists();
        assertThat(STATIC.resolve("images/media/login-player.webp")).exists();
        assertThat(STATIC.resolve("images/media/category-player.webp")).exists();
        assertThat(STATIC.resolve("images/media/category-retro.webp")).exists();
        assertThat(STATIC.resolve("images/media/category-boots.webp")).exists();
        assertThat(STATIC.resolve("images/media/category-new-arrivals.webp")).exists();
        assertThat(STATIC.resolve("images/media/campaign-retro.webp")).exists();
        assertThat(STATIC.resolve("images/media/product-ball.webp")).exists();
        assertThat(STATIC.resolve("images/media/product-boot-black.webp")).exists();
        assertThat(STATIC.resolve("images/media/product-boot-white.webp")).exists();
        assertThat(STATIC.resolve("images/media/product-training-top.webp")).exists();
    }

    @Test
    void customerAndAdminDashboardUseThePreviewLikeRepairLayer() throws IOException {
        String dashboard = read(TEMPLATES.resolve("dashboard/index.html"));

        assertThat(dashboard)
                .contains("storefront-repair.css", "dashboard-summary-card", "dashboard-orders-card",
                        "customer-dashboard-shell", "admin-dashboard-shell")
                .contains("fragments/navigation :: navigation('dashboard')", "fragments/admin-sidebar :: sidebar('dashboard')");
    }

    @Test
    void premiumPresentationAvoidsObserverDrivenRevealAndSmoothScrollWork() throws IOException {
        String premiumJs = read(STATIC.resolve("js/storefront-premium-v2.js"));
        String repairCss = read(STATIC.resolve("css/storefront-repair.css"));

        assertThat(premiumJs)
                .contains("data-product-rail", "scrollBy", "behavior: \"auto\"", "prefers-reduced-motion")
                .doesNotContain("IntersectionObserver", "ResizeObserver", "behavior: \"smooth\"");
        assertThat(repairCss)
                .contains("scroll-behavior: auto !important", ".js [data-premium-reveal]",
                        "opacity: 1 !important", "transition: none !important");
    }

    private String read(Path path) throws IOException {
        return Files.readString(path);
    }
}
