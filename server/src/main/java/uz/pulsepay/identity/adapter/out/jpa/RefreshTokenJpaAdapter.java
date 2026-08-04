package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.identity.adapter.out.jpa.entity.RefreshTokenEntity;
import uz.pulsepay.identity.domain.model.RefreshToken;
import uz.pulsepay.identity.domain.port.out.RefreshTokenRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class RefreshTokenJpaAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    RefreshTokenJpaAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenEntity::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return jpa.save(RefreshTokenEntity.fromDomain(token)).toDomain();
    }

    @Override
    public void revokeAllForSession(UUID sessionId) {
        jpa.revokeAllForSession(sessionId, Instant.now());
    }
}
