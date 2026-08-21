package uz.pulsepay.merchant.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.merchant.domain.model.Merchant;
import uz.pulsepay.merchant.domain.model.MerchantAccount;
import uz.pulsepay.merchant.domain.port.in.MerchantSelfServicePort;
import uz.pulsepay.merchant.domain.port.out.MerchantAccountRepository;
import uz.pulsepay.merchant.domain.port.out.MerchantRepository;
import uz.pulsepay.shared.exception.NotFoundException;
import uz.pulsepay.transfer.domain.model.TransferSummary;
import uz.pulsepay.transfer.domain.port.out.TransferRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class MerchantSelfUseCase implements MerchantSelfServicePort {

    private final MerchantRepository merchantRepository;
    private final MerchantAccountRepository merchantAccountRepository;
    private final TransferRepository transferRepository;

    public MerchantSelfUseCase(MerchantRepository merchantRepository,
                                MerchantAccountRepository merchantAccountRepository,
                                TransferRepository transferRepository) {
        this.merchantRepository        = merchantRepository;
        this.merchantAccountRepository = merchantAccountRepository;
        this.transferRepository        = transferRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Merchant getProfile(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("Merchant not found: " + merchantId));
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantAccount getMyAccount(UUID merchantId) {
        return merchantAccountRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("Merchant account not found for: " + merchantId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferSummary> getTransfers(UUID merchantId) {
        return transferRepository.findSummariesByParticipantId(merchantId);
    }
}
