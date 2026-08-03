package uz.pulsepay.compliance.domain.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.compliance.domain.model.ComplianceFlag;
import uz.pulsepay.compliance.domain.model.FlagType;
import uz.pulsepay.compliance.domain.model.RegulatoryParameter;
import uz.pulsepay.compliance.domain.port.out.ComplianceFlagRepository;
import uz.pulsepay.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

@Service
public class ComplianceFlagEvaluationService {

    private final RegulatoryParameterResolver parameterResolver;
    private final ComplianceFlagRepository flagRepository;

    public ComplianceFlagEvaluationService(RegulatoryParameterResolver parameterResolver,
                                            ComplianceFlagRepository flagRepository) {
        this.parameterResolver = parameterResolver;
        this.flagRepository = flagRepository;
    }

    public void evaluate(UUID transferId, UUID partyId, Money amount) {
        // Check single-transfer threshold (500 BCV)
        checkLargeTransactionThreshold(transferId, partyId, amount);

        // AML one-off CDD threshold (175M UZS)
        checkAmlCddThreshold(transferId, partyId, amount);
    }

    private void checkLargeTransactionThreshold(UUID transferId, UUID partyId, Money amount) {
        try {
            RegulatoryParameter param = parameterResolver.resolve("aml_large_operation_transfer_threshold");
            long thresholdTiyin = resolveThresholdTiyin(param);
            if (amount.amount() >= thresholdTiyin) {
                raiseFlag(transferId, partyId, FlagType.LARGE_TRANSACTION, param.id());
            }
        } catch (Exception ignored) {
            // If threshold not configured, skip (log in production)
        }
    }

    private void checkAmlCddThreshold(UUID transferId, UUID partyId, Money amount) {
        try {
            RegulatoryParameter param = parameterResolver.resolve("aml_cdd_one_off_threshold");
            long thresholdTiyin = resolveThresholdTiyin(param);
            if (amount.amount() >= thresholdTiyin) {
                raiseFlag(transferId, partyId, FlagType.LARGE_TRANSACTION, param.id());
            }
        } catch (Exception ignored) {
            // If threshold not yet effective, skip
        }
    }

    private long resolveThresholdTiyin(RegulatoryParameter param) {
        if ("bcv".equals(param.unit())) {
            // Resolve BCV value and multiply
            RegulatoryParameter bcv = parameterResolver.resolve("bcv");
            return param.valueAmount() * bcv.valueAmount();
        }
        return param.valueAmount();
    }

    private void raiseFlag(UUID transferId, UUID partyId, FlagType flagType, UUID paramId) {
        flagRepository.save(new ComplianceFlag(
                UUID.randomUUID(), transferId, partyId, flagType,
                paramId, "open", Instant.now(), null, null, null));
    }
}
