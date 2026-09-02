package uz.pulsepay.fee.application.usecase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.pulsepay.domain.fee.FeePayer;
import uz.pulsepay.domain.fee.FeeRecipient;
import uz.pulsepay.domain.fee.FeeRule;
import uz.pulsepay.domain.fee.FeeRuleTier;
import uz.pulsepay.domain.fee.FeeType;
import uz.pulsepay.domain.fee.FeeRuleEntity;
import uz.pulsepay.domain.fee.FeeRuleTierEntity;
import uz.pulsepay.service.FeeService;
import uz.pulsepay.service.FeeService.AddTierCommand;
import uz.pulsepay.service.FeeService.CreateFeeRuleCommand;
import uz.pulsepay.repository.FeeRuleRepository;
import uz.pulsepay.repository.FeeRuleTierRepository;
import uz.pulsepay.domain.shared.AuditContext;
import uz.pulsepay.domain.shared.ConflictException;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManageFeeRuleUseCaseTest {

    private FeeRuleRepository ruleRepo;
    private FeeRuleTierRepository tierRepo;
    private FeeService service;

    private static final UUID ADMIN_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ruleRepo = mock(FeeRuleRepository.class);
        tierRepo = mock(FeeRuleTierRepository.class);
        service  = new FeeService(ruleRepo, tierRepo);

        AuditContext.setAdminId(ADMIN_ID);
        when(ruleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        AuditContext.clear();
    }

    // ─── create ──────────────────────────────────────────────────────────────

    @Test
    void create_rule_saves_and_returns_rule_with_admin_attribution() {
        when(ruleRepo.findActiveByScope(any(), any(), any(), any(), eq(100)))
                .thenReturn(List.of());

        FeeRule rule = service.createRule(fixedCommand("test-rule", 100));

        assertThat(rule.id()).isNotNull();
        assertThat(rule.name()).isEqualTo("test-rule");
        assertThat(rule.isActive()).isTrue();
        assertThat(rule.createdByAdminId()).isEqualTo(ADMIN_ID);
        verify(ruleRepo).save(any());
    }

    @Test
    void create_rule_fails_when_overlapping_rule_exists_at_same_priority() {
        UUID existingId = UUID.randomUUID();
        FeeRule existing = ruleWithPriority(existingId, 100, Instant.EPOCH, null);
        when(ruleRepo.findActiveByScope(any(), any(), any(), any(), eq(100)))
                .thenReturn(List.of(FeeRuleEntity.fromDomain(existing)));

        assertThatThrownBy(() -> service.createRule(fixedCommand("duplicate", 100)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Overlapping fee rule");
    }

    @Test
    void create_rule_throws_when_no_admin_in_context() {
        AuditContext.clear();

        assertThatThrownBy(() -> service.createRule(fixedCommand("test", 50)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Admin identity is required");
    }

    // ─── deactivate ──────────────────────────────────────────────────────────

    @Test
    void deactivate_sets_is_active_false_and_effective_to_now() {
        UUID ruleId = UUID.randomUUID();
        FeeRule active = ruleWithPriority(ruleId, 100, Instant.EPOCH, null);
        when(ruleRepo.findById(ruleId)).thenReturn(Optional.of(FeeRuleEntity.fromDomain(active)));

        FeeRule result = service.deactivate(ruleId);

        assertThat(result.isActive()).isFalse();
        assertThat(result.effectiveTo()).isNotNull();
        assertThat(result.updatedByAdminId()).isEqualTo(ADMIN_ID);
    }

    @Test
    void deactivate_throws_not_found_for_unknown_id() {
        when(ruleRepo.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── supersede ───────────────────────────────────────────────────────────

    @Test
    void supersede_deactivates_old_and_creates_new() {
        UUID oldId = UUID.randomUUID();
        FeeRule old = ruleWithPriority(oldId, 100, Instant.EPOCH, null);
        when(ruleRepo.findById(oldId)).thenReturn(Optional.of(FeeRuleEntity.fromDomain(old)));

        FeeRule result = service.supersede(oldId, fixedCommand("replacement", 100));

        assertThat(result.id()).isNotEqualTo(oldId);
        assertThat(result.name()).isEqualTo("replacement");
        assertThat(result.isActive()).isTrue();
        // Both old (deactivated) and new rule must be saved
        verify(ruleRepo, org.mockito.Mockito.times(2)).save(any());
    }

    // ─── add tier ────────────────────────────────────────────────────────────

    @Test
    void add_tier_succeeds_when_no_transfer_references() {
        UUID ruleId = UUID.randomUUID();
        FeeRule tieredRule = tieredRule(ruleId);
        when(ruleRepo.findById(ruleId)).thenReturn(Optional.of(FeeRuleEntity.fromDomain(tieredRule)));
        when(ruleRepo.countTransferReferences(ruleId)).thenReturn(0L);

        FeeRuleTier tier = service.addTier(ruleId, new AddTierCommand(0L, 999_999L, 500L, null));

        assertThat(tier.feeRuleId()).isEqualTo(ruleId);
        assertThat(tier.fixedAmount()).isEqualTo(500L);
        verify(tierRepo).save(any());
    }

    @Test
    void add_tier_fails_when_rule_has_live_references() {
        UUID ruleId = UUID.randomUUID();
        when(ruleRepo.findById(ruleId)).thenReturn(Optional.of(FeeRuleEntity.fromDomain(tieredRule(ruleId))));
        when(ruleRepo.countTransferReferences(ruleId)).thenReturn(5L);

        assertThatThrownBy(() -> service.addTier(ruleId, new AddTierCommand(0L, null, null, 100)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("live transfer reference");
        verify(tierRepo, never()).save(any());
    }

    @Test
    void add_tier_fails_for_non_tiered_rule() {
        UUID ruleId = UUID.randomUUID();
        FeeRule fixedRule = ruleWithPriority(ruleId, 100, Instant.EPOCH, null); // FIXED type
        when(ruleRepo.findById(ruleId)).thenReturn(Optional.of(FeeRuleEntity.fromDomain(fixedRule)));
        when(ruleRepo.countTransferReferences(ruleId)).thenReturn(0L);

        assertThatThrownBy(() -> service.addTier(ruleId, new AddTierCommand(0L, null, 500L, null)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("TIERED");
    }

    // ─── remove tier ─────────────────────────────────────────────────────────

    @Test
    void remove_tier_calls_deleteById_when_no_live_references() {
        UUID ruleId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();
        when(ruleRepo.findById(ruleId)).thenReturn(Optional.of(FeeRuleEntity.fromDomain(tieredRule(ruleId))));
        when(ruleRepo.countTransferReferences(ruleId)).thenReturn(0L);

        service.removeTier(ruleId, tierId);

        verify(tierRepo).deleteById(tierId);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private static CreateFeeRuleCommand fixedCommand(String name, int priority) {
        return new CreateFeeRuleCommand(
                name, null, null, 0L, null,
                FeeType.FIXED, 1000L, null, null, null,
                "UZS", priority, Instant.EPOCH, null,
                1, FeePayer.SENDER, FeeRecipient.PLATFORM, null);
    }

    private static FeeRule ruleWithPriority(UUID id, int priority, Instant from, Instant to) {
        return new FeeRule(id, "rule", null, null,
                0L, null, FeeType.FIXED, 1000L, null, null, null,
                "UZS", priority, true,
                from, to, 1,
                FeePayer.SENDER, FeeRecipient.PLATFORM,
                Instant.now(), ADMIN_ID, null);
    }

    private static FeeRule tieredRule(UUID id) {
        return new FeeRule(id, "tiered-rule", null, null,
                0L, null, FeeType.TIERED, null, null, null, null,
                "UZS", 50, true,
                Instant.EPOCH, null, 1,
                FeePayer.SENDER, FeeRecipient.PLATFORM,
                Instant.now(), ADMIN_ID, null);
    }
}
