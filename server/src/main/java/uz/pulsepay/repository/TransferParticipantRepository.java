package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.transfer.TransferParticipantEntity;
import uz.pulsepay.domain.transfer.ParticipantRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferParticipantRepository extends JpaRepository<TransferParticipantEntity, UUID> {

    Optional<TransferParticipantEntity> findByTransferIdAndRole(UUID transferId, ParticipantRole role);

    List<TransferParticipantEntity> findByTransferId(UUID transferId);
}
