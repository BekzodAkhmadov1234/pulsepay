package uz.pulsepay.compliance.domain.port.out;

import uz.pulsepay.compliance.domain.model.RegulatoryParameter;

import java.util.Optional;

public interface RegulatoryParameterRepository {
    /**
     * Returns the currently-in-force row: effective_from <= NOW() AND (effective_to IS NULL OR effective_to > NOW())
     */
    Optional<RegulatoryParameter> findCurrentByCode(String code);
}
