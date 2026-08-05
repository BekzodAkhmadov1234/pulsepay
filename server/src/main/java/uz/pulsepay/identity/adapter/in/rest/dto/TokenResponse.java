package uz.pulsepay.identity.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload returned by {@code POST /api/v1/auth/register} and
 * {@code POST /api/v1/auth/login}.
 *
 * <p>Clients must include the {@code accessToken} in the {@code Authorization: Bearer <token>}
 * header on all authenticated requests. The token is opaque to the client — its structure
 * (JWT) is an implementation detail of the server.
 *
 * @param accessToken the signed JWT access token
 * @param tokenType   always {@code "Bearer"} per RFC 6750
 * @param expiresIn   remaining validity in seconds (matches {@code pulsepay.jwt.user-expiry-seconds})
 */
@Schema(description = "JWT access token issued on successful registration or login")
public record TokenResponse(

        @Schema(description = "Signed JWT access token to include in Authorization header",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "Token scheme — always 'Bearer'", example = "Bearer")
        String tokenType,

        @Schema(description = "Seconds until the access token expires", example = "900")
        long expiresIn

) {
    /** Convenience factory that pins {@code tokenType} to {@code "Bearer"}. */
    public static TokenResponse of(String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, "Bearer", expiresIn);
    }
}
