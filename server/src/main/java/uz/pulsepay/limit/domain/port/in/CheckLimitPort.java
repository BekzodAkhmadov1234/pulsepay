package uz.pulsepay.limit.domain.port.in;

import uz.pulsepay.shared.domain.Money;

import java.util.UUID;

public interface CheckLimitPort {
    void checkLimits(UUID userId, String kycLevel, Money amount, int transferTypeId);
}
