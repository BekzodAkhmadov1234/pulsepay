package uz.pulsepay.party.domain.port.out;

import uz.pulsepay.party.domain.model.Instrument;

import java.util.Optional;
import java.util.UUID;

public interface InstrumentRepository {
    Instrument save(Instrument instrument);
    Optional<Instrument> findById(UUID id);

    /**
     * Composite FK check: only returns the instrument if it belongs to the given party.
     * Used by TransferParticipantService to enforce the composite FK invariant.
     */
    Optional<Instrument> findByIdAndOwnerPartyId(UUID instrumentId, UUID ownerPartyId);
}
