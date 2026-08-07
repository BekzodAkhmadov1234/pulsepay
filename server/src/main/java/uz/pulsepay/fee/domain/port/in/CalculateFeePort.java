package uz.pulsepay.fee.domain.port.in;

import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.shared.domain.Money;

import java.time.Instant;
import java.util.Optional;

public interface CalculateFeePort {
    /**
     * Returns the calculated fee and the matching rule (for audit in transfers.applied_fee_rule_id).
     * Returns empty if no fee applies (zero-fee P2P promo, etc.).
     */
    Optional<FeeResult> calculate(Money amount, int transferTypeId,
                                  String sourceNetwork, String destNetwork,
                                  String currencyCode, Instant occurringAt);

    record FeeResult(Money fee, FeeRule appliedRule) {}
}
