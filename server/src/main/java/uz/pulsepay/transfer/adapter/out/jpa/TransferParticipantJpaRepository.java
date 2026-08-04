package uz.pulsepay.transfer.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.transfer.adapter.out.jpa.entity.TransferParticipantEntity;
import uz.pulsepay.transfer.domain.model.ParticipantRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TransferParticipantJpaRepository extends JpaRepository<TransferParticipantEntity, UUID> {
    Optional<TransferParticipantEntity> findByTransferIdAndRole(UUID transferId, ParticipantRole role);
    List<TransferParticipantEntity> findByTransferId(UUID transferId);
}
