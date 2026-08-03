package uz.pulsepay.ledger.domain.port.in;

import uz.pulsepay.shared.domain.Money;

import java.util.UUID;

/**
 * THE ONLY inbound port for the ledger module. No REST adapter exists.
 * Only called from the transfer use case, within the same @Transactional boundary (Risk #1).
 */
public interface PostLedgerEntriesPort {

    /**
     * Posts a double-entry transfer journal: debit clearing account, credit clearing account,
     * plus a fee revenue entry if feeAmount > 0.
     *
     * @param transferId  the business-level transfer this ledger transaction backs
     * @param amount      principal amount (debit sender clearing, credit recipient clearing)
     * @param feeAmount   fee amount (debit sender clearing, credit fee_revenue)
     * @param sourceNetwork  "uzcard" or "humo"
     * @param destNetwork    "uzcard" or "humo"
     */
    void postTransferEntries(UUID transferId, Money amount, Money feeAmount,
                             String sourceNetwork, String destNetwork);
}
