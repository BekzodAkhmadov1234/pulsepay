package uz.pulsepay.compliance.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.compliance.domain.model.ComplianceFlag;
import uz.pulsepay.compliance.domain.model.FlagType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_flags")
public class ComplianceFlagEntity {

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

    protected ComplianceFlagEntity() {}

    public ComplianceFlagEntity(UUID id, UUID transferId, UUID partyId, FlagType flagType,
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

    public ComplianceFlag toDomain() {
        return new ComplianceFlag(id, transferId, partyId, flagType, regulatoryParameterId,
                status, detectedAt, resolvedAt, resolvedByAdminId, notes);
    }

    public static ComplianceFlagEntity fromDomain(ComplianceFlag f) {
        return new ComplianceFlagEntity(f.id(), f.transferId(), f.partyId(), f.flagType(),
                f.regulatoryParameterId(), f.status(), f.detectedAt(), f.resolvedAt(),
                f.resolvedByAdminId(), f.notes());
    }
}
