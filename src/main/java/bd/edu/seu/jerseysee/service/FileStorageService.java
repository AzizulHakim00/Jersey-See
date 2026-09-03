package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.exception.InvalidFileException;
import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile("!production")
public class FileStorageService implements ProductImageStorage {

    static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif");
    private static final Set<String> STORED_EXTENSIONS = CONTENT_TYPES.keySet();

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDirectory) {
        if (uploadDirectory == null || uploadDirectory.isBlank()) {
            throw new IllegalStateException("Image upload directory must be configured.");
        }
        uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
        Path resourceRoot = Path.of("src", "main", "resources").toAbsolutePath().normalize();
        if (uploadRoot.startsWith(resourceRoot)) {
            throw new IllegalStateException("Image uploads must be stored outside application resources.");
        }
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize image storage.", exception);
        }
    }

    public StoredFile store(MultipartFile file) {
        ValidatedUpload upload = validateUpload(file);
        String storedName = UUID.randomUUID() + "." + upload.extension();
        Path target = resolveStoredName(storedName);
        try {
            Files.write(target, upload.content(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new InvalidFileException("Could not store image.", exception);
        }
        return new StoredFile(storedName, upload.originalName(), upload.contentType(), upload.content().length);
    }

    static ValidatedUpload validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Select an image to upload.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidFileException("Image must not exceed 5 MB.");
        }

        String originalName = validateOriginalName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        String expectedContentType = CONTENT_TYPES.get(extension);
        if (expectedContentType == null) {
            throw new InvalidFileException("Only JPG, JPEG, PNG, WEBP, or GIF images are allowed.");
        }
        String declaredContentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT).trim();
        if (!expectedContentType.equals(declaredContentType)) {
            throw new InvalidFileException("Image extension and content type do not match.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new InvalidFileException("Could not read uploaded image.", exception);
        }
        if (content.length == 0) {
            throw new InvalidFileException("Select an image to upload.");
        }
        if (content.length > MAX_IMAGE_SIZE) {
            throw new InvalidFileException("Image must not exceed 5 MB.");
        }
        validateContent(content, extension);

        return new ValidatedUpload(originalName, extension, expectedContentType, content);
    }

    public Resource load(String storedName) {
        Path target = resolveStoredName(storedName);
        try {
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable() || !Files.isRegularFile(target)) {
                throw new ResourceNotFoundException("Product image not found.");
            }
            return resource;
        } catch (java.net.MalformedURLException exception) {
            throw new ResourceNotFoundException("Product image not found.");
        }
    }

    public void delete(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        Path target = resolveStoredName(storedName);
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new InvalidFileException("Could not delete stored image.", exception);
        }
    }

    private static String validateOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new InvalidFileException("Image filename is required.");
        }
        if (originalName.contains("/") || originalName.contains("\\") || originalName.contains("..")
                || originalName.chars().anyMatch(character -> Character.isISOControl(character))) {
            throw new InvalidFileException("Image filename must not contain a path.");
        }
        return originalName;
    }

    private static String extensionOf(String filename) {
        int separator = filename.lastIndexOf('.');
        if (separator < 1 || separator == filename.length() - 1) {
            return "";
        }
        return filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private Path resolveStoredName(String storedName) {
        validateStoredName(storedName);
        Path target = uploadRoot.resolve(storedName).normalize();
        if (!target.getParent().equals(uploadRoot)) {
            throw new InvalidFileException("Invalid stored image name.");
        }
        return target;
    }

    static void validateStoredName(String storedName) {
        if (storedName == null || storedName.isBlank() || storedName.contains("/") || storedName.contains("\\")
                || storedName.contains("..")) {
            throw new InvalidFileException("Invalid stored image name.");
        }
        int extensionSeparator = storedName.lastIndexOf('.');
        if (extensionSeparator < 1
                || !STORED_EXTENSIONS.contains(storedName.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT))) {
            throw new InvalidFileException("Invalid stored image name.");
        }
    }

    private static void validateContent(byte[] content, String extension) {
        if ("webp".equals(extension)) {
            validateWebp(content);
            return;
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw new InvalidFileException("Image content is invalid or undecodable.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidFileException("Image content is invalid or undecodable.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String decodedFormat = reader.getFormatName().toLowerCase(Locale.ROOT);
                String expectedFormat = switch (extension) {
                    case "jpg", "jpeg" -> "jpeg";
                    default -> extension;
                };
                if (!expectedFormat.equals(decodedFormat) && !("jpeg".equals(expectedFormat) && "jpg".equals(decodedFormat))) {
                    throw new InvalidFileException("Image content does not match its extension.");
                }
                if (reader.getWidth(0) <= 0 || reader.getHeight(0) <= 0) {
                    throw new InvalidFileException("Image content is invalid or undecodable.");
                }
                reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (InvalidFileException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new InvalidFileException("Image content is invalid or undecodable.", exception);
        }
    }

    private static void validateWebp(byte[] content) {
        // The JDK has no standard WEBP ImageIO reader. This deliberately performs pragmatic
        // container verification: exact RIFF length, WEBP signature, and a known image chunk.
        if (content.length < 20 || !asciiEquals(content, 0, "RIFF") || !asciiEquals(content, 8, "WEBP")) {
            throw new InvalidFileException("Image content is invalid or undecodable.");
        }
        long riffSize = littleEndianUnsignedInt(content, 4);
        if (riffSize != content.length - 8L) {
            throw new InvalidFileException("Image content is invalid or undecodable.");
        }
        boolean recognizedChunk = asciiEquals(content, 12, "VP8 ")
                || asciiEquals(content, 12, "VP8L")
                || asciiEquals(content, 12, "VP8X");
        long chunkSize = littleEndianUnsignedInt(content, 16);
        long paddedChunkSize = chunkSize + (chunkSize & 1L);
        if (!recognizedChunk || paddedChunkSize > content.length - 20L) {
            throw new InvalidFileException("Image content is invalid or undecodable.");
        }
    }

    private static boolean asciiEquals(byte[] content, int offset, String value) {
        if (offset + value.length() > content.length) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if ((byte) value.charAt(index) != content[offset + index]) {
                return false;
            }
        }
        return true;
    }

    private static long littleEndianUnsignedInt(byte[] content, int offset) {
        return (content[offset] & 0xffL)
                | ((content[offset + 1] & 0xffL) << 8)
                | ((content[offset + 2] & 0xffL) << 16)
                | ((content[offset + 3] & 0xffL) << 24);
    }

    record ValidatedUpload(String originalName, String extension, String contentType, byte[] content) {
    }
}
