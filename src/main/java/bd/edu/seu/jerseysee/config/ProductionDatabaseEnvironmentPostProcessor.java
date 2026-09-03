package bd.edu.seu.jerseysee.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Validates production datasource configuration before JPA/Hikari initialization
 * so deployment mistakes fail with a clear, actionable message.
 */
public final class ProductionDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PRODUCTION_PROFILE = "production";
    private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";
    private static final String RENDER_DB_URL_KEY = "JERSEYSEE_DB_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isProduction(environment)) {
            return;
        }

        String datasourceUrl = environment.getProperty(DATASOURCE_URL_PROPERTY);
        validateDatasourceUrl(datasourceUrl);
    }

    private boolean isProduction(ConfigurableEnvironment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if (PRODUCTION_PROFILE.equals(profile)) {
                return true;
            }
        }
        return PRODUCTION_PROFILE.equals(environment.getProperty("spring.profiles.active"));
    }

    private void validateDatasourceUrl(String datasourceUrl) {
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            throw new IllegalStateException(
                    "Production requires JERSEYSEE_DB_URL. In Render, set the value field to only the JDBC URL, " +
                            "for example jdbc:mysql://host:port/defaultdb?sslMode=REQUIRED&serverTimezone=UTC.");
        }

        String trimmed = datasourceUrl.trim();
        if (trimmed.contains(RENDER_DB_URL_KEY)) {
            throw new IllegalStateException(
                    "Invalid JERSEYSEE_DB_URL. In Render, the environment-variable key is JERSEYSEE_DB_URL, " +
                            "but its value field must contain only the JDBC URL and must not include the key name.");
        }

        if (!trimmed.startsWith("jdbc:mysql://")) {
            throw new IllegalStateException(
                    "Invalid JERSEYSEE_DB_URL. Production requires a MySQL JDBC URL beginning with jdbc:mysql://.");
        }

        if (containsLineBreak(trimmed)) {
            throw new IllegalStateException(
                    "Invalid JERSEYSEE_DB_URL. The Render value must be a single-line MySQL JDBC URL.");
        }
    }

    private boolean containsLineBreak(String value) {
        return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    }

    @Override
    public int getOrder() {
        // Profile-specific config must already be resolved, but this still runs before the application context.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
