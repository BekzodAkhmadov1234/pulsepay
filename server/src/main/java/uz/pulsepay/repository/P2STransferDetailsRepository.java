package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.transfer.P2STransferDetailsEntity;

import java.util.Optional;
import java.util.UUID;

public interface P2STransferDetailsRepository extends JpaRepository<P2STransferDetailsEntity, UUID> {

    Optional<P2STransferDetailsEntity> findByTransferId(UUID transferId);
}
