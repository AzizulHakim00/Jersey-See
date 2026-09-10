package bd.edu.seu.jerseysee.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayEagerInitializationConfigTest {

    @Test
    void flywayInitializerIsExcludedFromGlobalLazyInitialization() {
        LazyInitializationExcludeFilter filter =
                FlywayEagerInitializationConfig.flywayMigrationLazyInitializationExcludeFilter();
        RootBeanDefinition definition = new RootBeanDefinition(FlywayMigrationInitializer.class);

        assertThat(filter.isExcluded("flywayInitializer", definition, FlywayMigrationInitializer.class)).isTrue();
    }
}
