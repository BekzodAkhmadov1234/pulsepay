package uz.pulsepay.reference.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.reference.adapter.out.jpa.entity.TransferTypeEntity;

import java.util.List;
import java.util.Optional;

interface TransferTypeJpaRepository extends JpaRepository<TransferTypeEntity, Integer> {
    Optional<TransferTypeEntity> findByCode(String code);
    List<TransferTypeEntity> findByIsActiveTrue();
}
