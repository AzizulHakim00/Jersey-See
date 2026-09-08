package bd.edu.seu.jerseysee.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentContractTest {

    @Test
    void renderAndDockerUseProcessReadinessAndTheRuntimePort() throws IOException {
        String dockerfile = Files.readString(Path.of("Dockerfile"));
        String render = Files.readString(Path.of("render.yaml"));
        String production = Files.readString(Path.of("src/main/resources/application-production.properties"));

        assertThat(render).contains("healthCheckPath: /actuator/health");
        assertThat(production).contains("server.address=0.0.0.0", "server.port=${PORT:8080}",
                "management.health.db.enabled=false");
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
    void productionColdStartDoesNotBlockOnRemoteDatabaseBootstrap() throws IOException {
        String production = Files.readString(Path.of("src/main/resources/application-production.properties"));
        String render = Files.readString(Path.of("render.yaml"));

        assertThat(production).contains(
                "spring.main.lazy-initialization=true",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
                "spring.jpa.properties.jakarta.persistence.database-product-name=MySQL",
                "spring.jpa.properties.jakarta.persistence.database-major-version=8",
                "spring.datasource.hikari.initialization-fail-timeout=-1",
                "spring.datasource.hikari.minimum-idle=0");
        assertThat(render).contains(
                "- key: APP_SEED_ADMIN_ENABLED\n        value: \"false\"");
    }

    @Test
    void productionDoesNotReseedPublicDemoRowsAfterTheDatabaseHasBeenBootstrapped() throws IOException {
        String render = Files.readString(Path.of("render.yaml"));
        String production = Files.readString(Path.of("src/main/resources/application-production.properties"));

        assertThat(render).contains(
                "- key: JERSEYSEE_PUBLIC_DEMO_ENABLED\n        value: \"false\"");
        assertThat(production).contains(
                "app.public-demo.enabled=${JERSEYSEE_PUBLIC_DEMO_ENABLED:false}");
    }

    @Test
    void freeRenderKeepWarmHasStaggeredIndependentSchedulesAndColdStartRetries() throws IOException {
        String primary = Files.readString(Path.of(".github/workflows/keep-render-warm.yml"));
        String backup = Files.readString(Path.of(".github/workflows/keep-render-warm-backup.yml"));

        assertThat(primary).contains(
                "cron: \"0,10,20,30,40,50 * * * *\"",
                "group: keep-render-warm-primary",
                "cancel-in-progress: false",
                "timeout-minutes: 5",
                "deadline=$((SECONDS + 240))",
                "JerseySee-GitHub-KeepWarm/2.0");

        assertThat(backup).contains(
                "cron: \"5,15,25,35,45,55 * * * *\"",
                "group: keep-render-warm-backup",
                "cancel-in-progress: false",
                "timeout-minutes: 5",
                "deadline=$((SECONDS + 240))",
                "JerseySee-GitHub-KeepWarm/2.0");
    }
}
