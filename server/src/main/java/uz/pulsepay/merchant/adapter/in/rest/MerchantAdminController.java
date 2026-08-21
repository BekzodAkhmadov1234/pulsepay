package uz.pulsepay.merchant.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.pulsepay.merchant.domain.command.OnboardMerchantCommand;
import uz.pulsepay.merchant.domain.model.Merchant;
import uz.pulsepay.merchant.domain.model.MerchantCategory;
import uz.pulsepay.merchant.domain.port.in.ManageMerchantPort;
import uz.pulsepay.merchant.domain.port.out.MerchantCategoryRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Admin — Merchants", description = "Merchant lifecycle management (admin only)")
@RestController
@RequestMapping("/admin/v1/merchants")
public class MerchantAdminController {

    private final ManageMerchantPort manageMerchantPort;
    private final MerchantCategoryRepository categoryRepository;

    public MerchantAdminController(ManageMerchantPort manageMerchantPort,
                                   MerchantCategoryRepository categoryRepository) {
        this.manageMerchantPort = manageMerchantPort;
        this.categoryRepository = categoryRepository;
    }

    public record CategoryResponse(int id, String mccCode, String nameUz, String riskTier) {
        static CategoryResponse from(MerchantCategory c) {
            return new CategoryResponse(c.id(), c.mccCode(), c.nameUz(), c.riskTier());
        }
    }

    @Operation(summary = "List merchant categories (MCC codes)")
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        return ResponseEntity.ok(categoryRepository.findAll().stream()
                .map(CategoryResponse::from).toList());
    }

    public record OnboardRequest(
            @NotBlank String legalTradeName,
            @NotBlank String mccCode,
            UUID acquiringBankId,
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RejectRequest(String reason) {}

    public record SuspendRequest(String reason) {}

    public record MerchantResponse(
            UUID id, String legalTradeName, Integer categoryId, UUID acquiringBankId,
            String kybStatus, String status, boolean uzqrEnabled, String email,
            Instant createdAt
    ) {
        static MerchantResponse from(Merchant m) {
            return new MerchantResponse(m.id(), m.legalTradeName(), m.categoryId(),
                    m.acquiringBankId(), m.kybStatus().name().toLowerCase(),
                    m.status().name().toLowerCase(), m.uzqrEnabled(), m.email(), m.createdAt());
        }
    }

    @Operation(summary = "Onboard a new merchant")
    @PostMapping
    public ResponseEntity<MerchantResponse> onboard(@RequestBody @Valid OnboardRequest req) {
        Merchant merchant = manageMerchantPort.onboard(new OnboardMerchantCommand(
                req.legalTradeName(), req.mccCode(), req.acquiringBankId(),
                req.email(), req.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(MerchantResponse.from(merchant));
    }

    @Operation(summary = "List all merchants")
    @GetMapping
    public ResponseEntity<List<MerchantResponse>> listAll() {
        List<MerchantResponse> list = manageMerchantPort.listAll().stream()
                .map(MerchantResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Get merchant by ID")
    @GetMapping("/{id}")
    public ResponseEntity<MerchantResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(MerchantResponse.from(manageMerchantPort.getById(id)));
    }

    @Operation(summary = "Approve merchant (KYB verified, status active)")
    @PostMapping("/{id}/approve")
    public ResponseEntity<MerchantResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(MerchantResponse.from(manageMerchantPort.approve(id)));
    }

    @Operation(summary = "Reject merchant KYB")
    @PostMapping("/{id}/reject")
    public ResponseEntity<MerchantResponse> reject(@PathVariable UUID id,
                                                    @RequestBody(required = false) RejectRequest req) {
        String reason = req != null ? req.reason() : "KYB rejected";
        return ResponseEntity.ok(MerchantResponse.from(manageMerchantPort.reject(id, reason)));
    }

    @Operation(summary = "Suspend merchant account")
    @PostMapping("/{id}/suspend")
    public ResponseEntity<MerchantResponse> suspend(@PathVariable UUID id,
                                                     @RequestBody(required = false) SuspendRequest req) {
        String reason = req != null ? req.reason() : "Suspended by admin";
        return ResponseEntity.ok(MerchantResponse.from(manageMerchantPort.suspend(id, reason)));
    }
}
