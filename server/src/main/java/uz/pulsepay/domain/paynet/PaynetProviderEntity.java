package uz.pulsepay.domain.paynet;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "paynet_providers")
public class PaynetProviderEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "service_code", nullable = false, unique = true, length = 50)
    private String serviceCode;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Convert(converter = StringArrayConverter.class)
    @Column(name = "field_names", nullable = false, length = 500)
    private String[] fieldNames;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "party_id", nullable = false)
    private UUID partyId;

    @Column(name = "instrument_id", nullable = false)
    private UUID instrumentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaynetProviderEntity() {}

    public PaynetProviderEntity(UUID id, String serviceCode, String serviceName, String category,
                                String[] fieldNames, boolean isActive, UUID partyId, UUID instrumentId,
                                Instant createdAt) {
        this.id           = id;
        this.serviceCode  = serviceCode;
        this.serviceName  = serviceName;
        this.category     = category;
        this.fieldNames   = fieldNames;
        this.isActive     = isActive;
        this.partyId      = partyId;
        this.instrumentId = instrumentId;
        this.createdAt    = createdAt;
    }

    public PaynetProvider toDomain() {
        return new PaynetProvider(id, serviceCode, serviceName, category,
                fieldNames, isActive, partyId, instrumentId);
    }

    public static PaynetProviderEntity fromDomain(PaynetProvider p) {
        return new PaynetProviderEntity(p.id(), p.serviceCode(), p.serviceName(), p.category(),
                p.fieldNames(), p.isActive(), p.partyId(), p.instrumentId(), Instant.now());
    }
}
