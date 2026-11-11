package bd.edu.seu.jerseysee.config;

import java.util.concurrent.atomic.AtomicBoolean;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FlywayLazyInitializationTest {

    @Test
    void migrationsRunBeforeAContextWithLazyInitializationIsReady() {
        AtomicBoolean migrated = new AtomicBoolean();
        try (var context = new AnnotationConfigApplicationContext()) {
            context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
            context.register(FlywayLazyInitializationConfiguration.class);
            context.registerBean(FlywayMigrationInitializer.class,
                    () -> new FlywayMigrationInitializer(mock(Flyway.class), ignored -> migrated.set(true)));
            context.registerBean("ordinaryBean", Object.class, Object::new);

            context.refresh();

            assertThat(migrated).isTrue();
            assertThat(context.getBeanFactory().containsSingleton("ordinaryBean")).isFalse();
        }
    }
}
