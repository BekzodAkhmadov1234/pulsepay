package uz.pulsepay.transfer.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.transfer.adapter.out.jpa.entity.TransferStatusHistoryEntity;
import uz.pulsepay.transfer.domain.model.TransferStatusHistory;
import uz.pulsepay.transfer.domain.port.out.TransferStatusHistoryRepository;

@Repository
class TransferStatusHistoryJpaAdapter implements TransferStatusHistoryRepository {

    private final TransferStatusHistoryJpaRepository jpa;

    TransferStatusHistoryJpaAdapter(TransferStatusHistoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(TransferStatusHistory history) {
        jpa.save(TransferStatusHistoryEntity.fromDomain(history));
    }
}
