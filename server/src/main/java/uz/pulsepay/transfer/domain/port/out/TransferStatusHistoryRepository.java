package uz.pulsepay.transfer.domain.port.out;

import uz.pulsepay.transfer.domain.model.TransferStatusHistory;

public interface TransferStatusHistoryRepository {
    void save(TransferStatusHistory history);
}
