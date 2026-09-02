package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.enums.CooldownType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_cooldowns")
public class SecurityCooldown {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "cooldown_type", nullable = false, length = 30)
    private String cooldownType;

    @Column(name = "locked_until", nullable = false)
    private Instant lockedUntil;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SecurityCooldown() {}

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public CooldownType getCooldownType() {
        return CooldownType.valueOf(cooldownType.toUpperCase());
    }
    public Instant getLockedUntil() { return lockedUntil; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setCooldownType(CooldownType cooldownType) {
        this.cooldownType = cooldownType.name().toLowerCase();
    }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public void setReason(String reason) { this.reason = reason; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
