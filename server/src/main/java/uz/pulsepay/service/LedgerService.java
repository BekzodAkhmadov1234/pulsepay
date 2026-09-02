package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.pulsepay.domain.ledger.LedgerAccountEntity;
import uz.pulsepay.domain.ledger.LedgerEntryEntity;
import uz.pulsepay.domain.ledger.LedgerTransactionEntity;
import uz.pulsepay.domain.ledger.EntryDirection;
import uz.pulsepay.domain.ledger.LedgerAccount;
import uz.pulsepay.domain.ledger.LedgerEntry;
import uz.pulsepay.domain.ledger.LedgerTransaction;
import uz.pulsepay.repository.LedgerAccountRepository;
import uz.pulsepay.repository.LedgerEntryRepository;
import uz.pulsepay.repository.LedgerTransactionRepository;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.domain.shared.DomainException;

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
public class LedgerService {

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

    public void postTransferEntries(UUID transferId, Money amount, Money feeAmount,
                                    String sourceNetwork, String destNetwork, String feeRecipient) {
        String srcAccountCode  = toAccountCode(sourceNetwork);
        String destAccountCode = toAccountCode(destNetwork);

        LedgerAccount sourceClearing = findAccount(srcAccountCode);
        LedgerAccount destClearing   = findAccount(destAccountCode);

        // ── Transaction 1: principal transfer (entry_type_id=1) ──────────────
        LedgerTransactionEntity transferTxnEntity = new LedgerTransactionEntity(
                UUID.randomUUID(), transferId, "posted",
                transferId + "-transfer",
                1, // entry_type_id=1 → "transfer"
                Instant.now(), Instant.now(), null);
        ledgerTransactionRepository.save(transferTxnEntity);
        UUID transferTxnId = transferTxnEntity.toDomain().id();

        ledgerEntryRepository.saveAll(List.of(
                entryEntity(transferTxnId, sourceClearing.id(), EntryDirection.DEBIT,  amount.amount(), amount.currency().name()),
                entryEntity(transferTxnId, destClearing.id(),   EntryDirection.CREDIT, amount.amount(), amount.currency().name())
        ));

        // ── Balance updates for principal ─────────────────────────────────────
        // source_clearing decreases (negative increment = debit on a credit-normal account)
        ledgerAccountRepository.incrementPostedBalance(sourceClearing.id(), -amount.amount());
        ledgerAccountRepository.incrementPostedBalance(destClearing.id(),    amount.amount());

        // ── Transaction 2: fee revenue (entry_type_id=2) — only if fee > 0 ──
        if (feeAmount.amount() > 0) {
            LedgerAccount feeAccount = resolveFeeAccount(feeRecipient, transferId);

            LedgerTransactionEntity feeTxnEntity = new LedgerTransactionEntity(
                    UUID.randomUUID(), transferId, "posted",
                    transferId + "-fee",
                    2, // entry_type_id=2 → "fee"
                    Instant.now(), Instant.now(), null);
            ledgerTransactionRepository.save(feeTxnEntity);
            UUID feeTxnId = feeTxnEntity.toDomain().id();

            ledgerEntryRepository.saveAll(List.of(
                    entryEntity(feeTxnId, sourceClearing.id(), EntryDirection.DEBIT,  feeAmount.amount(), feeAmount.currency().name()),
                    entryEntity(feeTxnId, feeAccount.id(),     EntryDirection.CREDIT, feeAmount.amount(), feeAmount.currency().name())
            ));

            ledgerAccountRepository.incrementPostedBalance(sourceClearing.id(), -feeAmount.amount());
            ledgerAccountRepository.incrementPostedBalance(feeAccount.id(),      feeAmount.amount());
        }
    }

    private String toAccountCode(String instrumentTypeStr) {
        String prefix = "bank_account".equals(instrumentTypeStr) ? "bank" : instrumentTypeStr;
        return prefix + "_clearing";
    }

    private LedgerAccount resolveFeeAccount(String feeRecipient, UUID transferId) {
        if (!"PLATFORM".equalsIgnoreCase(feeRecipient)) {
            log.warn("Non-platform fee recipient '{}' not yet routed for transfer {}; crediting fee_revenue",
                     feeRecipient, transferId);
        }
        return findAccount("fee_revenue");
    }

    private LedgerAccount findAccount(String code) {
        LedgerAccountEntity entity = ledgerAccountRepository.findByCode(code)
                .orElseThrow(() -> new DomainException("Ledger account not found: " + code));
        return entity.toDomain();
    }

    private LedgerEntryEntity entryEntity(UUID txnId, UUID accountId, EntryDirection direction,
                                          long amount, String currency) {
        LedgerEntry entry = new LedgerEntry(UUID.randomUUID(), txnId, accountId, direction,
                amount, currency, "posted", Instant.now());
        return LedgerEntryEntity.fromDomain(entry);
    }
}
