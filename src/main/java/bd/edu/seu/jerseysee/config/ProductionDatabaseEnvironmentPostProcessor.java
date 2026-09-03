package bd.edu.seu.jerseysee.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Validates the hosted production database environment variable before JPA/Hikari
 * initialization so Render configuration mistakes fail with a clear message.
 */
public final class ProductionDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PRODUCTION_PROFILE = "production";
    private static final String RENDER_DB_URL_KEY = "JERSEYSEE_DB_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isProduction(environment)) {
            return;
        }

        String renderDatabaseUrl = environment.getProperty(RENDER_DB_URL_KEY);
        if (renderDatabaseUrl == null || renderDatabaseUrl.isBlank()) {
            // Production-profile integration tests may deliberately supply their own
            // datasource. Missing hosted secrets are handled by Spring's required
            // production datasource placeholder during a real deployment.
            return;
        }

        validateRenderDatabaseUrl(renderDatabaseUrl);
    }

    private boolean isProduction(ConfigurableEnvironment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if (PRODUCTION_PROFILE.equals(profile)) {
                return true;
            }
        }
        return PRODUCTION_PROFILE.equals(environment.getProperty("spring.profiles.active"));
    }

    private void validateRenderDatabaseUrl(String renderDatabaseUrl) {
        String trimmed = renderDatabaseUrl.trim();

        if (trimmed.contains(RENDER_DB_URL_KEY)) {
            throw new IllegalStateException(
                    "Invalid JERSEYSEE_DB_URL. In Render, the environment-variable key is JERSEYSEE_DB_URL, " +
                            "but its value field must contain only the JDBC URL and must not include the key name.");
        }

        if (containsLineBreak(trimmed)) {
            throw new IllegalStateException(
                    "Invalid JERSEYSEE_DB_URL. The Render value must be a single-line MySQL JDBC URL.");
        }

        if (!trimmed.startsWith("jdbc:mysql://")) {
            throw new IllegalStateException(
                    "Invalid JERSEYSEE_DB_URL. The Render value must begin with jdbc:mysql://.");
        }
    }

    private boolean containsLineBreak(String value) {
        return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
