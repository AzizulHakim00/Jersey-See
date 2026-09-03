package bd.edu.seu.jerseysee.config;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Validates and normalizes the hosted production database environment variable
 * before JPA/Hikari initialization so Render can use either a JDBC URL or the
 * Aiven MySQL Service URI copied from the Aiven console.
 */
public final class ProductionDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PRODUCTION_PROFILE = "production";
    private static final String RENDER_DB_URL_KEY = "JERSEYSEE_DB_URL";
    private static final String NORMALIZED_DATASOURCE_PROPERTY_SOURCE = "jerseySeeNormalizedProductionDatasource";

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

        String normalizedDatabaseUrl = normalizeRenderDatabaseUrl(renderDatabaseUrl);
        if (!normalizedDatabaseUrl.equals(renderDatabaseUrl.trim())) {
            environment.getPropertySources().addFirst(new MapPropertySource(
                    NORMALIZED_DATASOURCE_PROPERTY_SOURCE,
                    Map.<String, Object>of("spring.datasource.url", normalizedDatabaseUrl)));
        }
    }

    private boolean isProduction(ConfigurableEnvironment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if (PRODUCTION_PROFILE.equals(profile)) {
                return true;
            }
        }
        return PRODUCTION_PROFILE.equals(environment.getProperty("spring.profiles.active"));
    }

    private String normalizeRenderDatabaseUrl(String renderDatabaseUrl) {
        String trimmed = renderDatabaseUrl.trim();

        if (trimmed.contains(RENDER_DB_URL_KEY)) {
            throw new IllegalStateException(
                    "Invalid JERSEYSEE_DB_URL. In Render, the environment-variable key is JERSEYSEE_DB_URL, " +
                            "but its value field must contain only the database URL and must not include the key name.");
        }

        if (containsLineBreak(trimmed)) {
            throw new IllegalStateException(
                    "Invalid JERSEYSEE_DB_URL. The Render value must be a single-line MySQL database URL.");
        }

        if (trimmed.startsWith("jdbc:mysql://")) {
            requireTls(trimmed);
            return trimmed;
        }

        if (trimmed.startsWith("mysql://")) {
            return normalizeAivenServiceUri(trimmed);
        }

        throw new IllegalStateException(
                "Invalid JERSEYSEE_DB_URL. The Render value must begin with jdbc:mysql:// or mysql://.");
    }

    private String normalizeAivenServiceUri(String serviceUri) {
        final URI uri;
        try {
            uri = URI.create(serviceUri);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Invalid JERSEYSEE_DB_URL. The Aiven mysql:// Service URI is malformed.", exception);
        }

        String host = uri.getHost();
        int port = uri.getPort();
        String databasePath = uri.getRawPath();

        if (host == null || host.isBlank() || port < 1 || databasePath == null || databasePath.length() <= 1) {
            throw new IllegalStateException(
                    "Invalid JERSEYSEE_DB_URL. The Aiven mysql:// Service URI must include host, port, and database name.");
        }

        // Deliberately discard any credentials and query parameters embedded in
        // Aiven's Service URI. Render supplies username/password separately, and
        // the normalized JDBC URL always enforces TLS for the hosted connection.
        return "jdbc:mysql://" + host + ":" + port + databasePath
                + "?sslMode=REQUIRED&serverTimezone=UTC";
    }

    private void requireTls(String jdbcUrl) {
        if (!jdbcUrl.toLowerCase(Locale.ROOT).contains("sslmode=required")) {
            throw new IllegalStateException(
                    "Invalid JERSEYSEE_DB_URL. Hosted MySQL connections must include sslMode=REQUIRED.");
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
