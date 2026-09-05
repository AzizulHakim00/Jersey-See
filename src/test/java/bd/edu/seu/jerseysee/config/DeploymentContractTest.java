package bd.edu.seu.jerseysee.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentContractTest {

    @Test
    void renderAndDockerUseDatabaseReadinessAndTheRuntimePort() throws IOException {
        String dockerfile = Files.readString(Path.of("Dockerfile"));
        String render = Files.readString(Path.of("render.yaml"));
        String production = Files.readString(Path.of("src/main/resources/application-production.properties"));

        assertThat(render).contains("healthCheckPath: /actuator/health");
        assertThat(production).contains("server.address=0.0.0.0", "server.port=${PORT:8080}",
                "management.health.db.enabled=true");
        assertThat(dockerfile)
                .contains("clean verify", "${PORT:-8080}/actuator/health")
                .doesNotContain("http://127.0.0.1:8080/actuator/health");
    }

    @Test
    void productionProfileIsPinnedToMysqlAndNeverToH2() throws IOException {
        String production = Files.readString(Path.of("src/main/resources/application-production.properties"));

        assertThat(production)
                .contains(
                        "spring.datasource.url=${JERSEYSEE_DB_URL}",
                        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect")
                .doesNotContain("jdbc:h2:", "org.h2.Driver");
    }

    @Test
    void portfolioProductionEnablesThePublicStorefrontCatalogByDefault() throws IOException {
        String render = Files.readString(Path.of("render.yaml"));
        String production = Files.readString(Path.of("src/main/resources/application-production.properties"));

        assertThat(render).contains(
                "- key: JERSEYSEE_PUBLIC_DEMO_ENABLED\n        value: \"true\"");
        assertThat(production).contains(
                "app.public-demo.enabled=${JERSEYSEE_PUBLIC_DEMO_ENABLED:true}");
    }
}
