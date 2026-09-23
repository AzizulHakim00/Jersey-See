package bd.edu.seu.jerseysee.config;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keeps Flyway eager even though the rest of the Render production application
 * uses global lazy bean initialization for a faster Free-plan cold start.
 *
 * <p>Schema migrations must complete before the first request can reach a
 * repository that depends on a newly introduced table.</p>
 */
@Configuration(proxyBeanMethods = false)
public class FlywayLazyInitializationConfiguration {

    @Bean
    public static LazyInitializationExcludeFilter eagerFlywayMigrationInitializer() {
        return LazyInitializationExcludeFilter.forBeanTypes(FlywayMigrationInitializer.class);
    }
}
