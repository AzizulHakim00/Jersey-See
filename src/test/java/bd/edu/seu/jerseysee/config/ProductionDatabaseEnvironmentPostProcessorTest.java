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
        MockEnvironment environment = productionEnvironmentWithRenderUrl(
                "JERSEYSEE_DB_URL\njdbc:mysql://db.example.com:3306/defaultdb?sslMode=REQUIRED");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JERSEYSEE_DB_URL")
                .hasMessageContaining("only the JDBC URL");
    }

    @Test
    void rejectsNonJdbcMysqlProductionUrl() {
        MockEnvironment environment = productionEnvironmentWithRenderUrl("db.example.com:3306/defaultdb");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jdbc:mysql://");
    }

    @Test
    void rejectsProductionUrlWithoutRequiredSsl() {
        MockEnvironment environment = productionEnvironmentWithRenderUrl(
                "jdbc:mysql://db.example.com:3306/defaultdb?serverTimezone=UTC");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sslMode=REQUIRED");
    }

    @Test
    void acceptsValidMysqlProductionUrl() {
        MockEnvironment environment = productionEnvironmentWithRenderUrl(
                "jdbc:mysql://db.example.com:3306/defaultdb?sslMode=REQUIRED&serverTimezone=UTC");

        assertThatCode(() -> postProcessor.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsProductionProfileTestDatasourceWhenRenderVariableIsAbsent() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "production")
                .withProperty("spring.datasource.url", "jdbc:h2:mem:production-test;MODE=MySQL");

        assertThatCode(() -> postProcessor.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void ignoresNonProductionProfiles() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "demo")
                .withProperty("JERSEYSEE_DB_URL", "not-a-jdbc-url")
                .withProperty("spring.datasource.url", "jdbc:h2:mem:test");

        assertThatCode(() -> postProcessor.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    private MockEnvironment productionEnvironmentWithRenderUrl(String renderUrl) {
        return new MockEnvironment()
                .withProperty("spring.profiles.active", "production")
                .withProperty("JERSEYSEE_DB_URL", renderUrl);
    }
}
