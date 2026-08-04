package uz.pulsepay.identity.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.identity.domain.model.Session;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id")
    private UUID deviceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected SessionEntity() {}

    SessionEntity(UUID id, UUID userId, UUID deviceId, String ipAddress,
                  Instant createdAt, Instant expiresAt, Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public Session toDomain() {
        return new Session(id, userId, deviceId, ipAddress, createdAt, expiresAt, revokedAt);
    }

    public static SessionEntity fromDomain(Session s) {
        return new SessionEntity(s.id(), s.userId(), s.deviceId(), s.ipAddress(),
                s.createdAt(), s.expiresAt(), s.revokedAt());
    }
}
