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
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.domain.transfer.TransferChannel;
import uz.pulsepay.dto.request.ConfirmOtpRequest;
import uz.pulsepay.dto.request.InitiateP2STransferRequest;
import uz.pulsepay.dto.response.TransferResponse;
import uz.pulsepay.service.P2STransferService;
import uz.pulsepay.service.TransferService;

import java.util.UUID;

@Tag(name = "P2S Transfers", description = "Person-to-Savings (Paynet utility) transfer lifecycle: initiate → OTP confirm → completed")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/p2s-transfers")
public class P2STransferController {

    private final P2STransferService p2sTransferService;
    private final TransferService    transferService;

    public P2STransferController(P2STransferService p2sTransferService,
                                  TransferService transferService) {
        this.p2sTransferService = p2sTransferService;
        this.transferService    = transferService;
    }

    @Operation(summary = "Initiate a P2S utility payment",
               description = "Creates a Paynet utility payment in `otp_pending` status. Use `PATCH /api/v1/p2s-transfers/{id}/otp` to confirm.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Transfer created — awaiting OTP confirmation"),
            @ApiResponse(responseCode = "400", description = "Validation error or business rule violation"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    public ResponseEntity<TransferResponse> initiate(@Valid @RequestBody InitiateP2STransferRequest request,
                                                     Authentication authentication) {
        UUID userId = extractUserId(authentication);
        Money amount = Money.fromUzs(request.amountUzs());
        TransferChannel channel = request.channel() != null
                ? TransferChannel.valueOf(request.channel().toUpperCase())
                : TransferChannel.MOBILE_APP;

        var transfer = p2sTransferService.initiate(
                userId, request.senderInstrumentId(),
                request.senderCardNetwork().toLowerCase(),
                request.serviceCode(), request.serviceFields(),
                amount, request.purposeCodeId(), channel, request.idempotencyKey());
        return ResponseEntity.accepted().body(TransferResponse.from(transfer));
    }

    @Operation(summary = "Confirm P2S transfer OTP",
               description = "Verifies the OTP, debits the sender's card, and credits the Paynet utility provider.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer completed"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "404", description = "Transfer not found or not owned by caller"),
            @ApiResponse(responseCode = "409", description = "Transfer is not in otp_pending status")
    })
    @PatchMapping("/{id}/otp")
    public ResponseEntity<TransferResponse> confirmOtp(
            @Parameter(description = "Transfer UUID") @PathVariable UUID id,
            @Valid @RequestBody ConfirmOtpRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        var transfer = p2sTransferService.confirmOtp(id, userId, request.code());
        return ResponseEntity.ok(TransferResponse.from(transfer));
    }

    @Operation(summary = "Get P2S transfer by ID")
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

    private static UUID extractUserId(Authentication authentication) {
        String userId = (String) ((UsernamePasswordAuthenticationToken) authentication).getDetails();
        return UUID.fromString(userId);
    }
}
