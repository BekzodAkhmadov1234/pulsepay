package uz.pulsepay.reference.domain.port.out;

import uz.pulsepay.reference.domain.model.PaymentNetwork;

import java.util.List;
import java.util.Optional;

public interface PaymentNetworkRepository {
    Optional<PaymentNetwork> findByCode(String code);
    List<PaymentNetwork> findAllActive();
}
