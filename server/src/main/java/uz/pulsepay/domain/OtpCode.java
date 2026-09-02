package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.converter.OtpPurposeConverter;
import uz.pulsepay.domain.enums.OtpPurpose;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_codes")
public class OtpCode {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Convert(converter = OtpPurposeConverter.class)
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

    protected OtpCode() {}

    public OtpCode(UUID id, UUID userId, OtpPurpose purpose, String codeHash,
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
    public UUID getUserId() { return userId; }
    public OtpPurpose getPurpose() { return purpose; }
    public String getCodeHash() { return codeHash; }
    public UUID getTargetId() { return targetId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public short getAttemptCount() { return attemptCount; }

    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
    public void incrementAttemptCount() { this.attemptCount++; }
}
