package uz.pulsepay.merchant.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.merchant.adapter.out.jpa.entity.MerchantAccountEntity;

import java.util.Optional;
import java.util.UUID;

interface MerchantAccountJpaRepository extends JpaRepository<MerchantAccountEntity, UUID> {
    Optional<MerchantAccountEntity> findByMerchantId(UUID merchantId);
}
