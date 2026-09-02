package bd.edu.seu.jerseysee.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDemoEnvironmentPostProcessorTest {

    private final LocalDemoEnvironmentPostProcessor postProcessor = new LocalDemoEnvironmentPostProcessor();

    @Test
    void rejectsRemoteMysqlBeforeTheApplicationContextStartsForIntellijProfile() {
        MockEnvironment environment = environment("intellij", true,
                "jdbc:mysql://db.example.com:3306/jerseysee");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("local database");
    }

    @Test
    void acceptsLoopbackMysqlForIntellijProfile() {
        MockEnvironment environment = environment("intellij", true,
                "jdbc:mysql://localhost:3306/jerseysee");

        assertThatCode(() -> postProcessor.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void leavesAnExplicitProductionProfileAloneWhenDemoDataIsDisabled() {
        MockEnvironment environment = environment("production", false,
                "jdbc:mysql://db.example.com:3306/jerseysee");

        assertThatCode(() -> postProcessor.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    private MockEnvironment environment(String profile, boolean demoDataEnabled, String datasourceUrl) {
        return new MockEnvironment()
                .withProperty("spring.profiles.active", profile)
                .withProperty("app.demo-data.enabled", Boolean.toString(demoDataEnabled))
                .withProperty("spring.datasource.url", datasourceUrl);
    }
}
