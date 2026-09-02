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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.service.BankTransferService;
import uz.pulsepay.service.TransferService;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.dto.request.InitiateBankTransferRequest;
import uz.pulsepay.dto.response.TransferResponse;
import uz.pulsepay.domain.transfer.TransferChannel;

import java.util.UUID;

@Tag(name = "Bank Transfers (P2A)", description = "Person-to-Account bank transfer lifecycle: initiate → OTP confirm → completed")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/bank-transfers")
public class BankTransferController {

    private final BankTransferService bankTransferService;
    private final TransferService transferService;

    public BankTransferController(BankTransferService bankTransferService,
                                   TransferService transferService) {
        this.bankTransferService = bankTransferService;
        this.transferService     = transferService;
    }

    @Operation(summary = "Initiate a P2A bank transfer",
               description = "Creates a bank transfer in `otp_pending` status. Use `PATCH /api/v1/transfers/{id}/otp` to confirm.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Transfer created — awaiting OTP confirmation"),
            @ApiResponse(responseCode = "400", description = "Validation error or business rule violation"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    public ResponseEntity<TransferResponse> initiate(@Valid @RequestBody InitiateBankTransferRequest request,
                                                     Authentication authentication) {
        UUID senderId = extractUserId(authentication);
        Money amount = Money.fromUzs(request.amountUzs());
        TransferChannel channel = request.channel() != null
                ? TransferChannel.valueOf(request.channel().toUpperCase())
                : TransferChannel.MOBILE_APP;

        var transfer = bankTransferService.initiate(
                senderId, request.senderInstrumentId(),
                request.senderCardNetwork().toLowerCase(),
                request.recipientIban(), request.recipientBankId(),
                request.recipientAccountHolderName(),
                amount, request.purposeCodeId(), channel, request.idempotencyKey());
        return ResponseEntity.accepted().body(TransferResponse.from(transfer));
    }

    @Operation(summary = "Get bank transfer by ID")
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
