package uz.pulsepay.party.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.party.adapter.out.jpa.entity.PartyEntity;
import uz.pulsepay.party.domain.model.Party;
import uz.pulsepay.party.domain.model.PartyType;
import uz.pulsepay.party.domain.port.out.PartyRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
class PartyJpaAdapter implements PartyRepository {

    private final PartyJpaRepository jpa;

    PartyJpaAdapter(PartyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Party save(Party party) {
        return jpa.save(PartyEntity.fromDomain(party)).toDomain();
    }

    @Override
    public Optional<Party> findById(UUID id) {
        return jpa.findById(id).map(PartyEntity::toDomain);
    }

    @Override
    public Optional<Party> findByIdAndType(UUID id, PartyType type) {
        return jpa.findByIdAndPartyType(id, type).map(PartyEntity::toDomain);
    }
}
