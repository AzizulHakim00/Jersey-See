package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.exception.InvalidFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KitImageCatalogTest {

    private final KitImageCatalog catalog = new KitImageCatalog(new DefaultResourceLoader());

    @Test
    void exposesAllSuppliedKitImagesWithUniqueIdentifiersAndExistingPreviews() {
        var options = catalog.options();

        assertThat(options).hasSize(22);
        assertThat(new HashSet<>(options.stream().map(KitImageCatalog.KitImageOption::id).toList()))
                .hasSize(22);
        assertThat(options).allSatisfy(option -> {
            assertThat(option.previewUrl()).isEqualTo("/images/kits/" + option.fileName());
            assertThat(Files.isRegularFile(Path.of("src/main/resources/static/images/kits", option.fileName())))
                    .isTrue();
        });
    }

    @Test
    void convertsAWhitelistedSelectionIntoACloudinaryCompatibleUpload() throws Exception {
        var selected = catalog.selectedFile("barcelona-home");

        assertThat(selected.getOriginalFilename()).isEqualTo("barcelona-home.jpg");
        assertThat(selected.getContentType()).isEqualTo("image/jpeg");
        assertThat(selected.getSize()).isGreaterThan(10_000);
        assertThat(selected.getBytes()).startsWith((byte) 0xff, (byte) 0xd8);
    }

    @Test
    void rejectsAnUnknownSelectionInsteadOfReadingAnArbitraryPath() {
        assertThatThrownBy(() -> catalog.selectedFile("../../application.properties"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Select a valid kit image.");
    }

    @Test
    void productEditorContainsTheSelectableGalleryAndPreviewBehaviour() throws Exception {
        String form = Files.readString(Path.of("src/main/resources/templates/staff/products/form.html"));
        String javascript = Files.readString(Path.of("src/main/resources/static/js/app.js"));

        assertThat(form).contains(
                "data-kit-library-source",
                "th:each=\"kit : ${kitImages}\"",
                "name=\"selectedKitImage\"",
                "data-kit-preview=${kit.previewUrl}");
        assertThat(javascript).contains(
                "imagePanel.insertBefore(kitLibrary",
                "preview.src = input.dataset.kitPreview",
                "upload.value = \"\"");
    }
}
