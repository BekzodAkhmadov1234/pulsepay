package uz.pulsepay.ledger.domain.port.in;

import uz.pulsepay.shared.domain.Money;

import java.util.UUID;

/**
 * THE ONLY inbound port for the ledger module. No REST adapter exists.
 * Only called from the transfer use case, within the same @Transactional boundary (Risk #1).
 */
public interface PostLedgerEntriesPort {

    /**
     * Posts two double-entry journal transactions:
     * 1) Transfer txn (entry_type=1): DEBIT source_clearing / CREDIT dest_clearing
     * 2) Fee txn (entry_type=2): DEBIT source_clearing / CREDIT fee_revenue  (only if feeAmount > 0)
     *
     * @param transferId    the business-level transfer this ledger transaction backs
     * @param amount        principal amount
     * @param feeAmount     fee amount (zero means no fee txn is posted)
     * @param sourceNetwork "uzcard" or "humo"
     * @param destNetwork   "uzcard" or "humo"
     * @param feeRecipient  "PLATFORM", "NETWORK", or "BANK" — controls which account receives the fee
     */
    void postTransferEntries(UUID transferId, Money amount, Money feeAmount,
                             String sourceNetwork, String destNetwork, String feeRecipient);
}
