package bd.edu.seu.jerseysee.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private final JwtService tokens = new JwtService("dGVzdC1vbmx5LWplcnNleXNlZS1rZXktMzItYnl0ZXMhISE=", true);
    @Test void requiresStrongProductionKey() {
        assertThatThrownBy(() -> new JwtService("", true)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService("d2Vhaw==", true)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void signedTokenRejectsChangedPasswordDisabledAccountAndWrongKey() {
        var user = User.withUsername("customer@example.com").password("encoded-password").roles("CUSTOMER").build();
        var token = tokens.issue(user);
        var claims = tokens.verify(token);
        assertThat(tokens.matches(claims, user)).isTrue();
        assertThat(tokens.matches(claims, User.withUserDetails(user).password("changed").build())).isFalse();
        assertThat(tokens.matches(claims, User.withUserDetails(user).disabled(true).build())).isFalse();
        assertThatThrownBy(() -> new JwtService("", false).verify(token)).isInstanceOf(io.jsonwebtoken.JwtException.class);
        assertThat(claims.getExpiration().toInstant()).isBefore(java.time.Instant.now().plusSeconds(1801));
    }
}
