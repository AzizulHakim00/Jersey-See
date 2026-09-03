package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.service.ProductImageStorage;
import bd.edu.seu.jerseysee.service.ProductService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductImageController {

    private final ProductService productService;
    private final ProductImageStorage imageStorage;

    public ProductImageController(ProductService productService, ProductImageStorage imageStorage) {
        this.productService = productService;
        this.imageStorage = imageStorage;
    }

    @GetMapping("/product-images/{storedName:.+}")
    public ResponseEntity<Resource> display(@PathVariable String storedName) {
        Product product = productService.getActiveByStoredImageName(storedName);
        return imageResponse(product, ContentDisposition.inline().build());
    }

    @GetMapping("/products/{id}/image/download")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Product product = productService.getForManagement(id);
        if (product.getOriginalImageName() == null) {
            throw new ResourceNotFoundException("Product image not found.");
        }
        ContentDisposition attachment = ContentDisposition.attachment()
                .filename(product.getOriginalImageName())
                .build();
        return imageResponse(product, attachment);
    }

    private ResponseEntity<Resource> imageResponse(Product product, ContentDisposition disposition) {
        if (product.getStoredImageName() == null || product.getOriginalImageName() == null
                || product.getImageContentType() == null || product.getImageSize() == null) {
            throw new ResourceNotFoundException("Product image not found.");
        }
        Resource resource = imageStorage.load(product.getStoredImageName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(product.getImageContentType()))
                .contentLength(product.getImageSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
