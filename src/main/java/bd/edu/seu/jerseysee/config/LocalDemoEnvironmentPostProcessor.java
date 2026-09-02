package bd.edu.seu.jerseysee.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Rejects a remote datasource before the application context and JPA start when
 * the well-known local demonstration accounts are enabled.
 */
public final class LocalDemoEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getProperty("app.demo-data.enabled", Boolean.class, false)) {
            LocalDemoDatabaseGuard.requireLocal(environment.getProperty("spring.datasource.url"));
        }
    }

    @Override
    public int getOrder() {
        // Config data and profile-specific properties must be resolved first.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
