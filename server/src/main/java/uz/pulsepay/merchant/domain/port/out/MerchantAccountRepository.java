package uz.pulsepay.merchant.domain.port.out;

import uz.pulsepay.merchant.domain.model.MerchantAccount;

import java.util.Optional;
import java.util.UUID;

public interface MerchantAccountRepository {
    MerchantAccount save(MerchantAccount account);
    Optional<MerchantAccount> findById(UUID id);
    Optional<MerchantAccount> findByMerchantId(UUID merchantId);
}
