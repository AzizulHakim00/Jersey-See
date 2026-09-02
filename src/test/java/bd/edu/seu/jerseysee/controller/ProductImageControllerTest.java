package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.config.SecurityConfig;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.service.FileStorageService;
import bd.edu.seu.jerseysee.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductImageController.class)
@Import(SecurityConfig.class)
class ProductImageControllerTest {

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicCanDisplayActiveProductImageInline() throws Exception {
        Product product = imageProduct();
        when(productService.getActiveByStoredImageName("safe-name.png")).thenReturn(product);
        when(fileStorageService.load("safe-name.png")).thenReturn(new ByteArrayResource(new byte[] {1, 2, 3}));

        mockMvc.perform(get("/product-images/safe-name.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotDownloadOriginalProductImage() throws Exception {
        mockMvc.perform(get("/products/15/image/download"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanDownloadImageWithOriginalSafeFilename() throws Exception {
        Product product = imageProduct();
        when(productService.getForManagement(15L)).thenReturn(product);
        when(fileStorageService.load("safe-name.png")).thenReturn(new ByteArrayResource(new byte[] {1, 2, 3}));

        mockMvc.perform(get("/products/15/image/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"shirt.png\""))
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    private Product imageProduct() {
        Product product = new Product();
        product.setStoredImageName("safe-name.png");
        product.setOriginalImageName("shirt.png");
        product.setImageContentType("image/png");
        product.setImageSize(3L);
        product.setActive(true);
        return product;
    }
}
