package uz.pulsepay.transfer.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.transfer.adapter.out.jpa.entity.TransferStatusHistoryEntity;

import java.util.UUID;

interface TransferStatusHistoryJpaRepository extends JpaRepository<TransferStatusHistoryEntity, UUID> {
}
