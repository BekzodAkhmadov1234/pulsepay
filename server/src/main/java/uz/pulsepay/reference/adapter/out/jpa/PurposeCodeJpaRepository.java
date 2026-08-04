package uz.pulsepay.reference.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.reference.adapter.out.jpa.entity.PurposeCodeEntity;

import java.util.List;

interface PurposeCodeJpaRepository extends JpaRepository<PurposeCodeEntity, Integer> {
    List<PurposeCodeEntity> findByApplicableTransferTypeId(int transferTypeId);
}
