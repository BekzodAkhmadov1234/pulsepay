package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.service.FeeService;
import uz.pulsepay.service.TransferService;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.dto.request.ConfirmOtpRequest;
import uz.pulsepay.dto.request.InitiateTransferRequest;
import uz.pulsepay.dto.response.FeePreviewResponse;
import uz.pulsepay.dto.response.TransferResponse;
import uz.pulsepay.domain.transfer.TransferChannel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Tag(name = "Transfers", description = "P2P card transfer lifecycle: initiate → OTP confirm → completed/failed")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;
    private final FeeService feeService;

    public TransferController(TransferService transferService, FeeService feeService) {
        this.transferService = transferService;
        this.feeService = feeService;
    }

    @Operation(summary = "Preview transfer fee",
               description = "Returns the calculated fee for a given amount, networks, and transfer type. "
                       + "Response fields: feeAmountUzs, totalAmountUzs, minAmountUzs, "
                       + "maxAmountUzs, commissionPercent. No transfer is created.")
    @ApiResponse(responseCode = "200", description = "Fee preview")
    @GetMapping("/fee-preview")
    public ResponseEntity<FeePreviewResponse> feePreview(
            @RequestParam BigDecimal amountUzs,
            @RequestParam String sourceNetwork,
            @RequestParam String destNetwork,
            @RequestParam(defaultValue = "1") int transferTypeId) {
        Money amount = Money.fromUzs(amountUzs);
        var result = feeService.calculate(amount, transferTypeId,
                sourceNetwork.toLowerCase(), destNetwork.toLowerCase(), "UZS", Instant.now());
        if (result.isEmpty()) {
            return ResponseEntity.ok(FeePreviewResponse.noRule(amount.amount()));
        }
        var r = result.get();
        return ResponseEntity.ok(FeePreviewResponse.of(
                amount.amount(),
                r.fee().amount(),
                r.appliedRule().minAmount(),
                r.appliedRule().maxAmount(),
                r.appliedRule().percentageBps()));
    }

    @Operation(summary = "Initiate a transfer",
               description = "Creates a transfer in `otp_pending` status. Idempotent: re-submitting the same "
                       + "`idempotencyKey` returns the cached response without creating a duplicate.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Transfer created — awaiting OTP confirmation"),
            @ApiResponse(responseCode = "400", description = "Validation error or business rule violation"),
            @ApiResponse(responseCode = "409", description = "Idempotency key already used with different parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    public ResponseEntity<TransferResponse> initiate(@Valid @RequestBody InitiateTransferRequest request,
                                                     Authentication authentication) {
        UUID senderId = extractUserId(authentication);
        Money amount = Money.fromUzs(request.amountUzs());
        TransferChannel channel = request.channel() != null
                ? TransferChannel.valueOf(request.channel().toUpperCase())
                : TransferChannel.MOBILE_APP;

        var transfer = transferService.initiate(
                senderId, request.senderInstrumentId(), request.senderCardNetwork(),
                request.recipientId(), request.recipientInstrumentId(), request.recipientCardNetwork(),
                amount, request.transferTypeId(), request.purposeCodeId(), channel, request.idempotencyKey());
        return ResponseEntity.accepted().body(TransferResponse.from(transfer));
    }

    @Operation(summary = "Confirm transfer OTP",
               description = "Submits the 6-digit OTP to authorise the transfer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP accepted — transfer processed"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "404", description = "Transfer not found or not owned by caller"),
            @ApiResponse(responseCode = "409", description = "Transfer is not in otp_pending status")
    })
    @PatchMapping("/{id}/otp")
    public ResponseEntity<TransferResponse> confirmOtp(
            @Parameter(description = "Transfer UUID") @PathVariable UUID id,
            @Valid @RequestBody ConfirmOtpRequest request,
            Authentication authentication) {
        var transfer = transferService.confirmOtp(id, extractUserId(authentication), request.code());
        return ResponseEntity.ok(TransferResponse.from(transfer));
    }

    @Operation(summary = "Get transfer by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer found"),
            @ApiResponse(responseCode = "404", description = "Transfer not found or not owned by caller")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransfer(
            @Parameter(description = "Transfer UUID") @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(
                TransferResponse.from(transferService.getById(id, extractUserId(authentication))));
    }

    @Operation(summary = "List my transfers")
    @ApiResponse(responseCode = "200", description = "Transfer list")
    @GetMapping
    public ResponseEntity<List<TransferResponse>> listTransfers(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        List<TransferResponse> result = transferService.listSummaries(userId)
                .stream().map(TransferResponse::from).toList();
        return ResponseEntity.ok(result);
    }

    private static UUID extractUserId(Authentication authentication) {
        String userId = (String) ((UsernamePasswordAuthenticationToken) authentication).getDetails();
        return UUID.fromString(userId);
    }
}
