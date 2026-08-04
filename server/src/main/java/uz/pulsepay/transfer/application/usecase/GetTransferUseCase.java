package uz.pulsepay.transfer.application.usecase;

import org.springframework.stereotype.Service;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;
import uz.pulsepay.transfer.domain.model.ParticipantRole;
import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.port.in.GetTransferPort;
import uz.pulsepay.transfer.domain.port.out.TransferParticipantRepository;
import uz.pulsepay.transfer.domain.port.out.TransferRepository;

import java.util.UUID;

@Service
public class GetTransferUseCase implements GetTransferPort {

    private final TransferRepository transferRepository;
    private final TransferParticipantRepository participantRepository;

    public GetTransferUseCase(TransferRepository transferRepository,
                               TransferParticipantRepository participantRepository) {
        this.transferRepository = transferRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    public Transfer getById(UUID transferId, UUID requestingUserId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new NotFoundException("Transfer not found"));

        // Verify the requesting user is a participant
        boolean isSender = participantRepository
                .findByTransferIdAndRole(transferId, ParticipantRole.SENDER)
                .map(p -> p.partyId().equals(requestingUserId))
                .orElse(false);
        boolean isRecipient = participantRepository
                .findByTransferIdAndRole(transferId, ParticipantRole.RECIPIENT)
                .map(p -> p.partyId().equals(requestingUserId))
                .orElse(false);

        if (!isSender && !isRecipient) {
            throw new DomainException("Access denied: not a participant in this transfer");
        }
        return transfer;
    }
}
