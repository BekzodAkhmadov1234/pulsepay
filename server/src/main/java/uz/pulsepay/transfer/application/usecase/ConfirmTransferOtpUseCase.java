package uz.pulsepay.transfer.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.compliance.domain.port.in.EvaluateComplianceFlagsPort;
import uz.pulsepay.fee.domain.model.FeePayer;
import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.fee.domain.port.out.FeeRuleRepository;
import uz.pulsepay.ledger.domain.port.in.PostLedgerEntriesPort;
import uz.pulsepay.limit.domain.port.in.IncrementLimitUsagePort;
import uz.pulsepay.network.domain.port.in.ExecuteCardTransferPort;
import uz.pulsepay.routing.domain.port.in.FindRoutePort;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;
import uz.pulsepay.transfer.domain.model.*;
import uz.pulsepay.transfer.domain.port.in.ConfirmTransferOtpPort;
import uz.pulsepay.transfer.domain.port.out.TransferOtpPort;
import uz.pulsepay.transfer.domain.port.out.TransferParticipantRepository;
import uz.pulsepay.transfer.domain.port.out.TransferRepository;
import uz.pulsepay.transfer.domain.port.out.TransferStatusHistoryRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * OTP confirmation → network execution → ledger posting → compliance evaluation.
 * All within ONE @Transactional boundary (Risk #1).
 */
@Slf4j
@Service
public class ConfirmTransferOtpUseCase implements ConfirmTransferOtpPort {

    private final TransferRepository transferRepository;
    private final TransferParticipantRepository participantRepository;
    private final TransferStatusHistoryRepository historyRepository;
    private final ExecuteCardTransferPort executeCardTransferPort;
    private final PostLedgerEntriesPort postLedgerEntriesPort;
    private final IncrementLimitUsagePort incrementLimitUsagePort;
    private final EvaluateComplianceFlagsPort evaluateComplianceFlagsPort;
    private final FeeRuleRepository feeRuleRepository;
    private final TransferOtpPort transferOtpPort;
    private final FindRoutePort findRoutePort;

    public ConfirmTransferOtpUseCase(
            TransferRepository transferRepository,
            TransferParticipantRepository participantRepository,
            TransferStatusHistoryRepository historyRepository,
            ExecuteCardTransferPort executeCardTransferPort,
            PostLedgerEntriesPort postLedgerEntriesPort,
            IncrementLimitUsagePort incrementLimitUsagePort,
            EvaluateComplianceFlagsPort evaluateComplianceFlagsPort,
            FeeRuleRepository feeRuleRepository,
            TransferOtpPort transferOtpPort,
            FindRoutePort findRoutePort) {
        this.transferRepository = transferRepository;
        this.participantRepository = participantRepository;
        this.historyRepository = historyRepository;
        this.executeCardTransferPort = executeCardTransferPort;
        this.postLedgerEntriesPort = postLedgerEntriesPort;
        this.incrementLimitUsagePort = incrementLimitUsagePort;
        this.evaluateComplianceFlagsPort = evaluateComplianceFlagsPort;
        this.feeRuleRepository = feeRuleRepository;
        this.transferOtpPort = transferOtpPort;
        this.findRoutePort = findRoutePort;
    }

    @Override
    @Transactional
    public Transfer confirmOtp(UUID transferId, UUID userId, String otpCode) {
        log.info("OTP confirmation requested: transferId={}, userId={}", transferId, userId);

        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new NotFoundException("Transfer not found"));

        // State machine guard: only OTP_PENDING → PROCESSING is legal here
        TransferStateMachine.assertTransition(transfer.status(), TransferStatus.PROCESSING);

        // Verify OTP: enforces 59s expiry, 3-attempt cap, 15-min lockout (REG-03)
        transferOtpPort.verify(userId, otpCode, transferId);

        // Status → PROCESSING
        transfer = transferRepository.updateStatus(transferId, TransferStatus.PROCESSING, "OTP confirmed");
        historyRepository.save(new TransferStatusHistory(
                UUID.randomUUID(), transferId,
                TransferStatus.OTP_PENDING, TransferStatus.PROCESSING, "OTP confirmed", Instant.now()));

        // Get participants
        TransferParticipant sender = participantRepository
                .findByTransferIdAndRole(transferId, ParticipantRole.SENDER)
                .orElseThrow(() -> new DomainException("Sender participant not found"));
        TransferParticipant recipient = participantRepository
                .findByTransferIdAndRole(transferId, ParticipantRole.RECIPIENT)
                .orElseThrow(() -> new DomainException("Recipient participant not found"));

        // Reload fee rule to determine fee_payer and fee_recipient (needed for debit routing)
        FeeRule appliedRule = transfer.appliedFeeRuleId() != null
                ? feeRuleRepository.findById(transfer.appliedFeeRuleId()).orElse(null)
                : null;

        FeePayer feePayer = appliedRule != null ? appliedRule.feePayer() : FeePayer.SENDER;
        String feeRecipient = appliedRule != null ? appliedRule.feeRecipient().name() : "PLATFORM";

        // Compute debit (sender) and credit (recipient) amounts based on fee_payer
        Money debitAmount;
        if (feePayer == FeePayer.SENDER) {
            debitAmount = transfer.amount().add(transfer.feeAmount());
        } else {
            log.warn("Non-sender fee_payer '{}' not yet fully routed for transfer {}; sender debited principal only",
                     feePayer, transferId);
            debitAmount = transfer.amount();
        }
        Money creditAmount = transfer.amount(); // recipient always receives principal in Stage 1

        // Resolve intermediary/processor from the applied route
        String processorName = transfer.appliedRouteId() != null
                ? findRoutePort.findById(transfer.appliedRouteId())
                        .map(r -> r.processorName())
                        .orElse("uzcard")
                : "uzcard";
        log.debug("Executing network transfer via intermediary: transferId={}, processor={}, debit={}, credit={}",
                  transferId, processorName, debitAmount, creditAmount);
        executeCardTransferPort.execute(
                transferId,
                sender.instrumentId(),
                recipient.instrumentId(),
                debitAmount,
                creditAmount,
                processorName);
        log.debug("Network transfer executed: transferId={}", transferId);

        // Post ledger entries (Risk #1: same transaction)
        postLedgerEntriesPort.postTransferEntries(
                transferId,
                transfer.amount(),
                transfer.feeAmount(),
                sender.instrumentType().name().toLowerCase(),
                recipient.instrumentType().name().toLowerCase(),
                feeRecipient);
        log.debug("Ledger entries posted: transferId={}, amount={}, fee={}", transferId, transfer.amount(), transfer.feeAmount());

        // Increment limit counters
        incrementLimitUsagePort.increment(userId, transfer.amount(), transfer.transferTypeId());

        // Compliance evaluation
        evaluateComplianceFlagsPort.evaluate(transferId, sender.partyId(), transfer.amount());

        // Status → COMPLETED
        transfer = transferRepository.updateStatus(transferId, TransferStatus.COMPLETED, "Network transfer successful");
        historyRepository.save(new TransferStatusHistory(
                UUID.randomUUID(), transferId,
                TransferStatus.PROCESSING, TransferStatus.COMPLETED, "Network transfer successful", Instant.now()));

        log.info("Transfer completed: id={}, amount={}", transferId, transfer.amount());
        return transfer;
    }
}
