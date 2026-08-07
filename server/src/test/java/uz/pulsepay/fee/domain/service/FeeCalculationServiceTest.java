package uz.pulsepay.fee.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.pulsepay.fee.domain.model.FeePayer;
import uz.pulsepay.fee.domain.model.FeeRecipient;
import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.fee.domain.model.FeeRuleTier;
import uz.pulsepay.fee.domain.model.FeeType;
import uz.pulsepay.fee.domain.port.in.CalculateFeePort.FeeResult;
import uz.pulsepay.fee.domain.port.out.FeeRuleRepository;
import uz.pulsepay.fee.domain.port.out.FeeRuleTierRepository;
import uz.pulsepay.shared.domain.CurrencyCode;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeeCalculationServiceTest {

    private FeeRuleRepository feeRuleRepository;
    private FeeRuleTierRepository feeRuleTierRepository;
    private FeeCalculationService service;

    private static final Instant EPOCH = Instant.EPOCH;
    private static final Instant NOW   = Instant.parse("2026-01-01T00:00:00Z");
    private static final String UZS    = "UZS";
    private static final int    P2P    = 1;

    @BeforeEach
    void setUp() {
        feeRuleRepository     = mock(FeeRuleRepository.class);
        feeRuleTierRepository = mock(FeeRuleTierRepository.class);
        service = new FeeCalculationService(feeRuleRepository, feeRuleTierRepository);
    }

    // ─── Helper factories ────────────────────────────────────────────────────

    /** Build a fee rule with all the essential fields; nulls for unused ones. */
    private static FeeRule rule(UUID id, FeeType type, Long fixed, Integer bps,
                                 Long minFee, Long maxFee, int priority,
                                 String src, String dst, Integer transferTypeId,
                                 FeePayer payer) {
        return new FeeRule(id, "test-rule", src, dst,
                0L, null, type, fixed, bps, minFee, maxFee,
                UZS, priority, true,
                EPOCH, null, transferTypeId,
                payer, FeeRecipient.PLATFORM,
                NOW, null, null);
    }

    private static FeeRule senderRule(UUID id, FeeType type, Long fixed, Integer bps,
                                       Long minFee, Long maxFee, int priority,
                                       String src, String dst, Integer typeId) {
        return rule(id, type, fixed, bps, minFee, maxFee, priority, src, dst, typeId, FeePayer.SENDER);
    }

    private static FeeRuleTier tier(UUID ruleId, long min, Long max, Long fixed, Integer bps) {
        return new FeeRuleTier(UUID.randomUUID(), ruleId, min, max, fixed, bps);
    }

    /** Convenience: stub repo to return a single rule, stub tiers as empty, then call service. */
    private Optional<FeeResult> calculate(FeeRule singleRule, long amountTiyin, String src, String dst) {
        when(feeRuleRepository.findApplicableRules(anyInt(), any(), any(), anyLong(), any(), any()))
                .thenReturn(List.of(singleRule));
        when(feeRuleTierRepository.findByFeeRuleId(any())).thenReturn(List.of());
        return service.calculate(Money.ofTiyin(amountTiyin, CurrencyCode.UZS), P2P, src, dst, UZS, NOW);
    }

    // ─── FIXED fee ──────────────────────────────────────────────────────────

    @Test
    void fixed_fee_returns_fixed_amount() {
        UUID id = UUID.randomUUID();
        FeeRule rule = senderRule(id, FeeType.FIXED, 10_000L, null, null, null, 100, null, null, P2P);

        Optional<FeeResult> result = calculate(rule, 1_000_000L, "uzcard", "humo");

        assertThat(result).isPresent();
        assertThat(result.get().fee().amount()).isEqualTo(10_000L);
        assertThat(result.get().appliedRule().id()).isEqualTo(id);
    }

    // ─── PERCENTAGE fee ──────────────────────────────────────────────────────

    @Test
    void percentage_fee_no_floor_no_cap() {
        // 100_000_000 tiyin × 100 bps → (10_000_000_000 + 5_000) / 10_000 = 1_000_000
        FeeRule rule = senderRule(UUID.randomUUID(), FeeType.PERCENTAGE, null, 100, null, null, 100, null, null, P2P);

        Optional<FeeResult> result = calculate(rule, 100_000_000L, "uzcard", "humo");

        assertThat(result.get().fee().amount()).isEqualTo(1_000_000L);
    }

    @Test
    void percentage_fee_floor_applied() {
        // 50_000 tiyin × 65 bps → raw = (3_250_000 + 5_000) / 10_000 = 325 < floor=500 → 500
        FeeRule rule = senderRule(UUID.randomUUID(), FeeType.PERCENTAGE, null, 65, 500L, null, 100, null, null, P2P);

        Optional<FeeResult> result = calculate(rule, 50_000L, "uzcard", "humo");

        assertThat(result.get().fee().amount()).isEqualTo(500L);
    }

    @Test
    void percentage_fee_cap_applied() {
        // 200_000_000 tiyin × 150 bps → raw = 3_000_000 > cap=150_000 → 150_000
        FeeRule rule = senderRule(UUID.randomUUID(), FeeType.PERCENTAGE, null, 150, null, 150_000L, 100, null, null, P2P);

        Optional<FeeResult> result = calculate(rule, 200_000_000L, "uzcard", "humo");

        assertThat(result.get().fee().amount()).isEqualTo(150_000L);
    }

    @Test
    void percentage_fee_rounding_half_up() {
        // 100_003 tiyin × 100 bps: raw = 10_000_300; (10_000_300 + 5_000) / 10_000 = 10_005_300 / 10_000 = 1000
        FeeRule rule = senderRule(UUID.randomUUID(), FeeType.PERCENTAGE, null, 100, null, null, 100, null, null, P2P);

        Optional<FeeResult> result = calculate(rule, 100_003L, "uzcard", "humo");

        assertThat(result.get().fee().amount()).isEqualTo(1_000L);
    }

    // ─── TIERED fee ──────────────────────────────────────────────────────────

    /**
     * Sets up a TIERED rule with three tiers:
     *   Tier 1: 0 – 999_999       → fixed 500 tiyin
     *   Tier 2: 1_000_000 – 9_999_999 → 50 bps
     *   Tier 3: 10_000_000+       → 100 bps  (rule maxFeeAmount = 200_000)
     */
    private UUID setupTieredRule() {
        UUID ruleId = UUID.randomUUID();
        FeeRule rule = senderRule(ruleId, FeeType.TIERED, null, null, null, 200_000L, 50, "humo", "humo", P2P);
        when(feeRuleRepository.findApplicableRules(anyInt(), any(), any(), anyLong(), any(), any()))
                .thenReturn(List.of(rule));
        when(feeRuleTierRepository.findByFeeRuleId(ruleId)).thenReturn(List.of(
                tier(ruleId,          0L,   999_999L, 500L, null),
                tier(ruleId,  1_000_000L, 9_999_999L, null,   50),
                tier(ruleId, 10_000_000L,        null, null,  100)
        ));
        return ruleId;
    }

    @Test
    void tiered_fee_first_tier_fixed() {
        setupTieredRule();
        Money money = Money.ofTiyin(500_000L, CurrencyCode.UZS);

        Optional<FeeResult> result = service.calculate(money, P2P, "humo", "humo", UZS, NOW);

        assertThat(result.get().fee().amount()).isEqualTo(500L);
    }

    @Test
    void tiered_fee_second_tier_percentage() {
        // 5_000_000 × 50 bps → (250_000_000 + 5_000) / 10_000 = 25_000; cap=200_000 → 25_000
        setupTieredRule();
        Money money = Money.ofTiyin(5_000_000L, CurrencyCode.UZS);

        Optional<FeeResult> result = service.calculate(money, P2P, "humo", "humo", UZS, NOW);

        assertThat(result.get().fee().amount()).isEqualTo(25_000L);
    }

    @Test
    void tiered_fee_third_tier_with_cap() {
        // 20_000_000 × 100 bps → raw = 200_000; cap = 200_000 → 200_000
        setupTieredRule();
        Money money = Money.ofTiyin(20_000_000L, CurrencyCode.UZS);

        Optional<FeeResult> result = service.calculate(money, P2P, "humo", "humo", UZS, NOW);

        assertThat(result.get().fee().amount()).isEqualTo(200_000L);
    }

    @Test
    void tiered_fee_boundary_exact_1000000() {
        // 1_000_000 falls in tier 2 (tierMinAmount=1_000_000 is inclusive)
        // (1_000_000 × 50 + 5_000) / 10_000 = 50_005_000 / 10_000 = 5_000
        setupTieredRule();
        Money money = Money.ofTiyin(1_000_000L, CurrencyCode.UZS);

        Optional<FeeResult> result = service.calculate(money, P2P, "humo", "humo", UZS, NOW);

        assertThat(result.get().fee().amount()).isEqualTo(5_000L);
    }

    @Test
    void tiered_fee_no_matching_tier_throws() {
        UUID ruleId = UUID.randomUUID();
        FeeRule rule = senderRule(ruleId, FeeType.TIERED, null, null, null, null, 50, "humo", "humo", P2P);
        when(feeRuleRepository.findApplicableRules(anyInt(), any(), any(), anyLong(), any(), any()))
                .thenReturn(List.of(rule));
        // Only a tier starting at 1_000_000 — amount=500 matches nothing
        when(feeRuleTierRepository.findByFeeRuleId(ruleId)).thenReturn(List.of(
                tier(ruleId, 1_000_000L, null, null, 50)
        ));

        Money money = Money.ofTiyin(500L, CurrencyCode.UZS);
        assertThatThrownBy(() -> service.calculate(money, P2P, "humo", "humo", UZS, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("No tier matches amount");
    }

    // ─── Rule selection / tie-breaking ──────────────────────────────────────

    @Test
    void same_network_rule_wins_over_catchall() {
        // priority=50 (uzcard→uzcard) must beat priority=200 (null→null)
        UUID specificId = UUID.randomUUID();
        UUID catchallId = UUID.randomUUID();
        FeeRule specific = senderRule(specificId, FeeType.FIXED, 100L, null, null, null,  50, "uzcard", "uzcard", P2P);
        FeeRule catchall = senderRule(catchallId, FeeType.FIXED, 200L, null, null, null, 200,    null,    null, P2P);
        when(feeRuleRepository.findApplicableRules(anyInt(), any(), any(), anyLong(), any(), any()))
                .thenReturn(List.of(catchall, specific)); // deliberately reversed to test sort
        when(feeRuleTierRepository.findByFeeRuleId(any())).thenReturn(List.of());

        Optional<FeeResult> result = service.calculate(
                Money.ofTiyin(1_000_000L, CurrencyCode.UZS), P2P, "uzcard", "uzcard", UZS, NOW);

        assertThat(result.get().appliedRule().id()).isEqualTo(specificId);
    }

    @Test
    void cross_network_rule_selected() {
        UUID crossId = UUID.randomUUID();
        FeeRule crossRule = senderRule(crossId, FeeType.PERCENTAGE, null, 150, 500L, 150_000L, 50, "uzcard", "humo", P2P);

        Optional<FeeResult> result = calculate(crossRule, 1_000_000L, "uzcard", "humo");

        assertThat(result.get().appliedRule().id()).isEqualTo(crossId);
    }

    @Test
    void lower_priority_number_wins() {
        UUID winnerId = UUID.randomUUID();
        UUID loserId  = UUID.randomUUID();
        FeeRule winner = senderRule(winnerId, FeeType.FIXED,  50L, null, null, null,  50, null, null, P2P);
        FeeRule loser  = senderRule(loserId,  FeeType.FIXED, 200L, null, null, null, 200, null, null, P2P);
        when(feeRuleRepository.findApplicableRules(anyInt(), any(), any(), anyLong(), any(), any()))
                .thenReturn(List.of(loser, winner));
        when(feeRuleTierRepository.findByFeeRuleId(any())).thenReturn(List.of());

        Optional<FeeResult> result = service.calculate(
                Money.ofTiyin(500_000L, CurrencyCode.UZS), P2P, "uzcard", "humo", UZS, NOW);

        assertThat(result.get().appliedRule().id()).isEqualTo(winnerId);
    }

    @Test
    void specificity_tiebreak_at_equal_priority() {
        // Both priority=50. specificId has transferTypeId set (score=4+2+1=7);
        // genericId has transferTypeId=null (score=0+2+1=3). Specific must win.
        UUID specificId = UUID.randomUUID();
        UUID genericId  = UUID.randomUUID();
        FeeRule specific = senderRule(specificId, FeeType.FIXED,  10L, null, null, null, 50, "uzcard", "uzcard",  P2P);
        FeeRule generic  = senderRule(genericId,  FeeType.FIXED, 999L, null, null, null, 50, "uzcard", "uzcard", null);
        when(feeRuleRepository.findApplicableRules(anyInt(), any(), any(), anyLong(), any(), any()))
                .thenReturn(List.of(generic, specific));
        when(feeRuleTierRepository.findByFeeRuleId(any())).thenReturn(List.of());

        Optional<FeeResult> result = service.calculate(
                Money.ofTiyin(1_000_000L, CurrencyCode.UZS), P2P, "uzcard", "uzcard", UZS, NOW);

        assertThat(result.get().appliedRule().id()).isEqualTo(specificId);
    }

    // ─── No-match / edge cases ───────────────────────────────────────────────

    @Test
    void no_matching_rule_returns_empty() {
        when(feeRuleRepository.findApplicableRules(anyInt(), any(), any(), anyLong(), any(), any()))
                .thenReturn(List.of());

        Optional<FeeResult> result = service.calculate(
                Money.ofTiyin(1_000_000L, CurrencyCode.UZS), P2P, "uzcard", "humo", UZS, NOW);

        assertThat(result).isEmpty();
    }

    @Test
    void expired_rule_not_returned_by_repo_yields_empty() {
        // Expired rules are filtered by the DB query; service sees an empty list
        when(feeRuleRepository.findApplicableRules(anyInt(), any(), any(), anyLong(), any(), any()))
                .thenReturn(List.of());

        Optional<FeeResult> result = service.calculate(
                Money.ofTiyin(1_000_000L, CurrencyCode.UZS), P2P, "uzcard", "humo", UZS, NOW);

        assertThat(result).isEmpty();
    }

    @Test
    void future_rule_not_returned_by_repo_yields_empty() {
        when(feeRuleRepository.findApplicableRules(anyInt(), any(), any(), anyLong(), any(), any()))
                .thenReturn(List.of());

        Optional<FeeResult> result = service.calculate(
                Money.ofTiyin(1_000_000L, CurrencyCode.UZS), P2P, "uzcard", "humo", UZS, NOW);

        assertThat(result).isEmpty();
    }

    @Test
    void merchant_pays_fee_result_carries_fee_payer() {
        UUID ruleId = UUID.randomUUID();
        FeeRule merchantRule = rule(ruleId, FeeType.PERCENTAGE, null, 65, null, null,
                100, null, null, 3, FeePayer.MERCHANT);
        when(feeRuleRepository.findApplicableRules(anyInt(), any(), any(), anyLong(), any(), any()))
                .thenReturn(List.of(merchantRule));
        when(feeRuleTierRepository.findByFeeRuleId(any())).thenReturn(List.of());

        Optional<FeeResult> result = service.calculate(
                Money.ofTiyin(1_000_000L, CurrencyCode.UZS), 3, null, null, UZS, NOW);

        assertThat(result).isPresent();
        assertThat(result.get().appliedRule().feePayer()).isEqualTo(FeePayer.MERCHANT);
    }
}
