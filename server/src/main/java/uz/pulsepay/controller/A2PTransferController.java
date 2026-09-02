package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
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
import uz.pulsepay.dto.request.InitiateA2PTransferRequest;
import uz.pulsepay.dto.response.TransferResponse;
import uz.pulsepay.service.A2PTransferService;
import uz.pulsepay.service.TransferService;

import java.util.UUID;

@Tag(name = "A2P Transfers", description = "Account-to-Person bank pull: initiate → OTP confirm → card credited")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/a2p-transfers")
public class A2PTransferController {

    private final A2PTransferService a2pTransferService;
    private final TransferService transferService;

    public A2PTransferController(A2PTransferService a2pTransferService,
                                  TransferService transferService) {
        this.a2pTransferService = a2pTransferService;
        this.transferService    = transferService;
    }

    @Operation(summary = "Initiate an A2P bank pull transfer",
               description = "Pulls funds from a bank account and credits the user's card. Returns transfer in `otp_pending` status.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Transfer created — awaiting OTP confirmation"),
            @ApiResponse(responseCode = "400", description = "Validation error or business rule violation"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    public ResponseEntity<TransferResponse> initiate(@Valid @RequestBody InitiateA2PTransferRequest req,
                                                     Authentication auth) {
        UUID userId = extractUserId(auth);
        Money amount = Money.fromUzs(req.amountUzs());
        TransferChannel channel = req.channel() != null
                ? TransferChannel.valueOf(req.channel().toUpperCase())
                : TransferChannel.MOBILE_APP;

        var transfer = a2pTransferService.initiate(
                userId,
                req.sourceIban(),
                req.sourceBankId(),
                req.sourceAccountHolderName(),
                req.destinationInstrumentId(),
                req.destinationCardNetwork(),
                amount,
                req.purposeCodeId(),
                channel,
                req.idempotencyKey());

        return ResponseEntity.accepted().body(TransferResponse.from(transfer));
    }

    @Operation(summary = "Confirm A2P transfer OTP",
               description = "Verifies the OTP, executes the bank pull, and credits the card.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer completed"),
            @ApiResponse(responseCode = "400", description = "Invalid OTP or expired"),
            @ApiResponse(responseCode = "404", description = "Transfer not found")
    })
    @PatchMapping("/{id}/otp")
    public ResponseEntity<TransferResponse> confirmOtp(@PathVariable UUID id,
                                                        @Valid @RequestBody ConfirmOtpRequest req,
                                                        Authentication auth) {
        UUID userId = extractUserId(auth);
        var transfer = a2pTransferService.confirmOtp(id, userId, req.code());
        return ResponseEntity.ok(TransferResponse.from(transfer));
    }

    @Operation(summary = "Get A2P transfer by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable UUID id,
                                                         Authentication auth) {
        return ResponseEntity.ok(
                TransferResponse.from(transferService.getById(id, extractUserId(auth))));
    }

    private static UUID extractUserId(Authentication auth) {
        String userId = (String) ((UsernamePasswordAuthenticationToken) auth).getDetails();
        return UUID.fromString(userId);
    }
}
