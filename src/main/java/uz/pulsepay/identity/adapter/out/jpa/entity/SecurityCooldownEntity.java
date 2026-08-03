package uz.pulsepay.identity.adapter.out.jpa.entity;

import jakarta.persistence.*;
import uz.pulsepay.identity.domain.model.CooldownType;
import uz.pulsepay.identity.domain.model.SecurityCooldown;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_cooldowns")
public class SecurityCooldownEntity {

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

    public SecurityCooldown toDomain() {
        return new SecurityCooldown(id, userId,
                CooldownType.valueOf(cooldownType.toUpperCase()),
                lockedUntil, reason, createdAt);
    }

    public static SecurityCooldownEntity fromDomain(SecurityCooldown c) {
        SecurityCooldownEntity e = new SecurityCooldownEntity();
        e.id           = c.id();
        e.userId       = c.userId();
        e.cooldownType = c.cooldownType().name().toLowerCase();
        e.lockedUntil  = c.lockedUntil();
        e.reason       = c.reason();
        e.createdAt    = c.createdAt();
        return e;
    }
}
