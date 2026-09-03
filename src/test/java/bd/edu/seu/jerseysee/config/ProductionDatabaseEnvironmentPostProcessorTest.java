package bd.edu.seu.jerseysee.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionDatabaseEnvironmentPostProcessorTest {

    private final ProductionDatabaseEnvironmentPostProcessor postProcessor =
            new ProductionDatabaseEnvironmentPostProcessor();

    @Test
    void rejectsRenderSecretThatContainsTheEnvironmentVariableName() {
        MockEnvironment environment = environment(
                "JERSEYSEE_DB_URL\njdbc:mysql://db.example.com:3306/defaultdb?sslMode=REQUIRED");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JERSEYSEE_DB_URL")
                .hasMessageContaining("only the JDBC URL");
    }

    @Test
    void rejectsNonJdbcMysqlProductionUrl() {
        MockEnvironment environment = environment("db.example.com:3306/defaultdb");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jdbc:mysql://");
    }

    @Test
    void acceptsValidMysqlProductionUrl() {
        MockEnvironment environment = environment(
                "jdbc:mysql://db.example.com:3306/defaultdb?sslMode=REQUIRED&serverTimezone=UTC");

        assertThatCode(() -> postProcessor.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void ignoresNonProductionProfiles() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "demo")
                .withProperty("spring.datasource.url", "jdbc:h2:mem:test");

        assertThatCode(() -> postProcessor.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    private MockEnvironment environment(String datasourceUrl) {
        return new MockEnvironment()
                .withProperty("spring.profiles.active", "production")
                .withProperty("spring.datasource.url", datasourceUrl);
    }
}
