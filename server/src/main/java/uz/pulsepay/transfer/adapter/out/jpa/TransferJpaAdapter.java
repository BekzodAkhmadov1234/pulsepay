package uz.pulsepay.transfer.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.shared.domain.CurrencyCode;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.NotFoundException;
import uz.pulsepay.transfer.adapter.out.jpa.entity.TransferEntity;
import uz.pulsepay.transfer.domain.model.ParticipantRole;
import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.model.TransferChannel;
import uz.pulsepay.transfer.domain.model.TransferStatus;
import uz.pulsepay.transfer.domain.model.TransferSummary;
import uz.pulsepay.transfer.domain.port.out.TransferRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class TransferJpaAdapter implements TransferRepository {

    private final TransferJpaRepository jpa;

    TransferJpaAdapter(TransferJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Transfer save(Transfer transfer) {
        return jpa.save(TransferEntity.fromDomain(transfer)).toDomain();
    }

    @Override
    public Optional<Transfer> findById(UUID id) {
        return jpa.findById(id).map(TransferEntity::toDomain);
    }

    @Override
    public Optional<Transfer> findByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey).map(TransferEntity::toDomain);
    }

    @Override
    public List<Transfer> findBySenderId(UUID senderId) {
        return jpa.findBySenderIdAndRole(senderId, ParticipantRole.SENDER)
                .stream().map(TransferEntity::toDomain).toList();
    }

    @Override
    public List<TransferSummary> findSummariesByParticipantId(UUID userId) {
        return jpa.findSummariesByParticipantId(userId).stream().map(row -> {
            UUID id = (UUID) row[0];
            long amountTiyin = ((Number) row[1]).longValue();
            long feeTiyin = ((Number) row[2]).longValue();
            String currencyCode = (String) row[3];
            TransferStatus status = TransferStatus.valueOf(((String) row[4]).toUpperCase());
            TransferChannel channel = TransferChannel.valueOf(((String) row[5]).toUpperCase());
            String idempotencyKey = (String) row[6];
            String initiatedAt = (String) row[7];
            String completedAt = (String) row[8];
            String senderName = (String) row[9];
            String senderMaskedPan = (String) row[10];
            String recipientName = (String) row[11];
            String recipientMaskedPan = (String) row[12];
            String processedAt = (String) row[13];
            String direction = (String) row[14];
            return new TransferSummary(id,
                    new Money(amountTiyin, CurrencyCode.valueOf(currencyCode)),
                    new Money(feeTiyin, CurrencyCode.valueOf(currencyCode)),
                    status, channel, idempotencyKey, initiatedAt, completedAt,
                    senderName, senderMaskedPan, recipientName, recipientMaskedPan, processedAt, direction);
        }).toList();
    }

    @Override
    public Transfer updateStatus(UUID id, TransferStatus newStatus, String reason) {
        Instant completedAt = (newStatus == TransferStatus.COMPLETED || newStatus == TransferStatus.FAILED)
                ? Instant.now() : null;
        jpa.updateStatus(id, newStatus, completedAt);
        return jpa.findById(id)
                .map(TransferEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Transfer not found: " + id));
    }
}
