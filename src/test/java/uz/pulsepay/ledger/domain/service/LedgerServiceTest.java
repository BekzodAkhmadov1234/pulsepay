package uz.pulsepay.ledger.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uz.pulsepay.ledger.domain.model.EntryDirection;
import uz.pulsepay.ledger.domain.model.LedgerAccount;
import uz.pulsepay.ledger.domain.model.LedgerEntry;
import uz.pulsepay.ledger.domain.model.LedgerTransaction;
import uz.pulsepay.ledger.domain.port.out.LedgerAccountRepository;
import uz.pulsepay.ledger.domain.port.out.LedgerEntryRepository;
import uz.pulsepay.ledger.domain.port.out.LedgerTransactionRepository;
import uz.pulsepay.shared.domain.CurrencyCode;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 0 MANDATORY test: ledger invariants.
 *
 * Rules under test:
 *  - Double-entry: sum of debits == sum of credits within each ledger transaction
 *  - Append-only: the port interface only exposes saveAll(), never delete/update
 *  - Balance updates go through atomic SQL increment, not ORM read-modify-write
 */
class LedgerServiceTest {

    private LedgerTransactionRepository txnRepo;
    private LedgerEntryRepository entryRepo;
    private LedgerAccountRepository accountRepo;
    private LedgerService service;

    private static final UUID TRANSFER_ID  = UUID.randomUUID();
    private static final UUID UZCARD_ACCT  = UUID.randomUUID();
    private static final UUID HUMO_ACCT    = UUID.randomUUID();
    private static final UUID FEE_ACCT     = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        txnRepo     = mock(LedgerTransactionRepository.class);
        entryRepo   = mock(LedgerEntryRepository.class);
        accountRepo = mock(LedgerAccountRepository.class);
        service     = new LedgerService(txnRepo, entryRepo, accountRepo);

        when(txnRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(accountRepo.findByCode("uzcard_clearing"))
                .thenReturn(Optional.of(account(UZCARD_ACCT, "uzcard_clearing")));
        when(accountRepo.findByCode("humo_clearing"))
                .thenReturn(Optional.of(account(HUMO_ACCT, "humo_clearing")));
        when(accountRepo.findByCode("fee_revenue"))
                .thenReturn(Optional.of(account(FEE_ACCT, "fee_revenue")));
    }

    @Test
    void postTransferEntries_with_zero_fee_saves_balanced_entries() {
        Money amount = Money.ofTiyin(5_000_000L, CurrencyCode.UZS);
        Money fee    = Money.ofTiyin(0L,          CurrencyCode.UZS);

        service.postTransferEntries(TRANSFER_ID, amount, fee, "uzcard", "humo");

        verify(txnRepo).save(any(LedgerTransaction.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(entryRepo).saveAll(captor.capture());

        List<LedgerEntry> entries = captor.getValue();
        assertThat(entries).hasSize(2);
        assertDoubleEntryBalanced(entries);
    }

    @Test
    void postTransferEntries_with_nonzero_fee_saves_4_balanced_entries() {
        Money amount = Money.ofTiyin(5_000_000L, CurrencyCode.UZS);
        Money fee    = Money.ofTiyin(50_000L,     CurrencyCode.UZS);

        service.postTransferEntries(TRANSFER_ID, amount, fee, "uzcard", "uzcard");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(entryRepo).saveAll(captor.capture());

        List<LedgerEntry> entries = captor.getValue();
        assertThat(entries).hasSize(4);
        assertDoubleEntryBalanced(entries);
    }

    @Test
    void postTransferEntries_performs_atomic_balance_increments() {
        Money amount = Money.ofTiyin(1_000_000L, CurrencyCode.UZS);
        Money fee    = Money.ofTiyin(10_000L,     CurrencyCode.UZS);

        service.postTransferEntries(TRANSFER_ID, amount, fee, "uzcard", "humo");

        // Source clearing decremented by amount + fee (atomic SQL increment with negative delta)
        verify(accountRepo).incrementPostedBalance(UZCARD_ACCT, -(1_000_000L + 10_000L));
        // Destination clearing incremented by principal amount
        verify(accountRepo).incrementPostedBalance(HUMO_ACCT, 1_000_000L);
        // Fee revenue incremented by fee
        verify(accountRepo).incrementPostedBalance(FEE_ACCT, 10_000L);
    }

    @Test
    void postTransferEntries_with_zero_fee_does_not_increment_fee_revenue() {
        Money amount = Money.ofTiyin(1_000_000L, CurrencyCode.UZS);
        Money fee    = Money.ofTiyin(0L,          CurrencyCode.UZS);

        service.postTransferEntries(TRANSFER_ID, amount, fee, "uzcard", "humo");

        // Fee revenue must NOT be touched when fee is zero
        verify(accountRepo, never()).incrementPostedBalance(eq(FEE_ACCT), anyLong());
    }

    @Test
    void postTransferEntries_missing_account_throws_domain_exception() {
        when(accountRepo.findByCode("missing_clearing")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.postTransferEntries(TRANSFER_ID,
                        Money.ofTiyin(100L, CurrencyCode.UZS),
                        Money.ofTiyin(0L,   CurrencyCode.UZS),
                        "missing", "humo"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("missing_clearing");
    }

    @Test
    void ledger_entry_repository_port_has_no_delete_or_update_methods() {
        // Structural test: append-only contract enforced by the port interface having
        // no delete() or update() methods — only saveAll().
        var methods = uz.pulsepay.ledger.domain.port.out.LedgerEntryRepository.class.getDeclaredMethods();
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

    private static void assertDoubleEntryBalanced(List<LedgerEntry> entries) {
        long totalDebit  = entries.stream()
                .filter(e -> e.direction() == EntryDirection.DEBIT)
                .mapToLong(LedgerEntry::amount).sum();
        long totalCredit = entries.stream()
                .filter(e -> e.direction() == EntryDirection.CREDIT)
                .mapToLong(LedgerEntry::amount).sum();
        assertThat(totalDebit)
                .as("Double-entry invariant: total debit must equal total credit")
                .isEqualTo(totalCredit);
    }

    private static LedgerAccount account(UUID id, String code) {
        return new LedgerAccount(id, 1, "debit", code, "UZS", 0L, 0L, "open", Instant.now());
    }
}
