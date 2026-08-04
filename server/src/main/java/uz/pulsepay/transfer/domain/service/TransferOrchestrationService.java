package uz.pulsepay.transfer.domain.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.reference.domain.model.TransferType;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.model.TransferChannel;

/**
 * Enforces cross-cutting transfer invariants that cannot be expressed as DB CHECKs
 * (Risk #10 — channel invariant; Risk #4 — party-type rules in code for typed exceptions).
 */
@Service
public class TransferOrchestrationService {

    /**
     * Risk #10: P2P over web is prohibited.
     */
    public void validateChannel(TransferChannel channel, TransferType transferType) {
        if (channel == TransferChannel.WEB && "p2p".equals(transferType.code())) {
            throw new DomainException("P2P transfers via web channel are prohibited");
        }
    }

    /**
     * Risk #4: Validate party types against transfer type's allowed types.
     */
    public void validatePartyTypes(String senderPartyType, String recipientPartyType,
                                   TransferType transferType) {
        if (transferType.allowedSenderPartyTypes() != null
                && !transferType.allowedSenderPartyTypes().contains(senderPartyType)) {
            throw new DomainException(
                    "Sender party type '%s' is not allowed for transfer type '%s'"
                            .formatted(senderPartyType, transferType.code()));
        }
        if (transferType.allowedRecipientPartyTypes() != null
                && !transferType.allowedRecipientPartyTypes().contains(recipientPartyType)) {
            throw new DomainException(
                    "Recipient party type '%s' is not allowed for transfer type '%s'"
                            .formatted(recipientPartyType, transferType.code()));
        }
    }
}
