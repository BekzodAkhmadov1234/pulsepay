package uz.pulsepay.compliance.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.pulsepay.compliance.domain.port.in.ResolveComplianceFlagPort;
import uz.pulsepay.compliance.domain.service.RegulatoryParameterResolver;
import uz.pulsepay.shared.audit.AuditContext;

import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/compliance")
public class ComplianceAdminController {

    private final ResolveComplianceFlagPort resolveComplianceFlagPort;
    private final RegulatoryParameterResolver parameterResolver;

    public ComplianceAdminController(ResolveComplianceFlagPort resolveComplianceFlagPort,
                                      RegulatoryParameterResolver parameterResolver) {
        this.resolveComplianceFlagPort = resolveComplianceFlagPort;
        this.parameterResolver = parameterResolver;
    }

    @PatchMapping("/flags/{id}/resolve")
    public ResponseEntity<Void> resolveFlag(
            @PathVariable UUID id,
            @RequestParam String notes) {
        UUID adminId = AuditContext.getAdminId();
        resolveComplianceFlagPort.resolve(id, adminId, notes);
        return ResponseEntity.noContent().build();
    }

    /**
     * Risk #5 mitigation: admin-triggered cache eviction for regulatory parameters.
     */
    @DeleteMapping("/regulatory-parameters/cache")
    public ResponseEntity<Void> evictRegulatoryCache() {
        parameterResolver.evictAll();
        return ResponseEntity.noContent().build();
    }
}
