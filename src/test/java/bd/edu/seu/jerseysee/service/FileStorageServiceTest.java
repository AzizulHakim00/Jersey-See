package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.exception.InvalidFileException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path uploadDirectory;

    @Test
    void storesDecodedImageUnderGeneratedNameAndRetainsSafeMetadata() throws Exception {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());
        byte[] png = image("png");

        ProductImageStorage.StoredFile stored = storage.store(
                new MockMultipartFile("image", "shirt.png", "image/png", png));

        assertThat(stored.storedName()).matches("[0-9a-f-]{36}\\.png");
        assertThat(stored.originalName()).isEqualTo("shirt.png");
        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.size()).isEqualTo(png.length);
        assertThat(storage.load(stored.storedName()).getInputStream().readAllBytes()).isEqualTo(png);
        assertThat(Files.exists(uploadDirectory.resolve("shirt.png"))).isFalse();
    }

    @Test
    void rejectsImageLargerThanFiveMegabytes() {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("image", "large.png", "image/png", oversized)))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Image must not exceed 5 MB.");
    }

    @Test
    void acceptsImageExactlyFiveMegabytes() throws Exception {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());
        byte[] exactLimit = Arrays.copyOf(image("png"), 5 * 1024 * 1024);

        ProductImageStorage.StoredFile stored = storage.store(
                new MockMultipartFile("image", "large.png", "image/png", exactLimit));

        assertThat(stored.size()).isEqualTo(5L * 1024 * 1024);
    }

    @Test
    void rejectsUnsupportedExtensionAndMimeType() {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());

        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("image", "notes.txt", "text/plain", "not an image".getBytes())))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Only JPG, JPEG, PNG, WEBP, or GIF images are allowed.");
    }

    @Test
    void rejectsPathLikeOriginalName() throws Exception {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());

        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("image", "../shirt.png", "image/png", image("png"))))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Image filename must not contain a path.");
    }

    @Test
    void rejectsExtensionAndDeclaredMimeMismatch() throws Exception {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());

        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("image", "shirt.png", "image/jpeg", image("png"))))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Image extension and content type do not match.");
    }

    @Test
    void rejectsDecodableImageWhoseSignatureDoesNotMatchExtension() throws Exception {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());

        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("image", "shirt.jpg", "image/jpeg", image("png"))))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Image content does not match its extension.");
    }

    @Test
    void rejectsUndecodableImagePayload() {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());

        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("image", "shirt.png", "image/png", "fake png".getBytes())))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Image content is invalid or undecodable.");
    }

    @Test
    void acceptsStructurallyValidWebpContainerWithoutOptionalDecoder() {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());
        byte[] webp = minimalWebp();

        ProductImageStorage.StoredFile stored = storage.store(
                new MockMultipartFile("image", "shirt.webp", "image/webp", webp));

        assertThat(stored.storedName()).endsWith(".webp");
        assertThat(stored.size()).isEqualTo(webp.length);
    }

    @Test
    void rejectsWebpWithBigEndianRiffLength() {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());
        byte[] webp = minimalWebp();
        webp[4] = 0;
        webp[7] = 12;

        assertInvalidWebp(storage, webp);
    }

    @Test
    void rejectsWebpWhoseRiffLengthDoesNotMatchPayload() {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());
        byte[] webp = minimalWebp();
        webp[4] = 11;

        assertInvalidWebp(storage, webp);
    }

    @Test
    void rejectsWebpWithoutRecognizedImageChunk() {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());
        byte[] webp = minimalWebp();
        webp[12] = 'J';
        webp[13] = 'U';
        webp[14] = 'N';
        webp[15] = 'K';

        assertInvalidWebp(storage, webp);
    }

    @Test
    void rejectsOddWebpChunkWithoutRequiredPaddingByte() {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());
        byte[] webp = Arrays.copyOf(minimalWebp(), 21);
        webp[4] = 13;
        webp[16] = 1;

        assertInvalidWebp(storage, webp);
    }

    @Test
    void preventsTraversalWhenLoadingOrDeleting() {
        FileStorageService storage = new FileStorageService(uploadDirectory.toString());

        assertThatThrownBy(() -> storage.load("../secret.png"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Invalid stored image name.");
        assertThatThrownBy(() -> storage.delete("subdir/secret.png"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Invalid stored image name.");
    }

    private byte[] image(String format) throws Exception {
        BufferedImage image = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private byte[] minimalWebp() {
        return new byte[] {
                'R', 'I', 'F', 'F', 12, 0, 0, 0, 'W', 'E', 'B', 'P',
                'V', 'P', '8', 'X', 0, 0, 0, 0
        };
    }

    private void assertInvalidWebp(FileStorageService storage, byte[] webp) {
        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("image", "shirt.webp", "image/webp", webp)))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Image content is invalid or undecodable.");
    }
}
