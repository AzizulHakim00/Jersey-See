package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.exception.InvalidFileException;
import bd.edu.seu.jerseysee.repository.ProductImageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;

class CloudinaryProductImageStorageTest {
    private final ProductImageRepository legacy = null;

    @Test void requiresCompleteCloudinaryCredentials() {
        assertThatThrownBy(() -> new CloudinaryProductImageStorage("", "key", "secret", "uploads",
                legacy, CloudinaryProductImageStorage.defaultClient())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CloudinaryProductImageStorage("cloud", "", "secret", "uploads",
                legacy, CloudinaryProductImageStorage.defaultClient())).isInstanceOf(IllegalStateException.class);
    }

    @Test void rejectsInvalidImageBeforeCallingCloudinary() {
        var storage = storage();
        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "image", "fake.png", "image/png", new byte[]{1, 2, 3})))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test void buildsOnlyTrustedCloudinaryDeliveryUrls() throws Exception {
        var resource = storage().load("cloud-550e8400-e29b-41d4-a716-446655440000.jpg");
        assertThat(resource.getURL().toString()).isEqualTo(
                "https://res.cloudinary.com/demo-cloud/image/upload/cloud-550e8400-e29b-41d4-a716-446655440000.jpg");
        assertThatThrownBy(() -> storage().load("../secret.jpg")).isInstanceOf(InvalidFileException.class);
    }

    @Test void signatureMatchesCloudinarySha1Contract() {
        assertThat(CloudinaryProductImageStorage.signature("public_id=sample&timestamp=1315060510", "abcd"))
                .isEqualTo("c3470533147774275dd37996cc4d0e68fd03cd4f");
    }

    private CloudinaryProductImageStorage storage() {
        return new CloudinaryProductImageStorage("demo-cloud", "key", "secret", "target/uploads",
                legacy, CloudinaryProductImageStorage.defaultClient());
    }
}
