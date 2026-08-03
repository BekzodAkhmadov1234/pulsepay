package uz.pulsepay.routing.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.routing.domain.model.TransferRoute;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_routes")
public class TransferRouteEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "route_code", nullable = false, unique = true, length = 30)
    private String routeCode;

    @Column(name = "source_network", nullable = false, length = 20)
    private String sourceNetwork;

    @Column(name = "destination_network", nullable = false, length = 20)
    private String destinationNetwork;

    @Column(name = "processor_name", nullable = false, length = 30)
    private String processorName;

    @Column(name = "max_amount")
    private Long maxAmount;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "avg_processing_seconds")
    private Integer avgProcessingSeconds;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "transfer_type_id")
    private Integer transferTypeId;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    protected TransferRouteEntity() {}

    public TransferRoute toDomain() {
        return new TransferRoute(id, routeCode, sourceNetwork, destinationNetwork, processorName,
                maxAmount, priority, avgProcessingSeconds, isActive, transferTypeId,
                effectiveFrom, effectiveTo);
    }
}
