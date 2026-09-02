package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.domain.merchant.MerchantAccount;
import uz.pulsepay.service.MerchantService;
import uz.pulsepay.service.SettlementService;
import uz.pulsepay.domain.settlement.SettlementBatch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Settlements", description = "Settlement batch management")
@RestController
public class SettlementAdminController {

    private final SettlementService settlementService;
    private final MerchantService merchantService;

    public SettlementAdminController(SettlementService settlementService,
                                      MerchantService merchantService) {
        this.settlementService = settlementService;
        this.merchantService   = merchantService;
    }

    public record GenerateBatchRequest(
            @NotNull UUID merchantAccountId,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate operationalDate
    ) {}

    public record BatchResponse(UUID id, String batchType, UUID merchantAccountId,
                                 LocalDate operationalDate, long totalAmount, String status,
                                 Instant generatedAt, Instant settledAt) {
        static BatchResponse from(SettlementBatch b) {
            return new BatchResponse(b.id(), b.batchType().name().toLowerCase(),
                    b.merchantAccountId(), b.operationalDate(), b.totalAmount(),
                    b.status().name().toLowerCase(), b.generatedAt(), b.settledAt());
        }
    }

    @Operation(summary = "Generate a settlement batch for a merchant account (admin)")
    @PostMapping("/admin/v1/settlements/generate")
    public ResponseEntity<BatchResponse> generate(@RequestBody @Valid GenerateBatchRequest req) {
        SettlementBatch batch = settlementService.generateDailyBatch(
                req.merchantAccountId(), req.operationalDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(BatchResponse.from(batch));
    }

    @Operation(summary = "Submit a settlement batch (admin)")
    @PostMapping("/admin/v1/settlements/{id}/submit")
    public ResponseEntity<BatchResponse> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(BatchResponse.from(settlementService.submitBatch(id)));
    }

    @Operation(summary = "List settlement batches for a merchant account (admin)")
    @GetMapping("/admin/v1/settlements")
    public ResponseEntity<List<BatchResponse>> listAdmin(@RequestParam UUID merchantAccountId) {
        return ResponseEntity.ok(settlementService.listBatches(merchantAccountId)
                .stream().map(BatchResponse::from).toList());
    }

    @Operation(summary = "List own settlement batches (merchant)")
    @GetMapping("/merchant/v1/settlements")
    public ResponseEntity<List<BatchResponse>> listMerchant(Authentication auth) {
        UUID merchantId = UUID.fromString(auth.getName());
        MerchantAccount account = merchantService.getMyAccount(merchantId);
        return ResponseEntity.ok(settlementService.listBatches(account.id())
                .stream().map(BatchResponse::from).toList());
    }
}
