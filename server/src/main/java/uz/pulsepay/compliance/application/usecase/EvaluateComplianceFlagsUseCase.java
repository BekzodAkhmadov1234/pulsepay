package uz.pulsepay.compliance.application.usecase;

import org.springframework.stereotype.Service;
import uz.pulsepay.compliance.domain.port.in.EvaluateComplianceFlagsPort;
import uz.pulsepay.compliance.domain.service.ComplianceFlagEvaluationService;
import uz.pulsepay.shared.domain.Money;

import java.util.UUID;

@Service("evaluateComplianceFlagsUseCase")
public class EvaluateComplianceFlagsUseCase implements EvaluateComplianceFlagsPort {

    private final ComplianceFlagEvaluationService evaluationService;

    public EvaluateComplianceFlagsUseCase(ComplianceFlagEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Override
    public void evaluate(UUID transferId, UUID partyId, Money amount) {
        evaluationService.evaluate(transferId, partyId, amount);
    }
}
