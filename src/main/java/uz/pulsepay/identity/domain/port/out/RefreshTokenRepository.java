package uz.pulsepay.identity.domain.port.out;

import uz.pulsepay.identity.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    RefreshToken save(RefreshToken token);
    void revokeAllForSession(UUID sessionId);
}
