package uz.pulsepay.compliance.domain.port.out;

import uz.pulsepay.compliance.domain.model.ComplianceFlag;

import java.util.Optional;
import java.util.UUID;

public interface ComplianceFlagRepository {
    ComplianceFlag save(ComplianceFlag flag);
    Optional<ComplianceFlag> findById(UUID id);
}
