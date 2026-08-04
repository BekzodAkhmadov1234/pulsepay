package uz.pulsepay.party.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.party.adapter.out.jpa.entity.InstrumentEntity;
import uz.pulsepay.party.domain.model.Instrument;
import uz.pulsepay.party.domain.port.out.InstrumentRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
class InstrumentJpaAdapter implements InstrumentRepository {

    private final InstrumentJpaRepository jpa;

    InstrumentJpaAdapter(InstrumentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Instrument save(Instrument instrument) {
        return jpa.save(InstrumentEntity.fromDomain(instrument)).toDomain();
    }

    @Override
    public Optional<Instrument> findById(UUID id) {
        return jpa.findById(id).map(InstrumentEntity::toDomain);
    }

    @Override
    public Optional<Instrument> findByIdAndOwnerPartyId(UUID instrumentId, UUID ownerPartyId) {
        return jpa.findByIdAndOwnerPartyId(instrumentId, ownerPartyId).map(InstrumentEntity::toDomain);
    }
}
