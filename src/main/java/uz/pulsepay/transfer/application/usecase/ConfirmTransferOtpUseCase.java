package uz.pulsepay.transfer.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.compliance.domain.port.in.EvaluateComplianceFlagsPort;
import uz.pulsepay.identity.domain.model.OtpPurpose;
import uz.pulsepay.identity.domain.service.OtpDomainService;
import uz.pulsepay.ledger.domain.port.in.PostLedgerEntriesPort;
import uz.pulsepay.limit.domain.port.in.IncrementLimitUsagePort;
import uz.pulsepay.network.domain.port.in.ExecuteCardTransferPort;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;
import uz.pulsepay.transfer.domain.model.*;
import uz.pulsepay.transfer.domain.port.in.ConfirmTransferOtpPort;
import uz.pulsepay.transfer.domain.port.out.TransferParticipantRepository;
import uz.pulsepay.transfer.domain.port.out.TransferRepository;
import uz.pulsepay.transfer.domain.port.out.TransferStatusHistoryRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * OTP confirmation → network execution → ledger posting → compliance evaluation.
 * All within ONE @Transactional boundary (Risk #1).
 */
@Service
public class ConfirmTransferOtpUseCase implements ConfirmTransferOtpPort {

    private final OtpDomainService otpDomainService;
    private final TransferRepository transferRepository;
    private final TransferParticipantRepository participantRepository;
    private final TransferStatusHistoryRepository historyRepository;
    private final ExecuteCardTransferPort executeCardTransferPort;
    private final PostLedgerEntriesPort postLedgerEntriesPort;
    private final IncrementLimitUsagePort incrementLimitUsagePort;
    private final EvaluateComplianceFlagsPort evaluateComplianceFlagsPort;

    public ConfirmTransferOtpUseCase(
            OtpDomainService otpDomainService,
            TransferRepository transferRepository,
            TransferParticipantRepository participantRepository,
            TransferStatusHistoryRepository historyRepository,
            ExecuteCardTransferPort executeCardTransferPort,
            PostLedgerEntriesPort postLedgerEntriesPort,
            IncrementLimitUsagePort incrementLimitUsagePort,
            EvaluateComplianceFlagsPort evaluateComplianceFlagsPort) {
        this.otpDomainService = otpDomainService;
        this.transferRepository = transferRepository;
        this.participantRepository = participantRepository;
        this.historyRepository = historyRepository;
        this.executeCardTransferPort = executeCardTransferPort;
        this.postLedgerEntriesPort = postLedgerEntriesPort;
        this.incrementLimitUsagePort = incrementLimitUsagePort;
        this.evaluateComplianceFlagsPort = evaluateComplianceFlagsPort;
    }

    @Override
    @Transactional
    public Transfer confirmOtp(UUID transferId, UUID userId, String otpCode) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new NotFoundException("Transfer not found"));

        // State machine guard: only OTP_PENDING → PROCESSING is legal here
        TransferStateMachine.assertTransition(transfer.status(), TransferStatus.PROCESSING);

        // Verify OTP
        otpDomainService.verifyCode(userId, otpCode, OtpPurpose.TRANSFER);

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

        // Execute network transfer
        String routeCode = transfer.appliedRouteId() != null ? transfer.appliedRouteId().toString() : "default";
        executeCardTransferPort.execute(
                transferId,
                sender.instrumentId(),
                recipient.instrumentId(),
                transfer.amount(),
                routeCode);

        // Post ledger entries (Risk #1: same transaction)
        postLedgerEntriesPort.postTransferEntries(
                transferId,
                transfer.amount(),
                transfer.feeAmount(),
                sender.instrumentType().name().toLowerCase(),
                recipient.instrumentType().name().toLowerCase());

        // Increment limit counters
        incrementLimitUsagePort.increment(userId, transfer.amount(), transfer.transferTypeId());

        // Compliance evaluation
        evaluateComplianceFlagsPort.evaluate(transferId, sender.partyId(), transfer.amount());

        // Status → COMPLETED
        transfer = transferRepository.updateStatus(transferId, TransferStatus.COMPLETED, "Network transfer successful");
        historyRepository.save(new TransferStatusHistory(
                UUID.randomUUID(), transferId,
                TransferStatus.PROCESSING, TransferStatus.COMPLETED, "Network transfer successful", Instant.now()));

        return transfer;
    }
}
