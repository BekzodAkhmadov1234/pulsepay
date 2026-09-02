package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.party.BankAccountDetailsEntity;

import java.util.Optional;
import java.util.UUID;

public interface BankAccountDetailsRepository extends JpaRepository<BankAccountDetailsEntity, UUID> {

    Optional<BankAccountDetailsEntity> findByIban(String iban);

    Optional<BankAccountDetailsEntity> findByInstrumentId(UUID instrumentId);
}
