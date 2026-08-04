package uz.pulsepay.reference.domain.port.out;

import uz.pulsepay.reference.domain.model.Bank;

import java.util.Optional;
import java.util.UUID;

public interface BankRepository {
    Optional<Bank> findById(UUID id);
    Optional<Bank> findByMfoCode(String mfoCode);
}
