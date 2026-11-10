package bd.edu.seu.jerseysee.security;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public class JwtCookieService {
    public static final String NAME = "JERSEYSEE_ACCESS";
    private final boolean secure;
    public JwtCookieService(boolean secure) { this.secure = secure; }
    public void issue(HttpServletResponse response, String token) { write(response, token, JwtService.LIFETIME_SECONDS); }
    public void clear(HttpServletResponse response) { write(response, "", 0); }
    private void write(HttpServletResponse response, String value, long seconds) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(NAME, value).httpOnly(true)
                .secure(secure).sameSite("Lax").path("/").maxAge(Duration.ofSeconds(seconds)).build().toString());
    }
}
