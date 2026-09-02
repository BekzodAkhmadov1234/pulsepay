package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.reference.BankEntity;

import java.util.Optional;
import java.util.UUID;

public interface BankRepository extends JpaRepository<BankEntity, UUID> {

    Optional<BankEntity> findByMfoCode(String mfoCode);
}
