package bd.edu.seu.jerseysee.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HomeHeroMediaContractTest {

    @Test
    void homepageUsesThePremiumFootballerArtworkInsteadOfAProductPlaceholder() throws Exception {
        String home = Files.readString(Path.of("src/main/resources/templates/home/index.html"));

        assertThat(home).contains("/images/auth-footballer.svg");
        assertThat(home).doesNotContain("class=\"premium-hero-media\"", "'/images/product-placeholder.svg'");
    }
}
