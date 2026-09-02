package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.merchant.MerchantAccountEntity;

import java.util.Optional;
import java.util.UUID;

public interface MerchantAccountRepository extends JpaRepository<MerchantAccountEntity, UUID> {

    Optional<MerchantAccountEntity> findByMerchantId(UUID merchantId);
}
