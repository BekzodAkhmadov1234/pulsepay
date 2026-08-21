package uz.pulsepay.merchant.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.merchant.adapter.out.jpa.entity.MerchantCategoryEntity;

import java.util.Optional;

interface MerchantCategoryJpaRepository extends JpaRepository<MerchantCategoryEntity, Integer> {
    Optional<MerchantCategoryEntity> findByMccCode(String mccCode);
}
