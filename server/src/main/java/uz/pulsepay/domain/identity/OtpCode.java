package uz.pulsepay.domain.identity;

import java.time.Instant;
import java.util.UUID;

public record OtpCode(
        UUID id,
        UUID userId,
        OtpPurpose purpose,
        String codeHash,
        UUID targetId,
        Instant expiresAt,
        Instant consumedAt,
        short attemptCount
) {
    public boolean isValid() {
        return consumedAt == null && expiresAt.isAfter(Instant.now()) && attemptCount < 5;
    }
}
