package uz.pulsepay.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.compliance.ComplianceFlagEntity;
import uz.pulsepay.domain.compliance.RegulatoryParameterEntity;
import uz.pulsepay.domain.compliance.ComplianceFlag;
import uz.pulsepay.domain.compliance.FlagType;
import uz.pulsepay.domain.compliance.RegulatoryParameter;
import uz.pulsepay.config.CacheConfig;
import uz.pulsepay.repository.ComplianceFlagRepository;
import uz.pulsepay.repository.RegulatoryParameterRepository;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;

import java.time.Instant;
import java.util.UUID;

@Service
public class ComplianceService {

    private final ComplianceFlagRepository flagRepository;
    private final RegulatoryParameterRepository parameterRepository;

    public ComplianceService(ComplianceFlagRepository flagRepository,
                              RegulatoryParameterRepository parameterRepository) {
        this.flagRepository   = flagRepository;
        this.parameterRepository = parameterRepository;
    }

    // ── RegulatoryParameterResolver logic (inlined) ───────────────────────────

    /**
     * Resolves the currently-in-force regulatory parameter for a given code.
     * Results are cached with a TTL defined in CacheConfig.
     */
    @Cacheable(value = CacheConfig.REGULATORY_PARAMETERS_CACHE, key = "#code")
    public RegulatoryParameter resolveParameter(String code) {
        return parameterRepository.findCurrentByCode(code, Instant.now())
                .map(RegulatoryParameterEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("No active regulatory parameter for code: " + code));
    }

    @CacheEvict(value = CacheConfig.REGULATORY_PARAMETERS_CACHE, key = "#code")
    public void evictParameter(String code) {
        // Cache eviction — callable by admin endpoint
    }

    @CacheEvict(value = CacheConfig.REGULATORY_PARAMETERS_CACHE, allEntries = true)
    public void evictAllParameters() {
        // Full cache eviction
    }

    // ── BcvResolver logic (inlined) ───────────────────────────────────────────

    static final String BCV_CODE = "bcv";

    /**
     * Resolves a regulatory parameter to UZS tiyin.
     *
     * If {@code unit='uzs'}: returns {@code valueAmount} directly (already in tiyin).
     * If {@code unit='bcv'}: multiplies {@code valueAmount × current_bcv_tiyin}.
     *
     * @param code the {@code regulatory_parameters.code} to resolve
     * @return amount in UZS tiyin
     * @throws DomainException if unit is unknown
     */
    public long resolveToUzsTiyin(String code) {
        RegulatoryParameter param = resolveParameter(code);
        return switch (param.unit()) {
            case "uzs" -> param.valueAmount();
            case "bcv" -> {
                long bcvTiyin = currentBcvTiyin();
                yield param.valueAmount() * bcvTiyin;
            }
            default -> throw new DomainException(
                    "Unknown unit '%s' for regulatory parameter '%s'".formatted(param.unit(), code));
        };
    }

    /**
     * Returns the current BCV value in UZS tiyin (always unit='uzs').
     */
    public long currentBcvTiyin() {
        RegulatoryParameter bcv = resolveParameter(BCV_CODE);
        if (!"uzs".equals(bcv.unit())) {
            throw new DomainException("BCV parameter must have unit='uzs', found: " + bcv.unit());
        }
        return bcv.valueAmount();
    }

    /**
     * Returns the current BCV value in whole UZS (for display/logging).
     */
    public long currentBcvUzs() {
        return currentBcvTiyin() / 100L;
    }

    // ── ComplianceFlagEvaluationService logic (inlined) ──────────────────────

    /**
     * Evaluates AML/compliance flags for a given transfer.
     * Checks single-transfer threshold (500 BCV) and CDD one-off threshold (175M UZS).
     */
    public void evaluate(UUID transferId, UUID partyId, Money amount) {
        checkLargeTransactionThreshold(transferId, partyId, amount);
        checkAmlCddThreshold(transferId, partyId, amount);
    }

    private void checkLargeTransactionThreshold(UUID transferId, UUID partyId, Money amount) {
        try {
            RegulatoryParameter param = resolveParameter("aml_large_operation_transfer_threshold");
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
            RegulatoryParameter param = resolveParameter("aml_cdd_one_off_threshold");
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
            RegulatoryParameter bcv = resolveParameter(BCV_CODE);
            return param.valueAmount() * bcv.valueAmount();
        }
        return param.valueAmount();
    }

    private void raiseFlag(UUID transferId, UUID partyId, FlagType flagType, UUID paramId) {
        ComplianceFlag flag = new ComplianceFlag(
                UUID.randomUUID(), transferId, partyId, flagType,
                paramId, "open", Instant.now(), null, null, null);
        flagRepository.save(ComplianceFlagEntity.fromDomain(flag));
    }

    // ── ResolveComplianceFlagUseCase logic (inlined) ─────────────────────────

    @Transactional
    public void resolve(UUID flagId, UUID resolvedByAdminId, String notes) {
        ComplianceFlag flag = flagRepository.findById(flagId)
                .map(ComplianceFlagEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Compliance flag not found"));
        ComplianceFlag resolved = new ComplianceFlag(
                flag.id(), flag.transferId(), flag.partyId(), flag.flagType(),
                flag.regulatoryParameterId(), "resolved", flag.detectedAt(),
                Instant.now(), resolvedByAdminId, notes);
        flagRepository.save(ComplianceFlagEntity.fromDomain(resolved));
    }
}
