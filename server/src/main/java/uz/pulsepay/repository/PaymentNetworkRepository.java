package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.reference.PaymentNetworkEntity;

import java.util.List;
import java.util.Optional;

public interface PaymentNetworkRepository extends JpaRepository<PaymentNetworkEntity, Integer> {

    Optional<PaymentNetworkEntity> findByCode(String code);

    List<PaymentNetworkEntity> findByIsActiveTrue();
}
