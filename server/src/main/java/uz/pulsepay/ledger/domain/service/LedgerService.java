package uz.pulsepay.ledger.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.pulsepay.ledger.domain.model.EntryDirection;
import uz.pulsepay.ledger.domain.model.LedgerAccount;
import uz.pulsepay.ledger.domain.model.LedgerEntry;
import uz.pulsepay.ledger.domain.model.LedgerTransaction;
import uz.pulsepay.ledger.domain.port.in.PostLedgerEntriesPort;
import uz.pulsepay.ledger.domain.port.out.LedgerAccountRepository;
import uz.pulsepay.ledger.domain.port.out.LedgerEntryRepository;
import uz.pulsepay.ledger.domain.port.out.LedgerTransactionRepository;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sole writer to the ledger. Enforces the double-entry invariant (Risk #1):
 * sum of debits must equal sum of credits within each ledger transaction.
 *
 * Posts two separate LedgerTransaction rows per transfer confirm:
 *   1) Transfer txn (entry_type_id=1): DEBIT source_clearing / CREDIT dest_clearing
 *   2) Fee txn    (entry_type_id=2): DEBIT source_clearing / CREDIT fee_revenue  (only if fee > 0)
 */
@Slf4j
@Service
public class LedgerService implements PostLedgerEntriesPort {

    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerAccountRepository ledgerAccountRepository;

    public LedgerService(LedgerTransactionRepository ledgerTransactionRepository,
                         LedgerEntryRepository ledgerEntryRepository,
                         LedgerAccountRepository ledgerAccountRepository) {
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
    }

    @Override
    public void postTransferEntries(UUID transferId, Money amount, Money feeAmount,
                                    String sourceNetwork, String destNetwork, String feeRecipient) {
        LedgerAccount sourceClearing = findAccount(sourceNetwork + "_clearing");
        LedgerAccount destClearing   = findAccount(destNetwork   + "_clearing");

        // ── Transaction 1: principal transfer (entry_type_id=1) ──────────────
        LedgerTransaction transferTxn = new LedgerTransaction(
                UUID.randomUUID(), transferId, "posted",
                transferId + "-transfer",
                1, // entry_type_id=1 → "transfer"
                Instant.now(), Instant.now(), null);
        ledgerTransactionRepository.save(transferTxn);

        ledgerEntryRepository.saveAll(List.of(
                entry(transferTxn.id(), sourceClearing.id(), EntryDirection.DEBIT,  amount.amount(), amount.currency().name()),
                entry(transferTxn.id(), destClearing.id(),   EntryDirection.CREDIT, amount.amount(), amount.currency().name())
        ));

        // ── Balance updates for principal ─────────────────────────────────────
        // source_clearing decreases (negative increment = debit on a credit-normal account)
        ledgerAccountRepository.incrementPostedBalance(sourceClearing.id(), -amount.amount());
        ledgerAccountRepository.incrementPostedBalance(destClearing.id(),    amount.amount());

        // ── Transaction 2: fee revenue (entry_type_id=2) — only if fee > 0 ──
        if (feeAmount.amount() > 0) {
            LedgerAccount feeAccount = resolveFeeAccount(feeRecipient, transferId);

            LedgerTransaction feeTxn = new LedgerTransaction(
                    UUID.randomUUID(), transferId, "posted",
                    transferId + "-fee",
                    2, // entry_type_id=2 → "fee"
                    Instant.now(), Instant.now(), null);
            ledgerTransactionRepository.save(feeTxn);

            ledgerEntryRepository.saveAll(List.of(
                    entry(feeTxn.id(), sourceClearing.id(), EntryDirection.DEBIT,  feeAmount.amount(), feeAmount.currency().name()),
                    entry(feeTxn.id(), feeAccount.id(),     EntryDirection.CREDIT, feeAmount.amount(), feeAmount.currency().name())
            ));

            ledgerAccountRepository.incrementPostedBalance(sourceClearing.id(), -feeAmount.amount());
            ledgerAccountRepository.incrementPostedBalance(feeAccount.id(),      feeAmount.amount());
        }
    }

    private LedgerAccount resolveFeeAccount(String feeRecipient, UUID transferId) {
        if (!"PLATFORM".equalsIgnoreCase(feeRecipient)) {
            log.warn("Non-platform fee recipient '{}' not yet routed for transfer {}; crediting fee_revenue",
                     feeRecipient, transferId);
        }
        return findAccount("fee_revenue");
    }

    private LedgerAccount findAccount(String code) {
        return ledgerAccountRepository.findByCode(code)
                .orElseThrow(() -> new DomainException("Ledger account not found: " + code));
    }

    private LedgerEntry entry(UUID txnId, UUID accountId, EntryDirection direction,
                               long amount, String currency) {
        return new LedgerEntry(UUID.randomUUID(), txnId, accountId, direction,
                amount, currency, "posted", Instant.now());
    }
}
