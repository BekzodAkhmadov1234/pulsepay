package uz.pulsepay.merchant.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.merchant.adapter.out.jpa.entity.MerchantEntity;
import uz.pulsepay.merchant.domain.model.Merchant;
import uz.pulsepay.merchant.domain.port.out.MerchantRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MerchantJpaAdapter implements MerchantRepository {

    private final MerchantJpaRepository jpa;

    public MerchantJpaAdapter(MerchantJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Merchant save(Merchant merchant) {
        jpa.upsertMerchantParty(merchant.id());
        return jpa.save(MerchantEntity.fromDomain(merchant)).toDomain();
    }

    @Override
    public Optional<Merchant> findById(UUID id) {
        return jpa.findById(id).map(MerchantEntity::toDomain);
    }

    @Override
    public Optional<Merchant> findByEmail(String email) {
        return jpa.findByEmail(email).map(MerchantEntity::toDomain);
    }

    @Override
    public List<Merchant> findAll() {
        return jpa.findAll().stream().map(MerchantEntity::toDomain).toList();
    }

    /** Inserts the instrument row for a merchant account (id → instruments FK). */
    @Transactional
    public void upsertMerchantAccountInstrument(UUID accountId, UUID merchantId) {
        jpa.upsertMerchantAccountInstrument(accountId, merchantId);
    }
}
