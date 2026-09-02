package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(name = "key", length = 64)
    private String key;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_snapshot", columnDefinition = "jsonb")
    private String responseSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyKey() {}

    public IdempotencyKey(String key, UUID userId, String requestHash,
                           String responseSnapshot, Instant createdAt, Instant expiresAt) {
        this.key = key;
        this.userId = userId;
        this.requestHash = requestHash;
        this.responseSnapshot = responseSnapshot;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getKey() { return key; }
    public UUID getUserId() { return userId; }
    public String getRequestHash() { return requestHash; }
    public String getResponseSnapshot() { return responseSnapshot; }
    public void setResponseSnapshot(String responseSnapshot) { this.responseSnapshot = responseSnapshot; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
