package uz.pulsepay.transfer.adapter.in.rest;

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
import org.springframework.web.bind.annotation.*;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.transfer.adapter.in.rest.dto.ConfirmOtpRequest;
import uz.pulsepay.transfer.adapter.in.rest.dto.InitiateTransferRequest;
import uz.pulsepay.transfer.adapter.in.rest.dto.TransferResponse;
import uz.pulsepay.transfer.domain.model.TransferChannel;
import uz.pulsepay.transfer.domain.port.in.ConfirmTransferOtpPort;
import uz.pulsepay.transfer.domain.port.in.GetTransferPort;
import uz.pulsepay.transfer.domain.port.in.InitiateTransferPort;
import uz.pulsepay.transfer.domain.port.in.ListTransfersPort;
import uz.pulsepay.transfer.domain.port.in.ListTransferSummariesPort;

import java.util.List;
import java.util.UUID;

@Tag(name = "Transfers", description = "P2P card transfer lifecycle: initiate → OTP confirm → completed/failed")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final InitiateTransferPort initiateTransferPort;
    private final ConfirmTransferOtpPort confirmTransferOtpPort;
    private final GetTransferPort getTransferPort;
    private final ListTransfersPort listTransfersPort;
    private final ListTransferSummariesPort listTransferSummariesPort;

    public TransferController(InitiateTransferPort initiateTransferPort,
                               ConfirmTransferOtpPort confirmTransferOtpPort,
                               GetTransferPort getTransferPort,
                               ListTransfersPort listTransfersPort,
                               ListTransferSummariesPort listTransferSummariesPort) {
        this.initiateTransferPort = initiateTransferPort;
        this.confirmTransferOtpPort = confirmTransferOtpPort;
        this.getTransferPort = getTransferPort;
        this.listTransfersPort = listTransfersPort;
        this.listTransferSummariesPort = listTransferSummariesPort;
    }

    @Operation(summary = "Initiate a transfer",
               description = "Creates a transfer in `otp_pending` status. Idempotent: re-submitting the same "
                       + "`idempotencyKey` returns the cached response without creating a duplicate.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Transfer created — awaiting OTP confirmation"),
            @ApiResponse(responseCode = "400", description = "Validation error or business rule violation (limit exceeded, invalid channel, etc.)"),
            @ApiResponse(responseCode = "409", description = "Idempotency key already used with different parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    public ResponseEntity<TransferResponse> initiate(
            @Valid @RequestBody InitiateTransferRequest request,
            Authentication authentication) {
        UUID senderId = extractUserId(authentication);

        Money amount = Money.fromUzs(request.amountUzs());
        TransferChannel channel = request.channel() != null
                ? TransferChannel.valueOf(request.channel().toUpperCase())
                : TransferChannel.MOBILE_APP;

        var transfer = initiateTransferPort.initiate(
                senderId,
                request.senderInstrumentId(),
                request.senderCardNetwork(),
                request.recipientId(),
                request.recipientInstrumentId(),
                request.recipientCardNetwork(),
                amount,
                request.transferTypeId(),
                request.purposeCodeId(),
                channel,
                request.idempotencyKey());
        return ResponseEntity.accepted().body(TransferResponse.from(transfer));
    }

    @Operation(summary = "Confirm transfer OTP",
               description = "Submits the 6-digit OTP to authorise the transfer. On success the transfer moves to "
                       + "`processing`, the network call is made, ledger entries are posted, and status becomes `completed` or `failed`.")
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
        var transfer = confirmTransferOtpPort.confirmOtp(id, extractUserId(authentication), request.code());
        return ResponseEntity.ok(TransferResponse.from(transfer));
    }

    @Operation(summary = "Get transfer by ID",
               description = "Returns the current state of a transfer. Only the sender or recipient can retrieve it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer found"),
            @ApiResponse(responseCode = "404", description = "Transfer not found or not owned by caller")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransfer(
            @Parameter(description = "Transfer UUID") @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(TransferResponse.from(getTransferPort.getById(id, extractUserId(authentication))));
    }

    @Operation(summary = "List my transfers", description = "Returns all transfers initiated by the authenticated user, newest first, with participant details.")
    @ApiResponse(responseCode = "200", description = "Transfer list")
    @GetMapping
    public ResponseEntity<List<TransferResponse>> listTransfers(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        List<TransferResponse> result = listTransferSummariesPort.listSummariesByParticipant(userId)
                .stream().map(TransferResponse::from).toList();
        return ResponseEntity.ok(result);
    }

    private static UUID extractUserId(Authentication authentication) {
        String userId = (String) ((UsernamePasswordAuthenticationToken) authentication).getDetails();
        return UUID.fromString(userId);
    }
}
