package uz.pulsepay.compliance.domain.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.compliance.domain.model.RegulatoryParameter;
import uz.pulsepay.shared.exception.DomainException;

/**
 * Resolves BCV-denominated regulatory thresholds to UZS tiyin.
 *
 * Phase 0 requirement: "AML/BCV thresholds must be stored as BCV multiples, resolved
 * against a versioned, effective-dated BCV value (currently 412,000 UZS) — never
 * hardcode a UZS amount for a regulatory threshold."
 *
 * Usage:
 *   long uzsTiyin = bcvResolver.resolveToUzsTiyin("aml_large_operation_transfer_threshold");
 *   // → 500 BCV × 412,000 UZS × 100 tiyin/UZS = 20,600,000,000 tiyin
 */
@Service
public class BcvResolver {

    static final String BCV_CODE = "bcv";

    private final RegulatoryParameterResolver resolver;

    public BcvResolver(RegulatoryParameterResolver resolver) {
        this.resolver = resolver;
    }

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
        RegulatoryParameter param = resolver.resolve(code);
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
        RegulatoryParameter bcv = resolver.resolve(BCV_CODE);
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
}
