package uz.pulsepay.settlement.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uz.pulsepay.settlement.domain.model.SettlementBatch;
import uz.pulsepay.settlement.domain.port.in.ManageSettlementPort;
import uz.pulsepay.merchant.domain.port.in.MerchantSelfServicePort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Settlements", description = "Settlement batch management")
@RestController
public class SettlementAdminController {

    private final ManageSettlementPort manageSettlementPort;
    private final MerchantSelfServicePort merchantSelfServicePort;

    public SettlementAdminController(ManageSettlementPort manageSettlementPort,
                                      MerchantSelfServicePort merchantSelfServicePort) {
        this.manageSettlementPort    = manageSettlementPort;
        this.merchantSelfServicePort = merchantSelfServicePort;
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
        SettlementBatch batch = manageSettlementPort.generateDailyBatch(
                req.merchantAccountId(), req.operationalDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(BatchResponse.from(batch));
    }

    @Operation(summary = "Submit a settlement batch (admin)")
    @PostMapping("/admin/v1/settlements/{id}/submit")
    public ResponseEntity<BatchResponse> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(BatchResponse.from(manageSettlementPort.submitBatch(id)));
    }

    @Operation(summary = "List settlement batches for a merchant account (admin)")
    @GetMapping("/admin/v1/settlements")
    public ResponseEntity<List<BatchResponse>> listAdmin(@RequestParam UUID merchantAccountId) {
        return ResponseEntity.ok(manageSettlementPort.listBatches(merchantAccountId)
                .stream().map(BatchResponse::from).toList());
    }

    @Operation(summary = "List own settlement batches (merchant)")
    @GetMapping("/merchant/v1/settlements")
    public ResponseEntity<List<BatchResponse>> listMerchant(Authentication auth) {
        UUID merchantId = UUID.fromString(auth.getName());
        UUID accountId = merchantSelfServicePort.getMyAccount(merchantId).id();
        return ResponseEntity.ok(manageSettlementPort.listBatches(accountId)
                .stream().map(BatchResponse::from).toList());
    }
}
