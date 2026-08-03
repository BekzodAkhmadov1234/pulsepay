package uz.pulsepay.transfer.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.transfer.adapter.out.jpa.entity.TransferParticipantEntity;
import uz.pulsepay.transfer.domain.model.ParticipantRole;
import uz.pulsepay.transfer.domain.model.TransferParticipant;
import uz.pulsepay.transfer.domain.port.out.TransferParticipantRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class TransferParticipantJpaAdapter implements TransferParticipantRepository {

    private final TransferParticipantJpaRepository jpa;

    TransferParticipantJpaAdapter(TransferParticipantJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public TransferParticipant save(TransferParticipant participant) {
        return jpa.save(TransferParticipantEntity.fromDomain(participant)).toDomain();
    }

    @Override
    public Optional<TransferParticipant> findByTransferIdAndRole(UUID transferId, ParticipantRole role) {
        return jpa.findByTransferIdAndRole(transferId, role).map(TransferParticipantEntity::toDomain);
    }

    @Override
    public List<TransferParticipant> findByTransferId(UUID transferId) {
        return jpa.findByTransferId(transferId).stream().map(TransferParticipantEntity::toDomain).toList();
    }
}
