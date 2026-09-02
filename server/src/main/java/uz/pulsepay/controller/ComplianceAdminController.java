package uz.pulsepay.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.service.ComplianceService;
import uz.pulsepay.domain.shared.AuditContext;

import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/compliance")
public class ComplianceAdminController {

    private final ComplianceService complianceService;

    public ComplianceAdminController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @PatchMapping("/flags/{id}/resolve")
    public ResponseEntity<Void> resolveFlag(@PathVariable UUID id, @RequestParam String notes) {
        UUID adminId = AuditContext.getAdminId();
        complianceService.resolve(id, adminId, notes);
        return ResponseEntity.noContent().build();
    }

    /**
     * Risk #5 mitigation: admin-triggered cache eviction for regulatory parameters.
     */
    @DeleteMapping("/regulatory-parameters/cache")
    public ResponseEntity<Void> evictRegulatoryCache() {
        complianceService.evictAllParameters();
        return ResponseEntity.noContent().build();
    }
}
