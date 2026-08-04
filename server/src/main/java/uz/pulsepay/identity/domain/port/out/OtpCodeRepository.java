package uz.pulsepay.identity.domain.port.out;

import uz.pulsepay.identity.domain.model.OtpCode;
import uz.pulsepay.identity.domain.model.OtpPurpose;

import java.util.Optional;
import java.util.UUID;

public interface OtpCodeRepository {
    OtpCode save(OtpCode otpCode);
    Optional<OtpCode> findLatestByUserIdAndPurpose(UUID userId, OtpPurpose purpose);
    void markConsumed(UUID id);

    /**
     * Atomically increments the attempt counter and returns the new count.
     * Callers use the returned count to decide whether to apply a lockout.
     */
    int incrementAttemptCount(UUID id);
}
