package uz.pulsepay.transfer.domain.port.out;

import uz.pulsepay.transfer.domain.model.ParticipantRole;
import uz.pulsepay.transfer.domain.model.TransferParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferParticipantRepository {
    TransferParticipant save(TransferParticipant participant);
    Optional<TransferParticipant> findByTransferIdAndRole(UUID transferId, ParticipantRole role);
    List<TransferParticipant> findByTransferId(UUID transferId);
}
