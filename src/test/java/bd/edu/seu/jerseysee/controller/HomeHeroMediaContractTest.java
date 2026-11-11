package bd.edu.seu.jerseysee.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HomeHeroMediaContractTest {

    @Test
    void homepageUsesSuppliedPhotographicMediaInTheHeroAndCategoryCards() throws Exception {
        String home = Files.readString(Path.of("src/main/resources/templates/home/index.html"));

        assertThat(home).contains(
                "class=\"premium-hero-media\"",
                "th:src=\"@{/images/media/home-motion-strike.webp}\"",
                "/images/media/home-red-purple-kit.webp",
                "/images/kits/barcelona-away.jpg",
                "/images/kits/ac-milan-retro.jpg",
                "/images/media/product-boot-white.webp",
                "/images/kits/barcelona-home.jpg",
                "/css/home-campaign.css",
                "data-hero-showcase",
                "<div class=\"premium-hero-jersey\" data-hero-slide");
        assertThat(home).doesNotContain("home-campaign.jpg", "campaign-retro.webp",
                "auth-footballer.svg", "hero-kit.svg", "custom-printing.svg", "data-hero-controls",
                "class=\"premium-hero-product\"");
    }
}
