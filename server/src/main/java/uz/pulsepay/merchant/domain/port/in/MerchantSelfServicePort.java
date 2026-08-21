package uz.pulsepay.merchant.domain.port.in;

import uz.pulsepay.merchant.domain.model.Merchant;
import uz.pulsepay.merchant.domain.model.MerchantAccount;
import uz.pulsepay.transfer.domain.model.TransferSummary;

import java.util.List;
import java.util.UUID;

public interface MerchantSelfServicePort {
    Merchant getProfile(UUID merchantId);
    MerchantAccount getMyAccount(UUID merchantId);
    List<TransferSummary> getTransfers(UUID merchantId);
}
