package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.identity.adapter.out.jpa.entity.RefreshTokenEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :now WHERE r.sessionId = :sessionId AND r.revokedAt IS NULL")
    void revokeAllForSession(@Param("sessionId") UUID sessionId, @Param("now") Instant now);
}
