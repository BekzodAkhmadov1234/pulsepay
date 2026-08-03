package uz.pulsepay.identity.domain.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OTP discipline configuration (REG-03/AUTH-03).
 *
 * Defaults from the business rules specification:
 *   validitySeconds = 59   (spec: "59-second expiry")
 *   maxAttempts     = 3    (spec: "3-attempt cap")
 *   lockoutMinutes  = 15   (spec: "15-minute lockout")
 *
 * All values are configurable via application properties (pulsepay.otp.*) so they
 * can be adjusted if CBU tightens the rules without a code deploy.
 */
@ConfigurationProperties(prefix = "pulsepay.otp")
public record OtpProperties(
        int validitySeconds,
        int maxAttempts,
        int lockoutMinutes
) {
    public OtpProperties() {
        this(59, 3, 15);
    }
}
