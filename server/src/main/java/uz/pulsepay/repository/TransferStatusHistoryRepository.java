package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.transfer.TransferStatusHistoryEntity;

import java.util.UUID;

public interface TransferStatusHistoryRepository extends JpaRepository<TransferStatusHistoryEntity, UUID> {
}
