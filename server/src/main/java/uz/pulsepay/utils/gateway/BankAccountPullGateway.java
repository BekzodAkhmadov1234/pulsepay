package uz.pulsepay.utils.gateway;

import uz.pulsepay.domain.shared.Money;

import java.util.UUID;

/**
 * Abstraction over the bank-side pull (debit) rail for A2P transfers.
 *
 * <p>Stub-to-real swap: replace the {@code StubBankAccountPullGateway} Spring bean
 * with a production bean that implements this interface and calls the
 * National Bank of Uzbekistan Open Banking API (or any bank-specific API).
 *
 * <p>Real NBU integration sketch:
 * <pre>
 *   POST https://api.nbu.uz/openbanking/v1/debits
 *   Authorization: Bearer {oauth2_token}
 *   {
 *     "accountIban":        "UZ...",
 *     "accountHolderName":  "...",
 *     "bankMfo":            "...",
 *     "amount":             100000,   // in tiyin
 *     "currency":           "UZS",
 *     "referenceId":        "...",
 *     "description":        "PulsePay A2P top-up"
 *   }
 *   → { "transactionId": "NBU-...", "status": "ACCEPTED" }
 * </pre>
 */
public interface BankAccountPullGateway {

    /**
     * Identifier for this gateway implementation.
     * Stub: "stub_bank_pull" | Production: "nbu_openbanking"
     */
    String gatewayCode();

    /**
     * Initiates a debit instruction on the specified bank account,
     * pulling {@code amount} into the platform.
     *
     * @param iban               IBAN of the source bank account (e.g. UZ + 25 digits)
     * @param bankId             Internal bank UUID (for routing / MFO lookup)
     * @param accountHolderName  Account holder name for verification
     * @param amount             Amount to pull (in tiyin)
     * @param referenceId        Platform-generated idempotency reference
     * @return                   Result with gateway-side transaction ID
     */
    PullResult initiateDebit(String iban, UUID bankId, String accountHolderName,
                              Money amount, String referenceId);

    /**
     * Reverses / cancels a previously initiated debit.
     * Called on transfer failure after a successful debit.
     *
     * @param gatewayTransactionId  Transaction ID returned by {@link #initiateDebit}
     */
    void reverseDebit(String gatewayTransactionId);

    record PullResult(String gatewayTransactionId, boolean success, String statusMessage) {}
}
