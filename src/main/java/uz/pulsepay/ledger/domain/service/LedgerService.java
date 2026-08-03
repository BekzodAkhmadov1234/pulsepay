package uz.pulsepay.ledger.domain.service;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sole writer to the ledger. Enforces the double-entry invariant (Risk #1):
 * sum of debits must equal sum of credits within each ledger transaction.
 */
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
                                    String sourceNetwork, String destNetwork) {
        LedgerAccount sourceClearing = findAccount(sourceNetwork + "_clearing");
        LedgerAccount destClearing = findAccount(destNetwork + "_clearing");
        LedgerAccount feeRevenue = findAccount("fee_revenue");

        // Ledger transaction record
        LedgerTransaction txn = new LedgerTransaction(
                UUID.randomUUID(), transferId, "posted",
                transferId.toString() + "-transfer",
                1, // entry_type_id=1 → "transfer"
                Instant.now(), Instant.now(), null);
        ledgerTransactionRepository.save(txn);

        List<LedgerEntry> entries = new ArrayList<>();
        long totalDebit = 0;
        long totalCredit = 0;

        // Principal: debit source clearing, credit dest clearing
        entries.add(entry(txn.id(), sourceClearing.id(), EntryDirection.DEBIT, amount.amount(), amount.currency().name()));
        totalDebit += amount.amount();
        entries.add(entry(txn.id(), destClearing.id(), EntryDirection.CREDIT, amount.amount(), amount.currency().name()));
        totalCredit += amount.amount();

        // Fee: debit source clearing, credit fee_revenue
        if (feeAmount.amount() > 0) {
            entries.add(entry(txn.id(), sourceClearing.id(), EntryDirection.DEBIT, feeAmount.amount(), feeAmount.currency().name()));
            totalDebit += feeAmount.amount();
            entries.add(entry(txn.id(), feeRevenue.id(), EntryDirection.CREDIT, feeAmount.amount(), feeAmount.currency().name()));
            totalCredit += feeAmount.amount();
        }

        if (totalDebit != totalCredit) {
            throw new DomainException("Double-entry invariant violated: debit=" + totalDebit + " credit=" + totalCredit);
        }

        ledgerEntryRepository.saveAll(entries);

        // Atomic SQL increments (Risk #2)
        ledgerAccountRepository.incrementPostedBalance(sourceClearing.id(), -(amount.amount() + feeAmount.amount()));
        ledgerAccountRepository.incrementPostedBalance(destClearing.id(), amount.amount());
        if (feeAmount.amount() > 0) {
            ledgerAccountRepository.incrementPostedBalance(feeRevenue.id(), feeAmount.amount());
        }
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
