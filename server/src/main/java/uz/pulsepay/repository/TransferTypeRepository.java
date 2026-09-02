package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.reference.TransferTypeEntity;

import java.util.List;
import java.util.Optional;

public interface TransferTypeRepository extends JpaRepository<TransferTypeEntity, Integer> {

    Optional<TransferTypeEntity> findByCode(String code);

    List<TransferTypeEntity> findByIsActiveTrue();
}
