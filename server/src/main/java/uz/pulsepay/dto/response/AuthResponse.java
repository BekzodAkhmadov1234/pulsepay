package uz.pulsepay.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        boolean requiresBiometricStepUp   // true when new device detected (AUTH-02)
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                  long expiresIn, boolean requiresBiometricStepUp) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, requiresBiometricStepUp);
    }
}
