package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.exception.InvalidFileException;
import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.repository.ProductImageRepository;
import jakarta.persistence.EntityManager;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class DatabaseProductImageStorageTest {

    @Autowired
    private ProductImageRepository imageRepository;

    @Autowired
    private EntityManager entityManager;

    private DatabaseProductImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new DatabaseProductImageStorage(imageRepository);
    }

    @Test
    void storesValidatedImageAndLoadsItsBytesAfterPersistenceContextIsCleared() throws Exception {
        byte[] png = image("png");

        ProductImageStorage.StoredFile stored = storage.store(
                new MockMultipartFile("image", "match-shirt.png", "image/png", png));
        entityManager.flush();
        entityManager.clear();

        assertThat(stored.storedName()).matches("[0-9a-f-]{36}\\.png");
        assertThat(stored.originalName()).isEqualTo("match-shirt.png");
        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.size()).isEqualTo(png.length);
        assertThat(storage.load(stored.storedName()).getInputStream().readAllBytes()).isEqualTo(png);
    }

    @Test
    void deletesStoredImage() throws Exception {
        ProductImageStorage.StoredFile stored = storage.store(
                new MockMultipartFile("image", "shirt.png", "image/png", image("png")));

        storage.delete(stored.storedName());
        entityManager.flush();
        entityManager.clear();

        assertThat(imageRepository.findById(stored.storedName())).isEmpty();
        assertThatThrownBy(() -> storage.load(stored.storedName()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product image not found.");
    }

    @Test
    void rejectsInvalidImageContentBeforeWritingToDatabase() {
        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("image", "shirt.png", "image/png", "not an image".getBytes())))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Image content is invalid or undecodable.");
        assertThat(imageRepository.count()).isZero();
    }

    @Test
    void rejectsUnsafeStoredNameAndMissingImage() {
        assertThatThrownBy(() -> storage.load("../secret.png"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Invalid stored image name.");
        assertThatThrownBy(() -> storage.load("missing.png"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product image not found.");
    }

    private byte[] image(String format) throws Exception {
        BufferedImage image = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
