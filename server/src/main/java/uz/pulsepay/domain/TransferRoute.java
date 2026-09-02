package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_routes")
public class TransferRoute {

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

    protected TransferRoute() {}

    public UUID getId() { return id; }
    public String getRouteCode() { return routeCode; }
    public String getSourceNetwork() { return sourceNetwork; }
    public String getDestinationNetwork() { return destinationNetwork; }
    public String getProcessorName() { return processorName; }
    public Long getMaxAmount() { return maxAmount; }
    public int getPriority() { return priority; }
    public Integer getAvgProcessingSeconds() { return avgProcessingSeconds; }
    public boolean isActive() { return isActive; }
    public Integer getTransferTypeId() { return transferTypeId; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }

    public void setId(UUID id) { this.id = id; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }
    public void setSourceNetwork(String sourceNetwork) { this.sourceNetwork = sourceNetwork; }
    public void setDestinationNetwork(String destinationNetwork) { this.destinationNetwork = destinationNetwork; }
    public void setProcessorName(String processorName) { this.processorName = processorName; }
    public void setMaxAmount(Long maxAmount) { this.maxAmount = maxAmount; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setAvgProcessingSeconds(Integer avgProcessingSeconds) { this.avgProcessingSeconds = avgProcessingSeconds; }
    public void setActive(boolean isActive) { this.isActive = isActive; }
    public void setTransferTypeId(Integer transferTypeId) { this.transferTypeId = transferTypeId; }
    public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public void setEffectiveTo(Instant effectiveTo) { this.effectiveTo = effectiveTo; }
}
