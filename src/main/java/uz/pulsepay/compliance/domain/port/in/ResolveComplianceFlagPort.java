package uz.pulsepay.compliance.domain.port.in;

import java.util.UUID;

public interface ResolveComplianceFlagPort {
    void resolve(UUID flagId, UUID resolvedByAdminId, String notes);
}
