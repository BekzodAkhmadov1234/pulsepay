package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.identity.adapter.out.jpa.entity.SecurityCooldownEntity;
import uz.pulsepay.identity.domain.model.CooldownType;
import uz.pulsepay.identity.domain.model.SecurityCooldown;
import uz.pulsepay.identity.domain.port.out.SecurityCooldownRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class SecurityCooldownJpaAdapter implements SecurityCooldownRepository {

    private final SecurityCooldownJpaRepository jpa;

    SecurityCooldownJpaAdapter(SecurityCooldownJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<SecurityCooldown> findActiveCooldown(UUID userId, CooldownType type) {
        return jpa.findActiveCooldown(userId, type.name().toLowerCase(), Instant.now())
                .map(SecurityCooldownEntity::toDomain);
    }

    @Override
    public SecurityCooldown save(SecurityCooldown cooldown) {
        return jpa.save(SecurityCooldownEntity.fromDomain(cooldown)).toDomain();
    }
}
