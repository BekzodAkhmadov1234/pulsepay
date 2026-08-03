package uz.pulsepay.reference.domain.port.out;

import uz.pulsepay.reference.domain.model.PurposeCode;

import java.util.List;
import java.util.Optional;

public interface PurposeCodeRepository {
    Optional<PurposeCode> findById(int id);
    List<PurposeCode> findByTransferTypeId(int transferTypeId);
}
