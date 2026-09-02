package uz.pulsepay.utils.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.pulsepay.domain.shared.Money;

import java.util.UUID;

/**
 * Stub implementation of {@link BankAccountPullGateway}.
 *
 * Simulates National Bank of Uzbekistan Open Banking API responses locally.
 * Always returns success; logs what a real API call would do.
 *
 * <p><strong>To switch to production:</strong>
 * <ol>
 *   <li>Create {@code NbuOpenBankingGateway implements BankAccountPullGateway}
 *   <li>Inject {@code NbuApiClient} (WebClient/RestClient) with OAuth2 credentials
 *   <li>Remove {@code @Component} from this class (or add {@code @Profile("!prod")})
 *   <li>Add {@code @Profile("prod")} / {@code @Primary} to the real impl
 * </ol>
 */
@Slf4j
@Component
public class StubBankAccountPullGateway implements BankAccountPullGateway {

    @Override
    public String gatewayCode() {
        return "stub_bank_pull";
    }

    @Override
    public PullResult initiateDebit(String iban, UUID bankId, String accountHolderName,
                                     Money amount, String referenceId) {
        String txId = "NBU-STUB-" + referenceId.toUpperCase();
        log.info("[STUB-NBU] Initiating bank pull debit: iban={}, bank={}, holder='{}', amount={} tiyin, ref={}",
                iban, bankId, accountHolderName, amount.amount(), referenceId);
        // ── Real implementation would:
        //    1. Acquire OAuth2 token from NBU token endpoint
        //    2. POST https://api.nbu.uz/openbanking/v1/debits with IBAN, amount, reference
        //    3. Poll or receive webhook for ACCEPTED/REJECTED status
        //    4. Return the NBU-assigned transaction ID
        return new PullResult(txId, true, "STUB_ACCEPTED");
    }

    @Override
    public void reverseDebit(String gatewayTransactionId) {
        log.info("[STUB-NBU] Reversing bank pull debit: txId={}", gatewayTransactionId);
        // Real: POST https://api.nbu.uz/openbanking/v1/debits/{txId}/reverse
    }
}
