package uz.pulsepay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterConfirmRequest(
        @NotBlank
        @Pattern(regexp = "\\+\\d{7,15}", message = "Phone must be in E.164 format")
        String phoneE164,

        @NotBlank @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
        String code,

        @NotBlank @Size(min = 2, max = 100)
        String fullName,

        /** Browser fingerprint or mobile device ID. Defaults to a generated value if blank. */
        String deviceFingerprint,

        /**
         * Client platform: "ios", "android", or "web".
         * Defaults to "web" if not provided.
         */
        String platform
) {
    public String resolvedFingerprint() {
        return (deviceFingerprint != null && !deviceFingerprint.isBlank())
                ? deviceFingerprint
                : "web-" + java.util.UUID.randomUUID();
    }

    public String resolvedPlatform() {
        return (platform != null && !platform.isBlank()) ? platform : "web";
    }
}
