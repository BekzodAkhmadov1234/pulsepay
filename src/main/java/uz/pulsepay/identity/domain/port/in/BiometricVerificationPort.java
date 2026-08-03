package uz.pulsepay.identity.domain.port.in;

import java.util.UUID;

/**
 * Pluggable biometric liveness verification (REG-02/AUTH-02).
 *
 * The actual provider (vendor SDK, government e-ID service, or CBU-mandated system) is
 * an external contract — do not hardcode a specific vendor API.
 *
 * // TODO(external-contract): Uzbekistan CBU 2026 draft requires remote biometric identification
 * // at app registration. Provider, SLA, and integration details to be confirmed with compliance team.
 * // See business-rules-specification section REG-02 and AUTH-02.
 */
public interface BiometricVerificationPort {

    /**
     * Initiates a biometric verification session for the given user.
     *
     * @param userId    the user initiating verification
     * @param sessionId correlation ID for tracking this verification attempt
     * @return a redirect URL or provider-session token the client uses to complete verification
     */
    String initiateVerification(UUID userId, UUID sessionId);

    /**
     * Checks the result of a previously-initiated verification session.
     *
     * @param providerSessionToken token returned by {@link #initiateVerification}
     * @return true if the provider confirmed liveness and identity match
     */
    boolean checkVerificationResult(String providerSessionToken);
}
