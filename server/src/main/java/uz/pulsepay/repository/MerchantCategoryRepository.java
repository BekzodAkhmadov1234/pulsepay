package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.merchant.MerchantCategoryEntity;

import java.util.Optional;

public interface MerchantCategoryRepository extends JpaRepository<MerchantCategoryEntity, Integer> {

    Optional<MerchantCategoryEntity> findByMccCode(String mccCode);
}
