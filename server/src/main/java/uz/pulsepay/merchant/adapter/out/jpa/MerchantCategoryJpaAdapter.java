package uz.pulsepay.merchant.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.merchant.domain.model.MerchantCategory;
import uz.pulsepay.merchant.domain.port.out.MerchantCategoryRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class MerchantCategoryJpaAdapter implements MerchantCategoryRepository {

    private final MerchantCategoryJpaRepository jpa;

    public MerchantCategoryJpaAdapter(MerchantCategoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<MerchantCategory> findAll() {
        return jpa.findAll().stream().map(e -> e.toDomain()).toList();
    }

    @Override
    public Optional<MerchantCategory> findByMccCode(String mccCode) {
        return jpa.findByMccCode(mccCode).map(e -> e.toDomain());
    }
}
