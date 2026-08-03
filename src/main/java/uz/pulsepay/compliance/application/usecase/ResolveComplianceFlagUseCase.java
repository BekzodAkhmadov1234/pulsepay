package uz.pulsepay.compliance.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.compliance.domain.model.ComplianceFlag;
import uz.pulsepay.compliance.domain.port.in.ResolveComplianceFlagPort;
import uz.pulsepay.compliance.domain.port.out.ComplianceFlagRepository;
import uz.pulsepay.shared.exception.NotFoundException;

import java.time.Instant;
import java.util.UUID;

@Service
public class ResolveComplianceFlagUseCase implements ResolveComplianceFlagPort {

    private final ComplianceFlagRepository flagRepository;

    public ResolveComplianceFlagUseCase(ComplianceFlagRepository flagRepository) {
        this.flagRepository = flagRepository;
    }

    @Override
    @Transactional
    public void resolve(UUID flagId, UUID resolvedByAdminId, String notes) {
        ComplianceFlag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new NotFoundException("Compliance flag not found"));
        ComplianceFlag resolved = new ComplianceFlag(
                flag.id(), flag.transferId(), flag.partyId(), flag.flagType(),
                flag.regulatoryParameterId(), "resolved", flag.detectedAt(),
                Instant.now(), resolvedByAdminId, notes);
        flagRepository.save(resolved);
    }
}
