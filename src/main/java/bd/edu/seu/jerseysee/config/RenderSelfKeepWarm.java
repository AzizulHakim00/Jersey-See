package bd.edu.seu.jerseysee.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final URI KEEP_WARM_URI = URI.create("https://jersey-see.onrender.com/login");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final AtomicBoolean firstSuccessLogged = new AtomicBoolean();

    @Scheduled(
            initialDelayString = "${app.keep-warm.initial-delay-ms:60000}",
            fixedDelayString = "${app.keep-warm.interval-ms:300000}")
    void pingPublicEndpoint() {
        HttpRequest request = HttpRequest.newBuilder(KEEP_WARM_URI)
                .timeout(Duration.ofSeconds(25))
                .header("User-Agent", "JerseySee-Self-KeepWarm/1.1")
                .GET()
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status >= 200 && status < 400) {
                if (firstSuccessLogged.compareAndSet(false, true)) {
                    log.info("Self keep-warm established with HTTP {}", status);
                }
                return;
            }
            log.warn("Render self keep-warm request returned HTTP {}", status);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Render self keep-warm request was interrupted");
        } catch (Exception exception) {
            log.warn("Render self keep-warm request failed: {}", exception.getMessage());
        }
    }
}
