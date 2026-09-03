package uz.pulsepay.utils.gateway;

import uz.pulsepay.domain.shared.Money;

import java.time.Instant;
import java.util.Map;

/**
 * Abstraction over the Paynet utility payment rail for P2S transfers.
 *
 * <p>Stub-to-real swap: replace {@code StubPaynetGateway} with a production bean that
 * implements this interface and calls the Paynet SDK / REST API.
 *
 * <p>Real Paynet integration sketch:
 * <pre>
 *   Paynet paynet = new Paynet(merchantId, secretKey);
 *   paynet.transaction().performTransaction(serviceCode, fields, amount);
 * </pre>
 */
public interface PaynetGateway {

    /**
     * Identifier for this gateway implementation.
     * Stub: "stub_paynet" | Production: "paynet"
     */
    String gatewayCode();

    /**
     * Submits a utility payment to the Paynet provider.
     *
     * @param transactionId  Platform transfer UUID (used as Paynet reference)
     * @param serviceCode    Paynet service code (e.g. "gas-uzb")
     * @param transactionTime Time of the transaction
     * @param fields         Provider-specific fields (e.g. {account_number: "12345"})
     * @param amount         Amount to credit (in tiyin)
     * @return               Result with Paynet-assigned transaction ID
     */
    PaynetResult performTransaction(String transactionId, String serviceCode,
                                    Instant transactionTime, Map<String, String> fields,
                                    Money amount);

    record PaynetResult(String paynetTransactionId, boolean success, String statusMessage) {}
}
