package uz.pulsepay.reference.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.reference.adapter.out.jpa.entity.PaymentNetworkEntity;

import java.util.List;
import java.util.Optional;

interface PaymentNetworkJpaRepository extends JpaRepository<PaymentNetworkEntity, Integer> {
    Optional<PaymentNetworkEntity> findByCode(String code);
    List<PaymentNetworkEntity> findByIsActiveTrue();
}
