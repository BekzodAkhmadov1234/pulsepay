package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.domain.identity.SecurityCooldownEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SecurityCooldownRepository extends JpaRepository<SecurityCooldownEntity, UUID> {

    @Query("""
            SELECT c FROM SecurityCooldownEntity c
            WHERE c.userId = :userId
            AND c.cooldownType = :cooldownType
            AND c.lockedUntil > :now
            ORDER BY c.lockedUntil DESC
            LIMIT 1
            """)
    Optional<SecurityCooldownEntity> findActiveCooldown(
            @Param("userId") UUID userId,
            @Param("cooldownType") String cooldownType,
            @Param("now") Instant now);
}
