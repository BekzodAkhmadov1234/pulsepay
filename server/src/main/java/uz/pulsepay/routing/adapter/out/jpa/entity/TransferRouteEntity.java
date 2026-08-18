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
    UUID id;

    @Column(name = "route_code", nullable = false, unique = true, length = 30)
    String routeCode;

    @Column(name = "source_network", nullable = false, length = 20)
    String sourceNetwork;

    @Column(name = "destination_network", nullable = false, length = 20)
    String destinationNetwork;

    @Column(name = "processor_name", nullable = false, length = 30)
    String processorName;

    @Column(name = "max_amount")
    Long maxAmount;

    @Column(name = "priority", nullable = false)
    int priority;

    @Column(name = "avg_processing_seconds")
    Integer avgProcessingSeconds;

    @Column(name = "is_active", nullable = false)
    boolean isActive;

    @Column(name = "transfer_type_id")
    Integer transferTypeId;

    @Column(name = "effective_from", nullable = false)
    Instant effectiveFrom;

    @Column(name = "effective_to")
    Instant effectiveTo;

    protected TransferRouteEntity() {}

    public static TransferRouteEntity fromDomain(TransferRoute r) {
        TransferRouteEntity e = new TransferRouteEntity();
        e.id = r.id();
        e.routeCode = r.routeCode();
        e.sourceNetwork = r.sourceNetwork();
        e.destinationNetwork = r.destinationNetwork();
        e.processorName = r.processorName();
        e.maxAmount = r.maxAmount();
        e.priority = r.priority();
        e.avgProcessingSeconds = r.avgProcessingSeconds();
        e.isActive = r.isActive();
        e.transferTypeId = r.transferTypeId();
        e.effectiveFrom = r.effectiveFrom();
        e.effectiveTo = r.effectiveTo();
        return e;
    }

    public TransferRoute toDomain() {
        return new TransferRoute(id, routeCode, sourceNetwork, destinationNetwork, processorName,
                maxAmount, priority, avgProcessingSeconds, isActive, transferTypeId,
                effectiveFrom, effectiveTo);
    }
}
