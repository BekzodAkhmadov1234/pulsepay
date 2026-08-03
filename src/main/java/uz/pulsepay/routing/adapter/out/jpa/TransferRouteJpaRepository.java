package uz.pulsepay.routing.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.routing.adapter.out.jpa.entity.TransferRouteEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface TransferRouteJpaRepository extends JpaRepository<TransferRouteEntity, UUID> {

    @Query("""
            SELECT r FROM TransferRouteEntity r
            WHERE r.isActive = true
            AND r.sourceNetwork = :sourceNetwork
            AND r.destinationNetwork = :destNetwork
            AND (r.transferTypeId IS NULL OR r.transferTypeId = :transferTypeId)
            AND r.effectiveFrom <= :now
            AND (r.effectiveTo IS NULL OR r.effectiveTo > :now)
            ORDER BY r.priority ASC
            """)
    List<TransferRouteEntity> findActiveRoutes(@Param("sourceNetwork") String sourceNetwork,
                                                @Param("destNetwork") String destNetwork,
                                                @Param("transferTypeId") int transferTypeId,
                                                @Param("now") Instant now);
}
