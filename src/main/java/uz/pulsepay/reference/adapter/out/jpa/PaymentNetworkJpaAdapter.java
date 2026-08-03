package uz.pulsepay.reference.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.reference.domain.model.PaymentNetwork;
import uz.pulsepay.reference.domain.port.out.PaymentNetworkRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class PaymentNetworkJpaAdapter implements PaymentNetworkRepository {

    private final PaymentNetworkJpaRepository jpa;

    PaymentNetworkJpaAdapter(PaymentNetworkJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PaymentNetwork> findByCode(String code) {
        return jpa.findByCode(code).map(e -> e.toDomain());
    }

    @Override
    public List<PaymentNetwork> findAllActive() {
        return jpa.findByIsActiveTrue().stream().map(e -> e.toDomain()).toList();
    }
}
