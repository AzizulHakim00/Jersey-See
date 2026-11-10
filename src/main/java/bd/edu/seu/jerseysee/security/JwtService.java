package bd.edu.seu.jerseysee.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;

/** Roles and account status are always reloaded; password changes revoke old tokens. */
public class JwtService {
    public static final long LIFETIME_SECONDS = 1800;
    private final SecretKey key;

    public JwtService(String secret, boolean production) {
        if (secret == null || secret.isBlank()) {
            if (production) throw new IllegalStateException("Set JWT_SECRET_BASE64 to a random Base64-encoded key of at least 32 bytes.");
            key = Jwts.SIG.HS256.key().build();
        } else {
            byte[] bytes = Base64.getDecoder().decode(secret.trim());
            if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET_BASE64 requires at least 32 random bytes.");
            key = Keys.hmacShaKeyFor(bytes);
        }
    }

    public String issue(UserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder().issuer("jersey-see").subject(user.getUsername())
                .claim("pv", passwordVersion(user)).issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(LIFETIME_SECONDS)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public Claims verify(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer("jersey-see")
                .build().parseSignedClaims(token).getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank() || claims.getExpiration() == null)
            throw new IllegalArgumentException("Incomplete access token");
        return claims;
    }

    public boolean matches(Claims claims, UserDetails user) {
        String version = claims.get("pv", String.class);
        return version != null && user.isEnabled() && user.isAccountNonLocked()
                && user.isAccountNonExpired() && user.isCredentialsNonExpired()
                && MessageDigest.isEqual(version.getBytes(StandardCharsets.UTF_8),
                    passwordVersion(user).getBytes(StandardCharsets.UTF_8));
    }

    private String passwordVersion(UserDetails user) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(
                    (user.getUsername() + "\n" + user.getPassword()).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Cannot sign account version", exception);
        }
    }
}
