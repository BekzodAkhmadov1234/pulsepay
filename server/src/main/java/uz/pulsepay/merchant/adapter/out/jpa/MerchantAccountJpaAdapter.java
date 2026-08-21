package uz.pulsepay.merchant.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.merchant.adapter.out.jpa.entity.MerchantAccountEntity;
import uz.pulsepay.merchant.domain.model.MerchantAccount;
import uz.pulsepay.merchant.domain.port.out.MerchantAccountRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MerchantAccountJpaAdapter implements MerchantAccountRepository {

    private final MerchantAccountJpaRepository jpa;

    public MerchantAccountJpaAdapter(MerchantAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public MerchantAccount save(MerchantAccount account) {
        return jpa.save(MerchantAccountEntity.fromDomain(account)).toDomain();
    }

    @Override
    public Optional<MerchantAccount> findById(UUID id) {
        return jpa.findById(id).map(MerchantAccountEntity::toDomain);
    }

    @Override
    public Optional<MerchantAccount> findByMerchantId(UUID merchantId) {
        return jpa.findByMerchantId(merchantId).map(MerchantAccountEntity::toDomain);
    }
}
