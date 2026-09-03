package bd.edu.seu.jerseysee.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageStorage {

    StoredFile store(MultipartFile file);

    Resource load(String storedName);

    void delete(String storedName);

    record StoredFile(String storedName, String originalName, String contentType, long size) {
    }
}
