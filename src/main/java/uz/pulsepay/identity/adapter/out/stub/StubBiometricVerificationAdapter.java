package uz.pulsepay.identity.adapter.out.stub;

import org.springframework.stereotype.Service;
import uz.pulsepay.identity.domain.port.in.BiometricVerificationPort;

import java.util.UUID;

/**
 * Stub biometric verification adapter.
 *
 * // TODO(external-contract): Replace with the actual CBU-mandated or vendor-specific
 * // biometric liveness provider once the contract and integration details are confirmed.
 * // See business-rules-specification section REG-02 and AUTH-02.
 *
 * This stub always returns "not yet implemented" so the system can compile and run without
 * the real integration. Wire a real provider by implementing {@link BiometricVerificationPort}.
 */
@Service
public class StubBiometricVerificationAdapter implements BiometricVerificationPort {

    @Override
    public String initiateVerification(UUID userId, UUID sessionId) {
        // TODO(external-contract): call actual biometric provider
        throw new UnsupportedOperationException(
                "Biometric verification provider not yet configured — see REG-02 and AUTH-02");
    }

    @Override
    public boolean checkVerificationResult(String providerSessionToken) {
        // TODO(external-contract): call actual biometric provider
        throw new UnsupportedOperationException(
                "Biometric verification provider not yet configured — see REG-02 and AUTH-02");
    }
}
