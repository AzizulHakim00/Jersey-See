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
                "th:src=\"@{/images/media/home-hero.webp}\"",
                "/images/media/category-player.webp",
                "/images/media/category-retro.webp",
                "/images/media/category-boots.webp",
                "/images/media/category-new-arrivals.webp");
        assertThat(home).doesNotContain("auth-footballer.svg", "hero-kit.svg", "custom-printing.svg");
    }
}
