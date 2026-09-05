package bd.edu.seu.jerseysee.config;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;

class PublicDemoInitializationOrderTest {

    @Test
    void catalogSeedsBeforePersistentImagesAreAttached() throws Exception {
        Method catalogListener = PublicDemoCatalogInitializer.class.getMethod("initialize");
        Method imageListener = DemoProductImageInitializer.class.getMethod("onApplicationReady");

        Order catalogOrder = catalogListener.getAnnotation(Order.class);
        Order imageOrder = imageListener.getAnnotation(Order.class);

        assertThat(catalogOrder)
                .as("catalog ApplicationReady listener must have an explicit order")
                .isNotNull();
        assertThat(imageOrder)
                .as("image ApplicationReady listener must have an explicit order")
                .isNotNull();
        assertThat(catalogOrder.value())
                .as("catalog products must exist before image seeding searches for them")
                .isLessThan(imageOrder.value());
    }
}
