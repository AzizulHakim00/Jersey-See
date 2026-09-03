package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.model.ProductImage;
import bd.edu.seu.jerseysee.repository.ProductImageRepository;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile("production")
public class DatabaseProductImageStorage implements ProductImageStorage {

    private final ProductImageRepository imageRepository;

    public DatabaseProductImageStorage(ProductImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    @Transactional
    public StoredFile store(MultipartFile file) {
        FileStorageService.ValidatedUpload upload = FileStorageService.validateUpload(file);
        String storedName = UUID.randomUUID() + "." + upload.extension();
        imageRepository.saveAndFlush(new ProductImage(storedName, upload.content()));
        return new StoredFile(storedName, upload.originalName(), upload.contentType(),
                upload.content().length);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource load(String storedName) {
        FileStorageService.validateStoredName(storedName);
        ProductImage image = imageRepository.findById(storedName)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found."));
        return new ByteArrayResource(image.getContent());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        FileStorageService.validateStoredName(storedName);
        imageRepository.findById(storedName).ifPresent(imageRepository::delete);
        imageRepository.flush();
    }
}
