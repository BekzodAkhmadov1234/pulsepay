package uz.pulsepay.reference.domain.port.out;

import uz.pulsepay.reference.domain.model.TransferType;

import java.util.List;
import java.util.Optional;

public interface TransferTypeRepository {
    Optional<TransferType> findById(int id);
    Optional<TransferType> findByCode(String code);
    List<TransferType> findAllActive();
}
