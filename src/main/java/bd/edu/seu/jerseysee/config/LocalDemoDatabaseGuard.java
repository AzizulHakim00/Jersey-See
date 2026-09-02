package bd.edu.seu.jerseysee.config;

import java.net.URI;
import java.util.Locale;

final class LocalDemoDatabaseGuard {

    private LocalDemoDatabaseGuard() {
    }

    static void requireLocal(String jdbcUrl) {
        if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:h2:")) {
            return;
        }
        String host = null;
        if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:mysql://")) {
            try {
                host = URI.create(jdbcUrl.substring("jdbc:".length())).getHost();
            } catch (IllegalArgumentException ignored) {
                // The shared error below intentionally avoids echoing the configured URL.
            }
        }
        String normalizedHost = host == null ? "" : host.toLowerCase(Locale.ROOT);
        if (!normalizedHost.equals("localhost") && !normalizedHost.equals("127.0.0.1")
                && !normalizedHost.equals("::1")) {
            throw new IllegalStateException("Demo accounts may only be initialized in a local database.");
        }
    }
}
