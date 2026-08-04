package uz.pulsepay.limit.domain.port.in;

import uz.pulsepay.shared.domain.Money;

import java.util.UUID;

public interface IncrementLimitUsagePort {
    void increment(UUID userId, Money amount, int transferTypeId);
}
