package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.fee.FeeRuleEntity;
import uz.pulsepay.domain.fee.FeeRuleTierEntity;
import uz.pulsepay.domain.fee.FeeRule;
import uz.pulsepay.domain.fee.FeeRuleTier;
import uz.pulsepay.domain.fee.FeePayer;
import uz.pulsepay.domain.fee.FeeRecipient;
import uz.pulsepay.domain.fee.FeeType;
import uz.pulsepay.repository.FeeRuleRepository;
import uz.pulsepay.repository.FeeRuleTierRepository;
import uz.pulsepay.domain.shared.AuditContext;
import uz.pulsepay.domain.shared.CurrencyCode;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.domain.shared.ConflictException;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class FeeService {

    // ─── Inner command/result records ─────────────────────────────────────────

    public record FeeResult(Money fee, FeeRule appliedRule) {}

    public record CreateFeeRuleCommand(
            String name,
            String sourceNetwork,
            String destinationNetwork,
            long minAmount,
            Long maxAmount,
            FeeType feeType,
            Long fixedAmount,
            Integer percentageBps,
            Long minFeeAmount,
            Long maxFeeAmount,
            String currencyCode,
            int priority,
            Instant effectiveFrom,
            Instant effectiveTo,
            Integer transferTypeId,
            FeePayer feePayer,
            FeeRecipient feeRecipient,
            List<AddTierCommand> tiers
    ) {}

    public record AddTierCommand(
            long tierMinAmount,
            Long tierMaxAmount,
            Long fixedAmount,
            Integer percentageBps
    ) {}

    private final FeeRuleRepository feeRuleRepository;
    private final FeeRuleTierRepository feeRuleTierRepository;

    public FeeService(FeeRuleRepository feeRuleRepository,
                      FeeRuleTierRepository feeRuleTierRepository) {
        this.feeRuleRepository     = feeRuleRepository;
        this.feeRuleTierRepository = feeRuleTierRepository;
    }

    // ─── CalculateFeePort ─────────────────────────────────────────────────────

    public Optional<FeeResult> calculate(Money amount, int transferTypeId,
                                         String sourceNetwork, String destNetwork,
                                         String currencyCode, Instant occurringAt) {
        List<FeeRule> rules = feeRuleRepository.findApplicableRules(
                transferTypeId, sourceNetwork, destNetwork,
                amount.amount(), currencyCode, occurringAt)
                .stream().map(FeeRuleEntity::toDomain).toList();

        Optional<FeeRule> winner = rules.stream()
                .sorted(Comparator.comparingInt(FeeRule::priority)
                        .thenComparingInt(r -> -specificityScore(r))
                        .thenComparing(FeeRule::createdAt, Comparator.reverseOrder()))
                .findFirst();

        if (winner.isEmpty()) {
            log.warn("No fee rule matched: transferTypeId={}, sourceNetwork={}, destNetwork={}, " +
                     "amount={}, currencyCode={}", transferTypeId, sourceNetwork, destNetwork,
                     amount.amount(), currencyCode);
            return Optional.empty();
        }

        FeeRule rule = winner.get();
        long fee = computeFee(rule, amount.amount());
        return Optional.of(new FeeResult(Money.ofTiyin(fee, CurrencyCode.UZS), rule));
    }

    private int specificityScore(FeeRule rule) {
        return (rule.transferTypeId() != null ? 4 : 0)
             + (rule.sourceNetwork()  != null ? 2 : 0)
             + (rule.destinationNetwork() != null ? 1 : 0);
    }

    private long computeFee(FeeRule rule, long amount) {
        return switch (rule.feeType()) {
            case FIXED -> rule.fixedAmount() != null ? rule.fixedAmount() : 0L;
            case PERCENTAGE -> computePercentage(rule, amount);
            case TIERED -> computeTiered(rule, amount);
            case PERCENTAGE_PLUS_FLAT -> computePercentagePlusFlat(rule, amount);
        };
    }

    private long computePercentage(FeeRule rule, long amount) {
        long raw = (amount * rule.percentageBps() + 5_000L) / 10_000L;
        long floored = rule.minFeeAmount() != null ? Math.max(raw, rule.minFeeAmount()) : raw;
        return rule.maxFeeAmount() != null ? Math.min(floored, rule.maxFeeAmount()) : floored;
    }

    /**
     * Ports PHP OperationParam.calculateFeeAmount():
     *   fee = round(amount × percent_com / 100) + flat_com
     * Java equivalent (percent_com × 100 = percentageBps):
     *   fee = (amount × bps + 5_000) / 10_000 + fixedAmount
     * Min/max caps are applied to the combined total, matching PHP semantics.
     */
    private long computePercentagePlusFlat(FeeRule rule, long amount) {
        long pct  = (amount * rule.percentageBps() + 5_000L) / 10_000L;
        long flat = rule.fixedAmount() != null ? rule.fixedAmount() : 0L;
        long raw  = pct + flat;
        long floored = rule.minFeeAmount() != null ? Math.max(raw, rule.minFeeAmount()) : raw;
        return rule.maxFeeAmount() != null ? Math.min(floored, rule.maxFeeAmount()) : floored;
    }

    private long computeTiered(FeeRule rule, long amount) {
        List<FeeRuleTier> tiers = feeRuleTierRepository.findByFeeRuleId(rule.id())
                .stream().map(FeeRuleTierEntity::toDomain).toList();
        FeeRuleTier tier = tiers.stream()
                .filter(t -> t.tierMinAmount() <= amount
                          && (t.tierMaxAmount() == null || t.tierMaxAmount() >= amount))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No tier matches amount {} for fee rule {}", amount, rule.id());
                    return new DomainException(
                            "No tier matches amount %d for fee rule %s".formatted(amount, rule.id()));
                });

        if (tier.fixedAmount() != null) {
            return tier.fixedAmount();
        }

        // percentage-based tier — apply parent rule's floor/cap
        long raw = (amount * tier.percentageBps() + 5_000L) / 10_000L;
        long floored = rule.minFeeAmount() != null ? Math.max(raw, rule.minFeeAmount()) : raw;
        return rule.maxFeeAmount() != null ? Math.min(floored, rule.maxFeeAmount()) : floored;
    }

    // ─── ManageFeeRulePort ────────────────────────────────────────────────────

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

        FeeRule saved = feeRuleRepository.save(FeeRuleEntity.fromDomain(rule)).toDomain();

        // Atomically persist inline tiers for TIERED rules
        if (cmd.tiers() != null) {
            for (AddTierCommand tierCmd : cmd.tiers()) {
                FeeRuleTier tier = new FeeRuleTier(
                        UUID.randomUUID(), saved.id(),
                        tierCmd.tierMinAmount(), tierCmd.tierMaxAmount(),
                        tierCmd.fixedAmount(), tierCmd.percentageBps());
                feeRuleTierRepository.save(FeeRuleTierEntity.fromDomain(tier));
            }
        }

        log.info("Fee rule created: id={}, name={}, createdBy={}", saved.id(), saved.name(), adminId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<FeeRule> listAll() {
        return feeRuleRepository.findAll().stream().map(FeeRuleEntity::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public FeeRule getRule(UUID id) {
        return feeRuleRepository.findById(id)
                .map(FeeRuleEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Fee rule not found: " + id));
    }

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

        FeeRule saved = feeRuleRepository.save(FeeRuleEntity.fromDomain(updated)).toDomain();
        log.info("Fee rule deactivated: id={}, by adminId={}", id, adminId);
        return saved;
    }

    @Transactional
    public FeeRule activate(UUID id) {
        UUID adminId = requireAdminId();
        FeeRule rule = getRule(id);

        // Run overlap check using the rule's own scope, excluding itself
        CreateFeeRuleCommand checkCmd = new CreateFeeRuleCommand(
                rule.name(), rule.sourceNetwork(), rule.destinationNetwork(),
                rule.minAmount(), rule.maxAmount(),
                rule.feeType(), rule.fixedAmount(), rule.percentageBps(),
                rule.minFeeAmount(), rule.maxFeeAmount(),
                rule.currencyCode(), rule.priority(),
                rule.effectiveFrom(), null,
                rule.transferTypeId(), rule.feePayer(), rule.feeRecipient(), null);
        validateNoOverlap(rule.id(), checkCmd);

        FeeRule updated = new FeeRule(
                rule.id(), rule.name(),
                rule.sourceNetwork(), rule.destinationNetwork(),
                rule.minAmount(), rule.maxAmount(),
                rule.feeType(), rule.fixedAmount(), rule.percentageBps(),
                rule.minFeeAmount(), rule.maxFeeAmount(),
                rule.currencyCode(), rule.priority(), true,
                rule.effectiveFrom(), null,
                rule.transferTypeId(), rule.feePayer(), rule.feeRecipient(),
                rule.createdAt(), rule.createdByAdminId(), adminId);

        FeeRule saved = feeRuleRepository.save(FeeRuleEntity.fromDomain(updated)).toDomain();
        log.info("Fee rule activated: id={}, by adminId={}", id, adminId);
        return saved;
    }

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
        feeRuleRepository.save(FeeRuleEntity.fromDomain(deactivated));

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

        FeeRule saved = feeRuleRepository.save(FeeRuleEntity.fromDomain(newRule)).toDomain();
        log.info("Fee rule superseded: oldId={}, newId={}, by adminId={}", id, saved.id(), adminId);
        return saved;
    }

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
        FeeRuleTier saved = feeRuleTierRepository.save(FeeRuleTierEntity.fromDomain(tier)).toDomain();
        log.info("Tier added to fee rule: ruleId={}, tierId={}", ruleId, saved.id());
        return saved;
    }

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

    public List<FeeRuleTier> getTiersForRule(UUID ruleId) {
        return feeRuleTierRepository.findByFeeRuleId(ruleId)
                .stream().map(FeeRuleTierEntity::toDomain).toList();
    }

    private void validateNoOverlap(UUID excludeId, CreateFeeRuleCommand cmd) {
        List<FeeRule> candidates = feeRuleRepository.findActiveByScope(
                cmd.transferTypeId(), cmd.sourceNetwork(), cmd.destinationNetwork(),
                cmd.currencyCode(), cmd.priority())
                .stream().map(FeeRuleEntity::toDomain).toList();

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
