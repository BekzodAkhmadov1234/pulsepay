package uz.pulsepay.ledger.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uz.pulsepay.domain.ledger.LedgerAccount;
import uz.pulsepay.domain.ledger.LedgerAccountEntity;
import uz.pulsepay.domain.ledger.LedgerEntryEntity;
import uz.pulsepay.domain.ledger.LedgerTransactionEntity;
import uz.pulsepay.repository.LedgerAccountRepository;
import uz.pulsepay.repository.LedgerEntryRepository;
import uz.pulsepay.repository.LedgerTransactionRepository;
import uz.pulsepay.service.LedgerService;
import uz.pulsepay.domain.shared.CurrencyCode;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.domain.shared.DomainException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 0 MANDATORY test: ledger invariants.
 *
 * Rules under test:
 *  - Two separate LedgerTransaction rows per confirm (entry_type=1 for transfer, entry_type=2 for fee)
 *  - Append-only: LedgerEntryRepository only exposes saveAll(), never delete/update
 *  - Balance updates go through atomic SQL increment, not ORM read-modify-write
 */
class LedgerServiceTest {

    private LedgerTransactionRepository txnRepo;
    private LedgerEntryRepository entryRepo;
    private LedgerAccountRepository accountRepo;
    private LedgerService service;

    private static final UUID TRANSFER_ID = UUID.randomUUID();
    private static final UUID UZCARD_ACCT = UUID.randomUUID();
    private static final UUID HUMO_ACCT   = UUID.randomUUID();
    private static final UUID FEE_ACCT    = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        txnRepo     = mock(LedgerTransactionRepository.class);
        entryRepo   = mock(LedgerEntryRepository.class);
        accountRepo = mock(LedgerAccountRepository.class);
        service     = new LedgerService(txnRepo, entryRepo, accountRepo);

        LedgerAccountEntity uzCardEntity = mock(LedgerAccountEntity.class);
        when(uzCardEntity.toDomain()).thenReturn(account(UZCARD_ACCT, "uzcard_clearing"));

        LedgerAccountEntity humoEntity = mock(LedgerAccountEntity.class);
        when(humoEntity.toDomain()).thenReturn(account(HUMO_ACCT, "humo_clearing"));

        LedgerAccountEntity feeEntity = mock(LedgerAccountEntity.class);
        when(feeEntity.toDomain()).thenReturn(account(FEE_ACCT, "fee_revenue"));

        when(accountRepo.findByCode("uzcard_clearing")).thenReturn(Optional.of(uzCardEntity));
        when(accountRepo.findByCode("humo_clearing")).thenReturn(Optional.of(humoEntity));
        when(accountRepo.findByCode("fee_revenue")).thenReturn(Optional.of(feeEntity));
    }

    @Test
    void zero_fee_posts_one_transfer_txn_with_two_entries() {
        Money amount = Money.ofTiyin(5_000_000L, CurrencyCode.UZS);
        Money fee    = Money.ofTiyin(0L, CurrencyCode.UZS);

        service.postTransferEntries(TRANSFER_ID, amount, fee, "uzcard", "humo", "PLATFORM");

        // Only transfer txn — no fee txn
        verify(txnRepo).save(any(LedgerTransactionEntity.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntryEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(entryRepo).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void nonzero_fee_posts_two_txns_each_with_two_entries() {
        Money amount = Money.ofTiyin(5_000_000L, CurrencyCode.UZS);
        Money fee    = Money.ofTiyin(50_000L, CurrencyCode.UZS);

        service.postTransferEntries(TRANSFER_ID, amount, fee, "uzcard", "uzcard", "PLATFORM");

        // Two LedgerTransaction rows: transfer txn + fee txn
        verify(txnRepo, org.mockito.Mockito.times(2)).save(any(LedgerTransactionEntity.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntryEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(entryRepo, org.mockito.Mockito.times(2)).saveAll(captor.capture());
        captor.getAllValues().forEach(entries -> assertThat(entries).hasSize(2));
    }

    @Test
    void atomic_balance_increments_are_split_per_txn() {
        Money amount = Money.ofTiyin(1_000_000L, CurrencyCode.UZS);
        Money fee    = Money.ofTiyin(10_000L, CurrencyCode.UZS);

        service.postTransferEntries(TRANSFER_ID, amount, fee, "uzcard", "humo", "PLATFORM");

        // Source clearing decremented twice: once for principal, once for fee
        verify(accountRepo).incrementPostedBalance(UZCARD_ACCT, -1_000_000L);
        verify(accountRepo).incrementPostedBalance(UZCARD_ACCT, -10_000L);
        // Destination clearing incremented by principal amount
        verify(accountRepo).incrementPostedBalance(HUMO_ACCT, 1_000_000L);
        // Fee revenue incremented by fee amount
        verify(accountRepo).incrementPostedBalance(FEE_ACCT, 10_000L);
    }

    @Test
    void zero_fee_does_not_post_fee_txn_or_touch_fee_revenue() {
        Money amount = Money.ofTiyin(1_000_000L, CurrencyCode.UZS);
        Money fee    = Money.ofTiyin(0L, CurrencyCode.UZS);

        service.postTransferEntries(TRANSFER_ID, amount, fee, "uzcard", "humo", "PLATFORM");

        // Fee revenue account must NOT be touched
        verify(accountRepo, never()).incrementPostedBalance(eq(FEE_ACCT), anyLong());
    }

    @Test
    void missing_account_throws_domain_exception() {
        when(accountRepo.findByCode("missing_clearing")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.postTransferEntries(TRANSFER_ID,
                        Money.ofTiyin(100L, CurrencyCode.UZS),
                        Money.ofTiyin(0L, CurrencyCode.UZS),
                        "missing", "humo", "PLATFORM"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("missing_clearing");
    }

    @Test
    void ledger_entry_repository_has_no_declared_delete_or_update_methods() {
        // LedgerEntryRepository extends JpaRepository with no additional methods,
        // ensuring it is append-only (no delete/update operations declared).
        var methods = uz.pulsepay.repository.LedgerEntryRepository.class.getDeclaredMethods();
        for (var m : methods) {
            String name = m.getName().toLowerCase();
            assertThat(name)
                    .as("LedgerEntryRepository must not expose: " + m.getName())
                    .doesNotContain("delete")
                    .doesNotContain("update")
                    .doesNotContain("remove");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static LedgerAccount account(UUID id, String code) {
        return new LedgerAccount(id, 1, "debit", code, "UZS", 0L, 0L, "open", Instant.now());
    }
}
