package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.converter.FlagTypeConverter;
import uz.pulsepay.domain.enums.FlagType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_flags")
public class ComplianceFlag {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "party_id")
    private UUID partyId;

    @Convert(converter = FlagTypeConverter.class)
    @Column(name = "flag_type", nullable = false, length = 20)
    private FlagType flagType;

    @Column(name = "regulatory_parameter_id")
    private UUID regulatoryParameterId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by_admin_id")
    private UUID resolvedByAdminId;

    @Column(name = "notes")
    private String notes;

    protected ComplianceFlag() {}

    public ComplianceFlag(UUID id, UUID transferId, UUID partyId, FlagType flagType,
                           UUID regulatoryParameterId, String status, Instant detectedAt,
                           Instant resolvedAt, UUID resolvedByAdminId, String notes) {
        this.id = id;
        this.transferId = transferId;
        this.partyId = partyId;
        this.flagType = flagType;
        this.regulatoryParameterId = regulatoryParameterId;
        this.status = status;
        this.detectedAt = detectedAt;
        this.resolvedAt = resolvedAt;
        this.resolvedByAdminId = resolvedByAdminId;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public UUID getTransferId() { return transferId; }
    public UUID getPartyId() { return partyId; }
    public FlagType getFlagType() { return flagType; }
    public UUID getRegulatoryParameterId() { return regulatoryParameterId; }
    public String getStatus() { return status; }
    public Instant getDetectedAt() { return detectedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public UUID getResolvedByAdminId() { return resolvedByAdminId; }
    public String getNotes() { return notes; }

    public void setStatus(String status) { this.status = status; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setResolvedByAdminId(UUID resolvedByAdminId) { this.resolvedByAdminId = resolvedByAdminId; }
    public void setNotes(String notes) { this.notes = notes; }
}
