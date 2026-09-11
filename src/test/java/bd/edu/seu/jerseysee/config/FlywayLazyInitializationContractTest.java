package bd.edu.seu.jerseysee.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayLazyInitializationContractTest {

    @Test
    void productionKeepsFlywayMigrationInitializerEagerWhileOtherBeansRemainLazy() throws IOException {
        String production = Files.readString(Path.of("src/main/resources/application-production.properties"));
        String configuration = Files.readString(Path.of(
                "src/main/java/bd/edu/seu/jerseysee/config/FlywayLazyInitializationConfiguration.java"));

        assertThat(production).contains(
                "spring.main.lazy-initialization=true",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=none");
        assertThat(configuration).contains(
                "LazyInitializationExcludeFilter",
                "FlywayMigrationInitializer.class",
                "forBeanTypes");
    }
}
