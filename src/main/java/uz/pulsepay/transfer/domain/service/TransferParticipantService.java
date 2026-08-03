package uz.pulsepay.transfer.domain.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.party.domain.model.Instrument;
import uz.pulsepay.party.domain.port.out.InstrumentRepository;
import uz.pulsepay.shared.exception.DomainException;

import java.util.UUID;

/**
 * Enforces the composite FK invariant (Risk #3):
 * an instrument must belong to the claimed party before being used in a transfer.
 */
@Service
public class TransferParticipantService {

    private final InstrumentRepository instrumentRepository;

    public TransferParticipantService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    public Instrument validateInstrumentOwnership(UUID instrumentId, UUID partyId) {
        return instrumentRepository
                .findByIdAndOwnerPartyId(instrumentId, partyId)
                .filter(Instrument::isUsable)
                .orElseThrow(() -> new DomainException(
                        "Instrument %s does not belong to party %s or is not usable"
                                .formatted(instrumentId, partyId)));
    }
}
