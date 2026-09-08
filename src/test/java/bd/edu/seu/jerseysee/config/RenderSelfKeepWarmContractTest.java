package bd.edu.seu.jerseysee.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RenderSelfKeepWarmContractTest {

    private static final Path SOURCE = Path.of(
            "src/main/java/bd/edu/seu/jerseysee/config/RenderSelfKeepWarm.java");

    @Test
    void productionServicePingsCheapPublicEndpointEveryFiveMinutes() throws IOException {
        assertTrue(Files.exists(SOURCE), "Render self keep-warm component must exist");

        String source = Files.readString(SOURCE);
        assertTrue(source.contains("@Profile(\"production\")"));
        assertTrue(source.contains("@Scheduled"));
        assertTrue(source.contains("300000"), "keep-warm interval must be five minutes");
        assertTrue(source.contains("https://jersey-see.onrender.com/login"));
        assertTrue(source.contains("Self keep-warm established with HTTP"));
    }
}
