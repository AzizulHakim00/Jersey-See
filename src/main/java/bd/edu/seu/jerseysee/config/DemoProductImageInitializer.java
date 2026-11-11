package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductImage;
import bd.edu.seu.jerseysee.repository.ProductImageRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"demo", "production"})
@ConditionalOnExpression("${app.public-demo.enabled:false} || ${app.catalog.supplied-kits.enabled:false}")
public class DemoProductImageInitializer {

    private static final String FALLBACK_IMAGE = "jerseysee-training-wear.svg";
    private static final String KIT_IMAGE_DIRECTORY = "classpath:/static/images/kits/";
    private static final Map<String, DemoImage> IMAGES = imageMap();

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final ResourceLoader resourceLoader;
    private final boolean publicDemoEnabled;

    public DemoProductImageInitializer(ProductRepository productRepository,
            ProductImageRepository imageRepository,
            ResourceLoader resourceLoader,
            @Value("${app.public-demo.enabled:false}") boolean publicDemoEnabled) {
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.resourceLoader = resourceLoader;
        this.publicDemoEnabled = publicDemoEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(20)
    @CacheEvict(value = "featuredCards", allEntries = true)
    @Transactional
    public void onApplicationReady() {
        seedImages();
    }

    public void seedImages() {
        IMAGES.forEach((seedKey, image) -> {
            if (publicDemoEnabled || KIT_IMAGE_DIRECTORY.equals(image.resourceDirectory())) {
                productRepository.findByDemoSeedKey(seedKey).ifPresent(product -> seedProductImage(product, image));
            }
        });
    }

    private void seedProductImage(Product product, DemoImage image) {
        String expectedStoredName = "demo-" + image.storedSlug() + image.extension();
        String currentStoredName = product.getStoredImageName();
        if (!publicDemoEnabled && currentStoredName != null && !currentStoredName.startsWith("demo-")) {
            return;
        }
        if (expectedStoredName.equals(currentStoredName)
                && imageRepository.findById(currentStoredName).isPresent()) {
            return;
        }

        try {
            LoadedImage loaded = loadImage(image);
            String storedName = "demo-" + image.storedSlug() + loaded.extension();
            if (imageRepository.findById(storedName).isEmpty()) {
                imageRepository.saveAndFlush(new ProductImage(storedName, loaded.bytes()));
            }
            product.setStoredImageName(storedName);
            product.setOriginalImageName(loaded.originalName());
            product.setImageContentType(loaded.contentType());
            product.setImageSize((long) loaded.bytes().length);
            productRepository.saveAndFlush(product);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load demo product image: " + image.resourceName(), exception);
        }
    }

    private LoadedImage loadImage(DemoImage image) throws IOException {
        Resource direct = resourceLoader.getResource(image.resourceDirectory() + image.resourceName());
        if (direct.exists()) {
            try (InputStream input = direct.getInputStream()) {
                return new LoadedImage(input.readAllBytes(), image.resourceName(), image.extension(), image.contentType());
            }
        }

        Resource fallback = resourceLoader.getResource("classpath:/demo-images/" + FALLBACK_IMAGE);
        if (!fallback.exists()) {
            throw new IllegalStateException("Missing demo product image and fallback: " + image.resourceName());
        }
        try (InputStream input = fallback.getInputStream()) {
            return new LoadedImage(input.readAllBytes(), FALLBACK_IMAGE, ".svg", "image/svg+xml");
        }
    }

    private static Map<String, DemoImage> imageMap() {
        Map<String, DemoImage> images = new LinkedHashMap<>();
        addJpeg(images, "barcelona-home-fan", "barcelona-home.jpg");
        addJpeg(images, "barcelona-away-player", "barcelona-away.jpg");
        addJpeg(images, "barcelona-third-player", "barcelona-third.jpg");
        addJpeg(images, "real-madrid-home-fan", "real-madrid-home.jpg");
        addJpeg(images, "real-madrid-away-player", "real-madrid-away.jpg");
        addJpeg(images, "real-madrid-third-player", "real-madrid-third.jpg");
        addJpeg(images, "real-madrid-retro", "real-madrid-retro.jpg");
        addJpeg(images, "arsenal-home-fan", "arsenal-home.jpg");
        addJpeg(images, "arsenal-away-player", "arsenal-away.jpg");
        addJpeg(images, "arsenal-third-player", "arsenal-third.jpg");
        addJpeg(images, "chelsea-home-fan", "chelsea-home.jpg");
        addJpeg(images, "chelsea-away-player", "chelsea-away.jpg");
        addJpeg(images, "chelsea-third-player", "chelsea-third.jpg");
        addJpeg(images, "liverpool-home-fan", "liverpool-home.jpg");
        addJpeg(images, "liverpool-away-player", "liverpool-away.jpg");
        addJpeg(images, "manchester-city-home-fan", "manchester-city-home.jpg");
        addJpeg(images, "manchester-city-away-player", "manchester-city-away.jpg");
        addJpeg(images, "manchester-city-third-player", "manchester-city-third.jpg");
        addJpeg(images, "manchester-united-away-player", "manchester-united-away.jpg");
        addJpeg(images, "manchester-united-third-player", "manchester-united-third.jpg");
        addJpeg(images, "ac-milan-retro", "ac-milan-retro.jpg");
        addJpeg(images, "juventus-94-95-retro", "juventus-94-95-retro.jpg");

        addWebp(images, "premium-football-boots", "product-boot-white.webp");
        addWebp(images, "indoor-futsal-shoes", "product-boot-black.webp");
        addWebp(images, "training-sneakers", "product-training-sneakers.webp");
        addWebp(images, "match-football", "product-match-ball.webp");
        addWebp(images, "training-football", "product-training-ball.webp");
        addWebp(images, "mini-supporter-ball", "product-ball.webp");
        addWebp(images, "training-top", "product-training-top.webp");

        addSvg(images, "training-trousers", "jerseysee-training-wear.svg");
        addSvg(images, "coach-jacket", "jerseysee-training-wear.svg");
        addSvg(images, "supporter-cap", "jerseysee-accessory.svg");
        addSvg(images, "club-wristband", "jerseysee-accessory.svg");
        addSvg(images, "gym-sack", "jerseysee-accessory.svg");
        addSvg(images, "shin-guard", "jerseysee-accessory.svg");
        return Map.copyOf(images);
    }

    private static void addJpeg(Map<String, DemoImage> images, String productSlug, String resourceName) {
        images.put("public.product." + productSlug,
                new DemoImage(productSlug + "-supplied", KIT_IMAGE_DIRECTORY, resourceName, ".jpg", "image/jpeg"));
    }

    private static void addWebp(Map<String, DemoImage> images, String productSlug, String resourceName) {
        images.put("public.product." + productSlug,
                new DemoImage(productSlug + "-supplied", "classpath:/static/images/media/", resourceName, ".webp", "image/webp"));
    }

    private static void addSvg(Map<String, DemoImage> images, String productSlug, String resourceName) {
        images.put("public.product." + productSlug,
                new DemoImage(productSlug, "classpath:/demo-images/", resourceName, ".svg", "image/svg+xml"));
    }

    private record DemoImage(String storedSlug, String resourceDirectory, String resourceName,
                             String extension, String contentType) { }

    private record LoadedImage(byte[] bytes, String originalName, String extension, String contentType) { }
}
