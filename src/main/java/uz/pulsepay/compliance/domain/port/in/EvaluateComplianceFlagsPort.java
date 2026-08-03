package uz.pulsepay.compliance.domain.port.in;

import uz.pulsepay.shared.domain.Money;

import java.util.UUID;

public interface EvaluateComplianceFlagsPort {
    void evaluate(UUID transferId, UUID partyId, Money amount);
}
