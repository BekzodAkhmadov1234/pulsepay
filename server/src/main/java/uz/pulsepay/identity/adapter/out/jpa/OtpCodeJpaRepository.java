package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.identity.adapter.out.jpa.entity.OtpCodeEntity;
import uz.pulsepay.identity.domain.model.OtpPurpose;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface OtpCodeJpaRepository extends JpaRepository<OtpCodeEntity, UUID> {

    @Query("SELECT o FROM OtpCodeEntity o WHERE o.userId = :userId AND o.purpose = :purpose " +
           "ORDER BY o.expiresAt DESC LIMIT 1")
    Optional<OtpCodeEntity> findLatestByUserIdAndPurpose(@Param("userId") UUID userId,
                                                          @Param("purpose") OtpPurpose purpose);

    @Modifying
    @Query("UPDATE OtpCodeEntity o SET o.consumedAt = :now WHERE o.id = :id AND o.consumedAt IS NULL")
    void markConsumed(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE OtpCodeEntity o SET o.attemptCount = o.attemptCount + 1 WHERE o.id = :id")
    void incrementAttemptCount(@Param("id") UUID id);

    @Query("SELECT o.attemptCount FROM OtpCodeEntity o WHERE o.id = :id")
    short findAttemptCount(@Param("id") UUID id);
}
