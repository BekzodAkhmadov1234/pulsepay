package uz.pulsepay.transfer.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.party.adapter.out.jpa.entity.InstrumentTypeConverter;
import uz.pulsepay.party.adapter.out.jpa.entity.PartyTypeConverter;
import uz.pulsepay.transfer.adapter.out.jpa.entity.ParticipantRoleConverter;
import uz.pulsepay.party.domain.model.InstrumentType;
import uz.pulsepay.party.domain.model.PartyType;
import uz.pulsepay.transfer.domain.model.ParticipantRole;
import uz.pulsepay.transfer.domain.model.TransferParticipant;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_participants")
public class TransferParticipantEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Convert(converter = ParticipantRoleConverter.class)
    @Column(name = "role", nullable = false, length = 10)
    private ParticipantRole role;

    @Column(name = "party_id", nullable = false)
    private UUID partyId;

    @Convert(converter = PartyTypeConverter.class)
    @Column(name = "party_type", nullable = false, length = 20)
    private PartyType partyType;

    @Column(name = "instrument_id", nullable = false)
    private UUID instrumentId;

    @Convert(converter = InstrumentTypeConverter.class)
    @Column(name = "instrument_type", nullable = false, length = 20)
    private InstrumentType instrumentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TransferParticipantEntity() {}

    TransferParticipantEntity(UUID id, UUID transferId, ParticipantRole role,
                               UUID partyId, PartyType partyType,
                               UUID instrumentId, InstrumentType instrumentType, Instant createdAt) {
        this.id = id;
        this.transferId = transferId;
        this.role = role;
        this.partyId = partyId;
        this.partyType = partyType;
        this.instrumentId = instrumentId;
        this.instrumentType = instrumentType;
        this.createdAt = createdAt;
    }

    public TransferParticipant toDomain() {
        return new TransferParticipant(id, transferId, role, partyId, partyType,
                instrumentId, instrumentType, createdAt);
    }

    public static TransferParticipantEntity fromDomain(TransferParticipant p) {
        return new TransferParticipantEntity(p.id(), p.transferId(), p.role(),
                p.partyId(), p.partyType(), p.instrumentId(), p.instrumentType(), p.createdAt());
    }
}
