package bd.edu.seu.jerseysee.config;

import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductImage;
import bd.edu.seu.jerseysee.repository.ProductImageRepository;
import bd.edu.seu.jerseysee.repository.ProductRepository;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("production")
@ConditionalOnProperty(name = "app.public-demo.enabled", havingValue = "true")
public class DemoProductImageInitializer {

    private static final Map<String, DemoImage> IMAGES = imageMap();

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final ResourceLoader resourceLoader;

    public DemoProductImageInitializer(ProductRepository productRepository,
            ProductImageRepository imageRepository,
            ResourceLoader resourceLoader) {
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.resourceLoader = resourceLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        seedImages();
    }

    public void seedImages() {
        IMAGES.forEach((seedKey, image) -> productRepository.findByDemoSeedKey(seedKey)
                .ifPresent(product -> seedProductImage(product, image)));
    }

    private void seedProductImage(Product product, DemoImage image) {
        if (product.getStoredImageName() != null && !product.getStoredImageName().isBlank()) {
            return;
        }
        Resource resource = resourceLoader.getResource("classpath:/demo-images/" + image.resourceName());
        try {
            if (!resource.exists()) {
                throw new IllegalStateException("Missing demo product image: " + image.resourceName());
            }
            byte[] bytes = resource.getInputStream().readAllBytes();
            String storedName = "demo-" + image.storedSlug() + image.extension();
            if (imageRepository.findById(storedName).isEmpty()) {
                imageRepository.saveAndFlush(new ProductImage(storedName, bytes));
            }
            product.setStoredImageName(storedName);
            product.setOriginalImageName(image.resourceName());
            product.setImageContentType(image.contentType());
            product.setImageSize((long) bytes.length);
            productRepository.saveAndFlush(product);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load demo product image: " + image.resourceName(), exception);
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

        addSvg(images, "premium-football-boots", "jerseysee-football-boots.svg");
        addSvg(images, "indoor-futsal-shoes", "jerseysee-football-boots.svg");
        addSvg(images, "training-sneakers", "jerseysee-football-boots.svg");
        addSvg(images, "match-football", "jerseysee-match-ball.svg");
        addSvg(images, "training-football", "jerseysee-match-ball.svg");
        addSvg(images, "mini-supporter-ball", "jerseysee-match-ball.svg");
        addSvg(images, "training-top", "jerseysee-training-wear.svg");
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
                new DemoImage(productSlug, resourceName, ".jpg", "image/jpeg"));
    }

    private static void addSvg(Map<String, DemoImage> images, String productSlug, String resourceName) {
        images.put("public.product." + productSlug,
                new DemoImage(productSlug, resourceName, ".svg", "image/svg+xml"));
    }

    private record DemoImage(String storedSlug, String resourceName, String extension, String contentType) { }
}
