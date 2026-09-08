package bd.edu.seu.jerseysee.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@Profile("production")
@EnableScheduling
public class RenderSelfKeepWarm {

    private static final Logger log = LoggerFactory.getLogger(RenderSelfKeepWarm.class);
    private static final URI HEALTH_URI = URI.create("https://jersey-see.onrender.com/actuator/health");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Scheduled(
            initialDelayString = "${app.keep-warm.initial-delay-ms:60000}",
            fixedDelayString = "${app.keep-warm.interval-ms:300000}")
    void pingPublicHealthEndpoint() {
        HttpRequest request = HttpRequest.newBuilder(HEALTH_URI)
                .timeout(Duration.ofSeconds(25))
                .header("User-Agent", "JerseySee-Self-KeepWarm/1.0")
                .GET()
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                log.warn("Render self keep-warm health check returned HTTP {}", response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Render self keep-warm health check was interrupted");
        } catch (Exception exception) {
            log.warn("Render self keep-warm health check failed: {}", exception.getMessage());
        }
    }
}
