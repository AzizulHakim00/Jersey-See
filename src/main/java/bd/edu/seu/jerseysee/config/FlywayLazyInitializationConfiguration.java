package bd.edu.seu.jerseysee.config;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class FlywayLazyInitializationConfiguration {

    @Bean
    static LazyInitializationExcludeFilter eagerFlywayMigrationInitializer() {
        return LazyInitializationExcludeFilter.forBeanTypes(FlywayMigrationInitializer.class);
    }
}
