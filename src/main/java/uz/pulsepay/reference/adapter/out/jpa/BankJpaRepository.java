package uz.pulsepay.reference.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.reference.adapter.out.jpa.entity.BankEntity;

import java.util.Optional;
import java.util.UUID;

interface BankJpaRepository extends JpaRepository<BankEntity, UUID> {
    Optional<BankEntity> findByMfoCode(String mfoCode);
}
