package uz.pulsepay.party.domain.port.out;

import uz.pulsepay.party.domain.model.Party;
import uz.pulsepay.party.domain.model.PartyType;

import java.util.Optional;
import java.util.UUID;

public interface PartyRepository {
    Party save(Party party);
    Optional<Party> findById(UUID id);
    Optional<Party> findByIdAndType(UUID id, PartyType type);
}
