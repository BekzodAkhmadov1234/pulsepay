package uz.pulsepay.transfer.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.fee.domain.port.in.CalculateFeePort;
import uz.pulsepay.identity.domain.model.User;
import uz.pulsepay.identity.domain.port.out.UserRepository;
import uz.pulsepay.limit.domain.port.in.CheckLimitPort;
import uz.pulsepay.party.domain.model.Instrument;
import uz.pulsepay.party.domain.model.PartyType;
import uz.pulsepay.reference.domain.model.TransferType;
import uz.pulsepay.reference.domain.port.out.TransferTypeRepository;
import uz.pulsepay.routing.domain.model.TransferRoute;
import uz.pulsepay.routing.domain.port.in.ResolveRoutePort;
import uz.pulsepay.shared.domain.CurrencyCode;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;
import uz.pulsepay.shared.idempotency.IdempotencyService;
import uz.pulsepay.transfer.domain.model.*;
import uz.pulsepay.transfer.domain.port.in.InitiateTransferPort;
import uz.pulsepay.transfer.domain.port.out.TransferParticipantRepository;
import uz.pulsepay.transfer.domain.port.out.TransferRepository;
import uz.pulsepay.transfer.domain.port.out.TransferStatusHistoryRepository;
import uz.pulsepay.transfer.domain.service.TransferOrchestrationService;
import uz.pulsepay.transfer.domain.service.TransferParticipantService;

import java.time.Instant;
import java.util.UUID;

/**
 * Core transfer initiation use case.
 *
 * Sequence (all within ONE @Transactional boundary — Risk #1):
 *   1. Idempotency claim (insert-first — Risk #6)
 *   2. Validate users and instruments (composite FK check — Risk #3)
 *   3. Channel + party-type invariant checks (Risk #10, #4)
 *   4. Limit check
 *   5. Fee calculation
 *   6. Route resolution
 *   7. Persist transfer + participants + status history
 */
@Slf4j
@Service
public class InitiateTransferUseCase implements InitiateTransferPort {

    private final IdempotencyService idempotencyService;
    private final UserRepository userRepository;
    private final TransferParticipantService participantService;
    private final TransferOrchestrationService orchestrationService;
    private final TransferTypeRepository transferTypeRepository;
    private final CheckLimitPort checkLimitPort;
    private final CalculateFeePort calculateFeePort;
    private final ResolveRoutePort resolveRoutePort;
    private final TransferRepository transferRepository;
    private final TransferParticipantRepository participantRepository;
    private final TransferStatusHistoryRepository historyRepository;

    public InitiateTransferUseCase(
            IdempotencyService idempotencyService,
            UserRepository userRepository,
            TransferParticipantService participantService,
            TransferOrchestrationService orchestrationService,
            TransferTypeRepository transferTypeRepository,
            CheckLimitPort checkLimitPort,
            CalculateFeePort calculateFeePort,
            ResolveRoutePort resolveRoutePort,
            TransferRepository transferRepository,
            TransferParticipantRepository participantRepository,
            TransferStatusHistoryRepository historyRepository) {
        this.idempotencyService = idempotencyService;
        this.userRepository = userRepository;
        this.participantService = participantService;
        this.orchestrationService = orchestrationService;
        this.transferTypeRepository = transferTypeRepository;
        this.checkLimitPort = checkLimitPort;
        this.calculateFeePort = calculateFeePort;
        this.resolveRoutePort = resolveRoutePort;
        this.transferRepository = transferRepository;
        this.participantRepository = participantRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    @Transactional
    public Transfer initiate(UUID senderId, UUID senderInstrumentId, String senderCardNetwork,
                             UUID recipientId, UUID recipientInstrumentId, String recipientCardNetwork,
                             Money amount, int transferTypeId, Integer purposeCodeId,
                             TransferChannel channel, String idempotencyKey) {

        log.info("Initiating transfer: sender={}, amount={}, channel={}, idempotencyKey={}",
                senderId, amount, channel, idempotencyKey);

        // 1. Idempotency guard (Risk #6: insert-first)
        idempotencyService.claimKey(idempotencyKey, senderId, idempotencyKey);
        log.debug("Idempotency key claimed: {}", idempotencyKey);

        // 2. Validate sender
        User sender = userRepository.findById(senderId)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("Sender not found or inactive"));

        // 3. Validate instrument ownership (Risk #3)
        Instrument senderInstrument = participantService.validateInstrumentOwnership(senderInstrumentId, senderId);
        Instrument recipientInstrument = participantService.validateInstrumentOwnership(recipientInstrumentId, recipientId);

        // 4. Transfer type
        TransferType transferType = transferTypeRepository.findById(transferTypeId)
                .filter(TransferType::isActive)
                .orElseThrow(() -> new DomainException("Transfer type not available"));

        // 5. Channel invariant (Risk #10)
        orchestrationService.validateChannel(channel, transferType);

        // 6. Party-type invariant (Risk #4)
        orchestrationService.validatePartyTypes(
                PartyType.PERSON.name().toLowerCase(),
                PartyType.PERSON.name().toLowerCase(),
                transferType);

        // 7. Limit check
        checkLimitPort.checkLimits(senderId, sender.kycLevel(), amount, transferTypeId);
        log.debug("Limit check passed: sender={}, amount={}", senderId, amount);

        // 8. Fee calculation (senderCardNetwork / recipientCardNetwork are "uzcard" or "humo")
        String srcNetwork = senderCardNetwork;
        String dstNetwork = recipientCardNetwork;
        var feeResult = calculateFeePort.calculate(amount, transferTypeId, srcNetwork, dstNetwork);
        Money feeAmount = feeResult.map(r -> r.fee()).orElse(Money.ofTiyin(0, CurrencyCode.UZS));
        UUID appliedFeeRuleId = feeResult.map(r -> r.appliedRule().id()).orElse(null);
        log.debug("Fee calculated: fee={}, appliedRuleId={}, route={}->{}", feeAmount, appliedFeeRuleId, srcNetwork, dstNetwork);

        // 9. Route resolution
        TransferRoute route = resolveRoutePort.resolve(srcNetwork, dstNetwork, transferTypeId, amount);
        log.debug("Route resolved: id={}", route.id());

        // 10. Persist transfer
        Transfer transfer = transferRepository.save(new Transfer(
                UUID.randomUUID(), amount, feeAmount,
                TransferStatus.OTP_PENDING, idempotencyKey, null,
                appliedFeeRuleId, route.id(), transferTypeId, purposeCodeId,
                channel, Instant.now(), null));

        // 11. Persist participants
        participantRepository.save(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.SENDER,
                senderId, PartyType.PERSON, senderInstrumentId,
                senderInstrument.instrumentType(), Instant.now()));
        participantRepository.save(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.RECIPIENT,
                recipientId, PartyType.PERSON, recipientInstrumentId,
                recipientInstrument.instrumentType(), Instant.now()));

        // 12. Status history
        historyRepository.save(new TransferStatusHistory(
                UUID.randomUUID(), transfer.id(), null,
                TransferStatus.OTP_PENDING, "Transfer initiated", Instant.now()));

        log.info("Transfer created: id={}, status=OTP_PENDING, amount={}, fee={}", transfer.id(), transfer.amount(), feeAmount);
        return transfer;
    }
}
