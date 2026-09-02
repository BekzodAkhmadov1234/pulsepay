package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "regulatory_parameters")
public class RegulatoryParameter {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "value_amount", nullable = false)
    private long valueAmount;

    @Column(name = "unit", length = 10)
    private String unit;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RegulatoryParameter() {}

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public long getValueAmount() { return valueAmount; }
    public String getUnit() { return unit; }
    public String getCurrencyCode() { return currencyCode; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public String getSourceReference() { return sourceReference; }
    public Instant getCreatedAt() { return createdAt; }
}
