package uz.pulsepay.fee.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.fee.domain.model.FeeRuleTier;
import uz.pulsepay.fee.domain.model.FeeType;
import uz.pulsepay.fee.domain.port.in.ManageFeeRulePort;
import uz.pulsepay.fee.domain.port.in.ManageFeeRulePort;
import uz.pulsepay.fee.domain.port.out.FeeRuleRepository;
import uz.pulsepay.fee.domain.port.out.FeeRuleTierRepository;
import uz.pulsepay.shared.audit.AuditContext;
import uz.pulsepay.shared.exception.ConflictException;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ManageFeeRuleUseCase implements ManageFeeRulePort {

    private final FeeRuleRepository feeRuleRepository;
    private final FeeRuleTierRepository feeRuleTierRepository;

    public ManageFeeRuleUseCase(FeeRuleRepository feeRuleRepository,
                                 FeeRuleTierRepository feeRuleTierRepository) {
        this.feeRuleRepository     = feeRuleRepository;
        this.feeRuleTierRepository = feeRuleTierRepository;
    }

    @Override
    @Transactional
    public FeeRule createRule(CreateFeeRuleCommand cmd) {
        UUID adminId = requireAdminId();
        validateNoOverlap(null, cmd);

        Instant now = Instant.now();
        FeeRule rule = new FeeRule(
                UUID.randomUUID(), cmd.name(),
                cmd.sourceNetwork(), cmd.destinationNetwork(),
                cmd.minAmount(), cmd.maxAmount(),
                cmd.feeType(), cmd.fixedAmount(), cmd.percentageBps(),
                cmd.minFeeAmount(), cmd.maxFeeAmount(),
                cmd.currencyCode(), cmd.priority(), true,
                cmd.effectiveFrom() != null ? cmd.effectiveFrom() : now,
                cmd.effectiveTo(),
                cmd.transferTypeId(), cmd.feePayer(), cmd.feeRecipient(),
                now, adminId, null);

        FeeRule saved = feeRuleRepository.save(rule);

        // Atomically persist inline tiers for TIERED rules
        if (cmd.tiers() != null) {
            for (ManageFeeRulePort.AddTierCommand tierCmd : cmd.tiers()) {
                feeRuleTierRepository.save(new FeeRuleTier(
                        UUID.randomUUID(), saved.id(),
                        tierCmd.tierMinAmount(), tierCmd.tierMaxAmount(),
                        tierCmd.fixedAmount(), tierCmd.percentageBps()));
            }
        }

        log.info("Fee rule created: id={}, name={}, createdBy={}", saved.id(), saved.name(), adminId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeRule> listAll() {
        return feeRuleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public FeeRule getRule(UUID id) {
        return feeRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fee rule not found: " + id));
    }

    @Override
    @Transactional
    public FeeRule deactivate(UUID id) {
        UUID adminId = requireAdminId();
        FeeRule rule = getRule(id);

        FeeRule updated = new FeeRule(
                rule.id(), rule.name(),
                rule.sourceNetwork(), rule.destinationNetwork(),
                rule.minAmount(), rule.maxAmount(),
                rule.feeType(), rule.fixedAmount(), rule.percentageBps(),
                rule.minFeeAmount(), rule.maxFeeAmount(),
                rule.currencyCode(), rule.priority(), false,
                rule.effectiveFrom(), Instant.now(),
                rule.transferTypeId(), rule.feePayer(), rule.feeRecipient(),
                rule.createdAt(), rule.createdByAdminId(), adminId);

        FeeRule saved = feeRuleRepository.save(updated);
        log.info("Fee rule deactivated: id={}, by adminId={}", id, adminId);
        return saved;
    }

    @Override
    @Transactional
    public FeeRule supersede(UUID id, CreateFeeRuleCommand replacement) {
        UUID adminId = requireAdminId();
        FeeRule old = getRule(id);

        // Deactivate old rule (set effective_to = now)
        Instant now = Instant.now();
        FeeRule deactivated = new FeeRule(
                old.id(), old.name(),
                old.sourceNetwork(), old.destinationNetwork(),
                old.minAmount(), old.maxAmount(),
                old.feeType(), old.fixedAmount(), old.percentageBps(),
                old.minFeeAmount(), old.maxFeeAmount(),
                old.currencyCode(), old.priority(), false,
                old.effectiveFrom(), now,
                old.transferTypeId(), old.feePayer(), old.feeRecipient(),
                old.createdAt(), old.createdByAdminId(), adminId);
        feeRuleRepository.save(deactivated);

        // Create the replacement rule (no overlap check needed since old is deactivated)
        FeeRule newRule = new FeeRule(
                UUID.randomUUID(), replacement.name(),
                replacement.sourceNetwork(), replacement.destinationNetwork(),
                replacement.minAmount(), replacement.maxAmount(),
                replacement.feeType(), replacement.fixedAmount(), replacement.percentageBps(),
                replacement.minFeeAmount(), replacement.maxFeeAmount(),
                replacement.currencyCode(), replacement.priority(), true,
                now, replacement.effectiveTo(),
                replacement.transferTypeId(), replacement.feePayer(), replacement.feeRecipient(),
                now, adminId, null);

        FeeRule saved = feeRuleRepository.save(newRule);
        log.info("Fee rule superseded: oldId={}, newId={}, by adminId={}", id, saved.id(), adminId);
        return saved;
    }

    @Override
    @Transactional
    public FeeRuleTier addTier(UUID ruleId, AddTierCommand cmd) {
        requireAdminId();
        FeeRule rule = getRule(ruleId);

        if (rule.feeType() != FeeType.TIERED) {
            throw new DomainException("Tiers can only be added to rules with fee_type=TIERED");
        }
        assertNoLiveReferences(ruleId);

        FeeRuleTier tier = new FeeRuleTier(
                UUID.randomUUID(), ruleId,
                cmd.tierMinAmount(), cmd.tierMaxAmount(),
                cmd.fixedAmount(), cmd.percentageBps());
        FeeRuleTier saved = feeRuleTierRepository.save(tier);
        log.info("Tier added to fee rule: ruleId={}, tierId={}", ruleId, saved.id());
        return saved;
    }

    @Override
    @Transactional
    public void removeTier(UUID ruleId, UUID tierId) {
        requireAdminId();
        assertNoLiveReferences(ruleId);
        feeRuleTierRepository.deleteById(tierId);
        log.info("Tier removed: ruleId={}, tierId={}", ruleId, tierId);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private UUID requireAdminId() {
        UUID adminId = AuditContext.getAdminId();
        if (adminId == null) {
            throw new DomainException("Admin identity is required but missing from context");
        }
        return adminId;
    }

    private void assertNoLiveReferences(UUID ruleId) {
        long refs = feeRuleRepository.countTransferReferences(ruleId);
        if (refs > 0) {
            throw new DomainException(
                    "Fee rule has %d live transfer reference(s); create a superseding rule instead".formatted(refs));
        }
    }

    /**
     * Validates that no active rule with the same (transferTypeId, sourceNetwork, destinationNetwork,
     * currencyCode, priority) has an overlapping effective period.
     *
     * @param excludeId rule ID to exclude from the check (used by supersede to skip the rule being replaced)
     */
    private void validateNoOverlap(UUID excludeId, CreateFeeRuleCommand cmd) {
        List<FeeRule> candidates = feeRuleRepository.findActiveByScope(
                cmd.transferTypeId(), cmd.sourceNetwork(), cmd.destinationNetwork(),
                cmd.currencyCode(), cmd.priority());

        Instant newFrom = cmd.effectiveFrom() != null ? cmd.effectiveFrom() : Instant.now();
        Instant newTo   = cmd.effectiveTo();

        for (FeeRule existing : candidates) {
            if (excludeId != null && existing.id().equals(excludeId)) continue;
            if (periodsOverlap(existing.effectiveFrom(), existing.effectiveTo(), newFrom, newTo)) {
                throw new ConflictException(
                        "Overlapping fee rule exists: id=%s, priority=%d, scope=(%s/%s/%s/%s)"
                                .formatted(existing.id(), existing.priority(),
                                           cmd.transferTypeId(), cmd.sourceNetwork(),
                                           cmd.destinationNetwork(), cmd.currencyCode()));
            }
        }
    }

    private boolean periodsOverlap(Instant existFrom, Instant existTo,
                                    Instant newFrom, Instant newTo) {
        boolean newCoversExistStart = newTo == null || existFrom.isBefore(newTo);
        boolean existCoversNewStart = existTo == null || existTo.isAfter(newFrom);
        return newCoversExistStart && existCoversNewStart;
    }
}
