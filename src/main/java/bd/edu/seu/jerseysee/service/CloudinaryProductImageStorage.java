package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.repository.ProductImageRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

/** Stores new product images in Cloudinary while retaining read access to legacy image storage. */
@Service
@ConditionalOnProperty(name = "app.images.storage", havingValue = "cloudinary")
public class CloudinaryProductImageStorage implements ProductImageStorage {
    private static final String CLOUD_PREFIX = "cloud-";

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final ProductImageRepository legacyImages;
    private final java.nio.file.Path legacyRoot;
    private final RestClient client;

    @Autowired
    public CloudinaryProductImageStorage(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${app.upload.dir:uploads}") String uploadDir,
            ProductImageRepository legacyImages) {
        this(cloudName, apiKey, apiSecret, uploadDir, legacyImages, defaultClient());
    }

    CloudinaryProductImageStorage(String cloudName, String apiKey, String apiSecret, String uploadDir,
            ProductImageRepository legacyImages, RestClient client) {
        if (cloudName == null || !cloudName.matches("[a-zA-Z0-9_-]+")
                || apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("Cloudinary credentials must be configured when IMAGE_STORAGE=cloudinary.");
        }
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.legacyImages = legacyImages;
        this.legacyRoot = java.nio.file.Path.of(uploadDir).toAbsolutePath().normalize();
        this.client = client;
    }

    @Override
    public StoredFile store(MultipartFile file) {
        FileStorageService.ValidatedUpload upload = FileStorageService.validateUpload(file);
        String publicId = CLOUD_PREFIX + UUID.randomUUID();
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        var parts = new LinkedMultiValueMap<String, Object>();
        parts.add("api_key", apiKey);
        parts.add("timestamp", timestamp);
        parts.add("public_id", publicId);
        parts.add("signature", signature("public_id=" + publicId + "&timestamp=" + timestamp, apiSecret));
        parts.add("file", new ByteArrayResource(upload.content()) {
            @Override public String getFilename() { return "upload." + upload.extension(); }
        });
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> reply = client.post()
                    .uri("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts).retrieve().body(Map.class);
            String expectedPrefix = "https://res.cloudinary.com/" + cloudName + "/image/upload/";
            Object returnedId = reply == null ? null : reply.get("public_id");
            Object secureUrl = reply == null ? null : reply.get("secure_url");
            if (!publicId.equals(returnedId) || !(secureUrl instanceof String url)
                    || !url.startsWith(expectedPrefix)) {
                throw new IllegalStateException("Unexpected Cloudinary upload response.");
            }
            return new StoredFile(publicId + "." + upload.extension(), upload.originalName(),
                    upload.contentType(), upload.content().length);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Cloudinary image upload failed.", exception);
        }
    }

    @Override
    public Resource load(String storedName) {
        FileStorageService.validateStoredName(storedName);
        if (storedName.startsWith(CLOUD_PREFIX)) {
            try {
                return new UrlResource("https://res.cloudinary.com/" + cloudName + "/image/upload/" + storedName);
            } catch (java.net.MalformedURLException exception) {
                throw new ResourceNotFoundException("Product image not found.");
            }
        }
        var legacy = legacyImages.findById(storedName);
        if (legacy.isPresent()) return new ByteArrayResource(legacy.get().getContent());
        var path = legacyRoot.resolve(storedName).normalize();
        if (path.getParent().equals(legacyRoot) && java.nio.file.Files.isRegularFile(path)) {
            return new FileSystemResource(path);
        }
        throw new ResourceNotFoundException("Product image not found.");
    }

    @Override
    public void delete(String storedName) {
        if (storedName == null || storedName.isBlank()) return;
        FileStorageService.validateStoredName(storedName);
        if (!storedName.startsWith(CLOUD_PREFIX)) return;
        String publicId = storedName.substring(0, storedName.lastIndexOf('.'));
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        var parts = new LinkedMultiValueMap<String, Object>();
        parts.add("api_key", apiKey);
        parts.add("timestamp", timestamp);
        parts.add("public_id", publicId);
        parts.add("signature", signature("public_id=" + publicId + "&timestamp=" + timestamp, apiSecret));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> reply = client.post()
                    .uri("https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts).retrieve().body(Map.class);
            Object result = reply == null ? null : reply.get("result");
            if (!"ok".equals(result) && !"not found".equals(result)) {
                throw new IllegalStateException("Cloudinary did not delete the image.");
            }
        } catch (RestClientException exception) {
            throw new IllegalStateException("Cloudinary image deletion failed.", exception);
        }
    }

    static String signature(String parameters, String secret) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                    .digest((parameters + secret).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is unavailable.", exception);
        }
    }

    static RestClient defaultClient() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(20_000);
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
