package uz.pulsepay.merchant.domain.port.in;

import uz.pulsepay.merchant.domain.command.OnboardMerchantCommand;
import uz.pulsepay.merchant.domain.model.Merchant;

import java.util.List;
import java.util.UUID;

public interface ManageMerchantPort {
    Merchant onboard(OnboardMerchantCommand cmd);
    Merchant approve(UUID id);
    Merchant reject(UUID id, String reason);
    Merchant suspend(UUID id, String reason);
    List<Merchant> listAll();
    Merchant getById(UUID id);
}
