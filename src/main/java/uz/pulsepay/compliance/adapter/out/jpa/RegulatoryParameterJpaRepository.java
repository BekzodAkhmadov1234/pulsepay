package uz.pulsepay.compliance.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.compliance.adapter.out.jpa.entity.RegulatoryParameterEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface RegulatoryParameterJpaRepository extends JpaRepository<RegulatoryParameterEntity, UUID> {

    @Query("""
            SELECT p FROM RegulatoryParameterEntity p
            WHERE p.code = :code
            AND p.effectiveFrom <= :now
            AND (p.effectiveTo IS NULL OR p.effectiveTo > :now)
            ORDER BY p.effectiveFrom DESC
            LIMIT 1
            """)
    Optional<RegulatoryParameterEntity> findCurrentByCode(@Param("code") String code,
                                                           @Param("now") Instant now);
}
