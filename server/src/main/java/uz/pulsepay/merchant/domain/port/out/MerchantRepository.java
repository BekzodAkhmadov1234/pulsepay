package uz.pulsepay.merchant.domain.port.out;

import uz.pulsepay.merchant.domain.model.Merchant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository {
    Merchant save(Merchant merchant);
    Optional<Merchant> findById(UUID id);
    Optional<Merchant> findByEmail(String email);
    List<Merchant> findAll();
}
