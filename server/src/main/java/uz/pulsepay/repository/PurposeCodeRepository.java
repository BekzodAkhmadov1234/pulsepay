package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.reference.PurposeCodeEntity;

import java.util.List;

public interface PurposeCodeRepository extends JpaRepository<PurposeCodeEntity, Integer> {

    List<PurposeCodeEntity> findByApplicableTransferTypeId(int transferTypeId);
}
