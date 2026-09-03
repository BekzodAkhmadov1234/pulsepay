package uz.pulsepay.utils.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.service.CardBalanceService;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Stub implementation of {@link PaynetGateway} and {@link CardNetworkGateway} for P2S transfers.
 *
 * <p>As a {@link CardNetworkGateway} (networkCode="stub_paynet"):
 * <ul>
 *   <li>{@link #debitCard}  — debits the sender's card shadow balance locally</li>
 *   <li>{@link #creditCard} — no-op (Paynet provider credit is handled by {@link #performTransaction})</li>
 *   <li>{@link #isCardNetwork} — returns false so no card_transaction is recorded for the provider instrument</li>
 * </ul>
 *
 * <p>As a {@link PaynetGateway}: always returns success; logs what a real Paynet SDK call would do.
 *
 * <p><strong>To switch to production:</strong>
 * <ol>
 *   <li>Create {@code PaynetSdkGateway implements CardNetworkGateway, PaynetGateway}
 *   <li>Inject the Paynet SDK client and merchant credentials
 *   <li>Remove {@code @Component} from this class (or add {@code @Profile("!prod")})
 * </ol>
 */
@Slf4j
@Component
public class StubPaynetGateway implements CardNetworkGateway, PaynetGateway {

    private final CardBalanceService cardBalanceService;

    public StubPaynetGateway(CardBalanceService cardBalanceService) {
        this.cardBalanceService = cardBalanceService;
    }

    // ── CardNetworkGateway ────────────────────────────────────────────────────

    @Override
    public String networkCode() {
        return "stub_paynet";
    }

    @Override
    public String debitCard(UUID cardId, Money amount, String referenceId) {
        String ref = "paynet_debit_" + referenceId;
        log.info("[STUB-PAYNET] Debit sender card: cardId={}, amount={} tiyin, ref={}",
                cardId, amount.amount(), ref);
        cardBalanceService.debit(cardId, amount.amount());
        return ref;
    }

    @Override
    public String creditCard(UUID instrumentId, Money amount, String referenceId) {
        // Paynet provider credit is handled by performTransaction(); this is a no-op.
        log.debug("[STUB-PAYNET] Provider instrument credited via Paynet (no shadow balance): instrumentId={}", instrumentId);
        return "paynet_credit_" + referenceId;
    }

    @Override
    public void reverseTransaction(String networkTransactionId) {
        log.info("[STUB-PAYNET] Reversing card debit: txId={}", networkTransactionId);
    }

    @Override
    public boolean isCardNetwork() {
        // Provider instrument is not in the cards table — skip credit-side card_transaction record.
        return false;
    }

    // ── PaynetGateway ─────────────────────────────────────────────────────────

    @Override
    public String gatewayCode() {
        return "stub_paynet";
    }

    @Override
    public PaynetResult performTransaction(String transactionId, String serviceCode,
                                           Instant transactionTime, Map<String, String> fields,
                                           Money amount) {
        String paynetTxId = "PAYNET-STUB-" + transactionId.toUpperCase();
        log.info("[STUB-PAYNET] performTransaction: txId={}, service={}, fields={}, amount={} tiyin",
                paynetTxId, serviceCode, fields, amount.amount());
        // Real implementation would:
        //   1. Authenticate with Paynet API (merchant ID + secret)
        //   2. POST /api/v1/transaction/perform with serviceCode, fields, amount
        //   3. Parse response for Paynet-assigned transaction ID
        //   4. Return success/failure based on Paynet status code
        return new PaynetResult(paynetTxId, true, "STUB_ACCEPTED");
    }
}
