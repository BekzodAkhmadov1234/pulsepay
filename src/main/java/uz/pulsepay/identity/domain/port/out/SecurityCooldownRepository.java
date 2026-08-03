package uz.pulsepay.identity.domain.port.out;

import uz.pulsepay.identity.domain.model.CooldownType;
import uz.pulsepay.identity.domain.model.SecurityCooldown;

import java.util.Optional;
import java.util.UUID;

public interface SecurityCooldownRepository {

    /** Returns the active (locked_until > now) cooldown for this user and type, if any. */
    Optional<SecurityCooldown> findActiveCooldown(UUID userId, CooldownType type);

    SecurityCooldown save(SecurityCooldown cooldown);
}
