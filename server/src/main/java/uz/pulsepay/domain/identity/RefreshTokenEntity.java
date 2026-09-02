package uz.pulsepay.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.identity.RefreshToken;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshTokenEntity() {}

    RefreshTokenEntity(UUID id, UUID userId, String tokenHash, UUID sessionId,
                       Instant expiresAt, Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.sessionId = sessionId;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public UUID getSessionId() { return sessionId; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public RefreshToken toDomain() {
        return new RefreshToken(id, userId, tokenHash, sessionId, expiresAt, revokedAt);
    }

    public static RefreshTokenEntity fromDomain(RefreshToken t) {
        return new RefreshTokenEntity(t.id(), t.userId(), t.tokenHash(), t.sessionId(),
                t.expiresAt(), t.revokedAt());
    }
}
