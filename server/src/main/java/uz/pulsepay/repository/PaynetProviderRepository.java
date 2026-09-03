package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.paynet.PaynetProviderEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaynetProviderRepository extends JpaRepository<PaynetProviderEntity, UUID> {

    Optional<PaynetProviderEntity> findByServiceCode(String serviceCode);

    List<PaynetProviderEntity> findAllByIsActiveTrue();
}
