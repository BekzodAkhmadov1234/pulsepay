package uz.pulsepay.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.pulsepay.config.JwtProperties;
import uz.pulsepay.identity.domain.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Central JWT management service for the PulsePay platform.
 *
 * <p><strong>Domain isolation guarantee:</strong> this service is the <em>only</em> place in the
 * security pipeline that touches the domain {@link User} record. It reads the record's fields
 * at token-generation time and embeds them as opaque string claims. Everything downstream
 * (the filter, controllers) works exclusively with those extracted claim values — the domain
 * model never implements or extends any Spring Security interface.
 *
 * <h3>Token payload schema</h3>
 * <pre>
 * User access token
 * ─────────────────
 *   sub        → user UUID  (standard JWT "subject")
 *   type       → "user"     (discriminator claim)
 *   phone_e164 → E.164 phone number (e.g. "+998901234567")
 *   kyc_level  → KYC tier   (e.g. "basic", "verified")
 *   iat / exp  → issued-at / expiry (epoch seconds)
 *
 * Admin access token
 * ──────────────────
 *   sub   → admin UUID
 *   type  → "admin"
 *   role  → admin role string (e.g. "ops", "super")
 *   iat / exp
 * </pre>
 */
@Slf4j
@Service
public class JwtService {

    // ── Claim key constants ───────────────────────────────────────────────────────

    /** Discriminator claim: either {@value #TYPE_USER} or {@value #TYPE_ADMIN}. */
    public static final String CLAIM_TYPE      = "type";

    /** E.164-formatted mobile number embedded in user tokens. */
    public static final String CLAIM_PHONE     = "phone_e164";

    /** KYC tier string embedded in user tokens; drives ROLE_ authority mapping. */
    public static final String CLAIM_KYC_LEVEL = "kyc_level";

    /** Full name embedded in user tokens. */
    public static final String CLAIM_FULL_NAME = "full_name";

    /** Admin role string embedded in admin tokens. */
    public static final String CLAIM_ROLE      = "role";

    public static final String TYPE_USER  = "user";
    public static final String TYPE_ADMIN = "admin";

    // ── State ────────────────────────────────────────────────────────────────────

    private final SecretKey    signingKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        // Keys.hmacShaKeyFor validates the minimum key length and selects the correct
        // HMAC-SHA algorithm (SHA-256, -384, or -512) based on key size automatically.
        this.signingKey = Keys.hmacShaKeyFor(
                properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // ── Token generation ─────────────────────────────────────────────────────────

    /**
     * Generates a short-lived user access token enriched with identity claims.
     *
     * <p>The {@code phone_e164} and {@code kyc_level} claims allow the
     * {@link JwtAuthenticationFilter} to reconstruct the Spring Security context
     * on every request without an additional database round-trip.
     *
     * @param user the authenticated domain user (must not be null, must be active)
     * @return a signed, compact JWT string
     */
    public String generateUserToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + properties.getUserExpirySeconds() * 1_000L);

        return Jwts.builder()
                .subject(user.id().toString())
                .claim(CLAIM_TYPE,      TYPE_USER)
                .claim(CLAIM_PHONE,     user.phoneE164())
                .claim(CLAIM_KYC_LEVEL, user.kycLevel())
                .claim(CLAIM_FULL_NAME, user.fullName())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a short-lived admin access token.
     *
     * @param adminId the admin entity's UUID
     * @param role    the admin's role string (stored as-is in the {@code role} claim)
     * @return a signed, compact JWT string
     */
    public String generateAdminToken(UUID adminId, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + properties.getAdminExpirySeconds() * 1_000L);

        return Jwts.builder()
                .subject(adminId.toString())
                .claim(CLAIM_TYPE, TYPE_ADMIN)
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // ── Claim extraction ─────────────────────────────────────────────────────────

    /**
     * Parses and cryptographically verifies the token, returning the full claims payload.
     *
     * <p>This is the single authoritative validation point. Callers that need individual
     * claims should prefer the typed accessor methods below rather than working with the
     * raw {@link Claims} map directly.
     *
     * @param token compact JWT string (must not be null)
     * @return the verified {@link Claims} payload
     * @throws IllegalArgumentException if the token is malformed, has an invalid signature,
     *                                  or has expired
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException ex) {
            log.debug("JWT validation rejected: {}", ex.getMessage());
            throw new IllegalArgumentException("Invalid or expired JWT token", ex);
        }
    }

    /**
     * Extracts and parses the user UUID from the {@code sub} claim.
     *
     * @param token compact JWT string
     * @return the user's UUID
     * @throws IllegalArgumentException on invalid token or malformed UUID
     */
    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).getSubject());
    }

    /**
     * Extracts the E.164 phone number from the {@code phone_e164} claim.
     *
     * @param token compact JWT string
     * @return the phone number, or {@link Optional#empty()} if the claim is absent
     *         (e.g. in admin tokens)
     */
    public Optional<String> extractPhoneE164(String token) {
        return Optional.ofNullable(
                extractAllClaims(token).get(CLAIM_PHONE, String.class));
    }

    /**
     * Extracts the KYC level from the {@code kyc_level} claim.
     *
     * @param token compact JWT string
     * @return the KYC level string, or {@link Optional#empty()} if absent
     */
    public Optional<String> extractKycLevel(String token) {
        return Optional.ofNullable(
                extractAllClaims(token).get(CLAIM_KYC_LEVEL, String.class));
    }

    /**
     * Extracts the token type discriminator ({@code "user"} or {@code "admin"}).
     *
     * @param token compact JWT string
     * @return the type claim value, or {@link Optional#empty()} if absent
     */
    public Optional<String> extractType(String token) {
        return Optional.ofNullable(
                extractAllClaims(token).get(CLAIM_TYPE, String.class));
    }

    // ── Validation helpers ────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the token has a valid signature and has not expired.
     *
     * <p>This method intentionally swallows exceptions and returns a boolean, making it
     * suitable for filter guard clauses. Use {@link #extractAllClaims(String)} directly
     * when you need structured error information.
     *
     * @param token compact JWT string (may be null or blank)
     * @return {@code true} when the token is structurally valid and unexpired
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            extractAllClaims(token);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Returns {@code true} when the token is valid AND its {@code type} claim
     * equals {@value #TYPE_USER}.
     *
     * @param token compact JWT string
     * @return {@code true} for valid, non-expired user-scoped tokens
     */
    public boolean isUserToken(String token) {
        return isTokenValid(token)
                && TYPE_USER.equals(extractAllClaims(token).get(CLAIM_TYPE, String.class));
    }
}
