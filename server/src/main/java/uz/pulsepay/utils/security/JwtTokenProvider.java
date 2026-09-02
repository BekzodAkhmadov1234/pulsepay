package uz.pulsepay.utils.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long userExpirySeconds;
    private final long adminExpirySeconds;

    public JwtTokenProvider(
            @Value("${pulsepay.jwt.secret}") String secret,
            @Value("${pulsepay.jwt.user-expiry-seconds}") long userExpirySeconds,
            @Value("${pulsepay.jwt.admin-expiry-seconds}") long adminExpirySeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.userExpirySeconds = userExpirySeconds;
        this.adminExpirySeconds = adminExpirySeconds;
    }

    public String generateUserToken(UUID userId, UUID sessionId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("session_id", sessionId.toString())
                .claim("type", "user")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + userExpirySeconds * 1000))
                .signWith(key)
                .compact();
    }

    public String generateAdminToken(UUID adminId, String role) {
        return Jwts.builder()
                .subject(adminId.toString())
                .claim("role", role)
                .claim("type", "admin")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + adminExpirySeconds * 1000))
                .signWith(key)
                .compact();
    }

    public Claims validateAndParseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
}
