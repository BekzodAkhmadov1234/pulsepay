package uz.pulsepay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly-typed, externalized JWT configuration bound from the {@code pulsepay.jwt.*}
 * property namespace via Spring Boot's {@link ConfigurationProperties} mechanism.
 *
 * <p>This class decouples all JWT tuning knobs from both the application code and the
 * {@code @Value} annotation scatter found in older implementations. Every field maps
 * 1-to-1 to a relaxed-binding property key (kebab-case → camelCase).
 *
 * <h3>application.yml equivalent</h3>
 * <pre>{@code
 * pulsepay:
 *   jwt:
 *     # REQUIRED — inject from a secret manager in production; never commit the real value.
 *     # Must be at least 32 UTF-8 bytes (256 bits) for HMAC-SHA256.
 *     secret: ${JWT_SECRET:change-me-in-production-must-be-at-least-256-bits-long-secret-key}
 *
 *     # Short-lived user access token — 15 minutes
 *     user-expiry-seconds: 900
 *
 *     # Long-lived refresh token — 30 days
 *     refresh-expiry-seconds: 2592000
 *
 *     # Admin token — 1 hour
 *     admin-expiry-seconds: 3600
 * }</pre>
 *
 * <p>The equivalent {@code application.properties} keys (already present in this project):
 * <pre>
 * pulsepay.jwt.secret=...
 * pulsepay.jwt.user-expiry-seconds=900
 * pulsepay.jwt.refresh-expiry-seconds=2592000
 * pulsepay.jwt.admin-expiry-seconds=3600
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "pulsepay.jwt")
@Data
public class JwtProperties {

    /**
     * HMAC-SHA signing secret shared across all token types (user and admin).
     * Must be injected from an environment variable or secret manager in production.
     * Minimum length: 32 UTF-8 bytes (256 bits) required by HMAC-SHA256.
     */
    private String secret;

    /**
     * Validity window in seconds for the short-lived user access token.
     * Default: 900 seconds (15 minutes).
     */
    private long userExpirySeconds;

    /**
     * Validity window in seconds for the long-lived refresh token.
     * Default: 2 592 000 seconds (30 days).
     */
    private long refreshExpirySeconds;

    /**
     * Validity window in seconds for admin tokens.
     * Default: 3 600 seconds (1 hour).
     */
    private long adminExpirySeconds;
}
