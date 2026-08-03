package uz.pulsepay.identity.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.identity.domain.model.OtpCode;
import uz.pulsepay.identity.domain.model.OtpPurpose;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_codes")
public class OtpCodeEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "attempt_count", nullable = false)
    private short attemptCount;

    protected OtpCodeEntity() {}

    OtpCodeEntity(UUID id, UUID userId, OtpPurpose purpose, String codeHash,
                  UUID targetId, Instant expiresAt, Instant consumedAt, short attemptCount) {
        this.id = id;
        this.userId = userId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.targetId = targetId;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.attemptCount = attemptCount;
    }

    public UUID getId() { return id; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
    public void incrementAttemptCount() { this.attemptCount++; }

    public OtpCode toDomain() {
        return new OtpCode(id, userId, purpose, codeHash, targetId, expiresAt, consumedAt, attemptCount);
    }

    public static OtpCodeEntity fromDomain(OtpCode o) {
        return new OtpCodeEntity(o.id(), o.userId(), o.purpose(), o.codeHash(),
                o.targetId(), o.expiresAt(), o.consumedAt(), o.attemptCount());
    }
}
