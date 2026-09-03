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
}
