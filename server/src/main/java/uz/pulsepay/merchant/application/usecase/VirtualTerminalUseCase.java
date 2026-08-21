package uz.pulsepay.merchant.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.fee.domain.port.in.CalculateFeePort;
import uz.pulsepay.identity.domain.model.User;
import uz.pulsepay.identity.domain.port.out.UserRepository;
import uz.pulsepay.merchant.domain.model.Merchant;
import uz.pulsepay.merchant.domain.model.MerchantAccount;
import uz.pulsepay.merchant.domain.port.in.VirtualTerminalPort;
import uz.pulsepay.merchant.domain.port.out.MerchantAccountRepository;
import uz.pulsepay.merchant.domain.port.out.MerchantRepository;
import uz.pulsepay.party.domain.model.Instrument;
import uz.pulsepay.party.domain.model.InstrumentType;
import uz.pulsepay.party.domain.model.PartyType;
import uz.pulsepay.party.domain.port.out.InstrumentRepository;
import uz.pulsepay.routing.domain.model.TransferRoute;
import uz.pulsepay.routing.domain.port.in.ResolveRoutePort;
import uz.pulsepay.shared.domain.CurrencyCode;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;
import uz.pulsepay.transfer.domain.model.ParticipantRole;
import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.model.TransferChannel;
import uz.pulsepay.transfer.domain.model.TransferParticipant;
import uz.pulsepay.transfer.domain.model.TransferStatus;
import uz.pulsepay.transfer.domain.model.TransferStatusHistory;
import uz.pulsepay.transfer.domain.port.out.TransferParticipantRepository;
import uz.pulsepay.transfer.domain.port.out.TransferRepository;
import uz.pulsepay.transfer.domain.port.out.TransferStatusHistoryRepository;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class VirtualTerminalUseCase implements VirtualTerminalPort {

    private static final int C2B_TRANSFER_TYPE_ID = 3;

    private final MerchantRepository merchantRepository;
    private final MerchantAccountRepository merchantAccountRepository;
    private final UserRepository userRepository;
    private final InstrumentRepository instrumentRepository;
    private final CalculateFeePort calculateFeePort;
    private final ResolveRoutePort resolveRoutePort;
    private final TransferRepository transferRepository;
    private final TransferParticipantRepository participantRepository;
    private final TransferStatusHistoryRepository historyRepository;

    public VirtualTerminalUseCase(MerchantRepository merchantRepository,
                                   MerchantAccountRepository merchantAccountRepository,
                                   UserRepository userRepository,
                                   InstrumentRepository instrumentRepository,
                                   CalculateFeePort calculateFeePort,
                                   ResolveRoutePort resolveRoutePort,
                                   TransferRepository transferRepository,
                                   TransferParticipantRepository participantRepository,
                                   TransferStatusHistoryRepository historyRepository) {
        this.merchantRepository        = merchantRepository;
        this.merchantAccountRepository = merchantAccountRepository;
        this.userRepository            = userRepository;
        this.instrumentRepository      = instrumentRepository;
        this.calculateFeePort          = calculateFeePort;
        this.resolveRoutePort          = resolveRoutePort;
        this.transferRepository        = transferRepository;
        this.participantRepository     = participantRepository;
        this.historyRepository         = historyRepository;
    }

    @Override
    @Transactional
    public Transfer charge(UUID merchantId, ChargeCommand cmd) {
        // 1. Validate merchant
        Merchant merchant = merchantRepository.findById(merchantId)
                .filter(Merchant::isActive)
                .orElseThrow(() -> new DomainException("Merchant is not active or KYB not verified"));

        MerchantAccount account = merchantAccountRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("Merchant account not found"));

        // 2. Validate customer
        User customer = userRepository.findByPhoneE164(cmd.customerPhone())
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("Customer not found or inactive: " + cmd.customerPhone()));

        // 3. Validate customer instrument ownership
        Instrument instrument = instrumentRepository.findByIdAndOwnerPartyId(
                        cmd.customerInstrumentId(), customer.id())
                .filter(Instrument::isUsable)
                .orElseThrow(() -> new DomainException("Customer instrument not found or not usable"));

        // 4. Build money objects
        Money amount = Money.ofTiyin(cmd.amountTiyin(), CurrencyCode.UZS);

        // 5. Fee calculation
        Instant now = Instant.now();
        var feeResult = calculateFeePort.calculate(amount, C2B_TRANSFER_TYPE_ID,
                cmd.cardNetwork(), "merchant", amount.currency().name(), now);
        Money feeAmount = feeResult.map(r -> r.fee()).orElse(Money.ofTiyin(0, CurrencyCode.UZS));
        UUID appliedFeeRuleId = feeResult.map(r -> r.appliedRule().id()).orElse(null);

        // 6. Route resolution
        TransferRoute route = resolveRoutePort.resolve(cmd.cardNetwork(), "merchant",
                C2B_TRANSFER_TYPE_ID, amount);

        // 7. Create transfer (skip OTP_PENDING — virtual terminal bypasses OTP)
        String idempotencyKey = UUID.randomUUID().toString();
        Transfer transfer = transferRepository.save(new Transfer(
                UUID.randomUUID(), amount, feeAmount,
                TransferStatus.PROCESSING, idempotencyKey, null,
                appliedFeeRuleId, route.id(), C2B_TRANSFER_TYPE_ID, cmd.purposeCodeId(),
                TransferChannel.API, now, null));

        // 8. Participants
        participantRepository.save(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.SENDER,
                customer.id(), PartyType.PERSON, cmd.customerInstrumentId(),
                instrument.instrumentType(), now));
        participantRepository.save(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.RECIPIENT,
                merchantId, PartyType.MERCHANT, account.id(),
                InstrumentType.MERCHANT_ACCOUNT, now));

        // 9. Status history: PROCESSING entry
        historyRepository.save(new TransferStatusHistory(
                UUID.randomUUID(), transfer.id(), null,
                TransferStatus.PROCESSING, "C2B virtual terminal charge initiated", now));

        // 10. Auto-complete (stub phase — no real gateway call)
        Transfer completed = transferRepository.updateStatus(
                transfer.id(), TransferStatus.COMPLETED, "Auto-completed (stub)");
        historyRepository.save(new TransferStatusHistory(
                UUID.randomUUID(), completed.id(), TransferStatus.PROCESSING,
                TransferStatus.COMPLETED, "Auto-completed (virtual terminal stub)", Instant.now()));

        log.info("C2B charge completed: transferId={}, merchant={}, customer={}, amount={}",
                completed.id(), merchantId, customer.id(), amount);
        return completed;
    }
}
