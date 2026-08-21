package uz.pulsepay.merchant.domain.port.out;

import uz.pulsepay.merchant.domain.model.MerchantCategory;

import java.util.List;
import java.util.Optional;

public interface MerchantCategoryRepository {
    List<MerchantCategory> findAll();
    Optional<MerchantCategory> findByMccCode(String mccCode);
}
