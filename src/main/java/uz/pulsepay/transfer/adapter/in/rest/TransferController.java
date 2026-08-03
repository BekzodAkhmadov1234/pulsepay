package uz.pulsepay.transfer.adapter.in.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.transfer.adapter.in.rest.dto.ConfirmOtpRequest;
import uz.pulsepay.transfer.adapter.in.rest.dto.InitiateTransferRequest;
import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.model.TransferChannel;
import uz.pulsepay.transfer.domain.port.in.ConfirmTransferOtpPort;
import uz.pulsepay.transfer.domain.port.in.GetTransferPort;
import uz.pulsepay.transfer.domain.port.in.InitiateTransferPort;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final InitiateTransferPort initiateTransferPort;
    private final ConfirmTransferOtpPort confirmTransferOtpPort;
    private final GetTransferPort getTransferPort;

    public TransferController(InitiateTransferPort initiateTransferPort,
                               ConfirmTransferOtpPort confirmTransferOtpPort,
                               GetTransferPort getTransferPort) {
        this.initiateTransferPort = initiateTransferPort;
        this.confirmTransferOtpPort = confirmTransferOtpPort;
        this.getTransferPort = getTransferPort;
    }

    @PostMapping
    public ResponseEntity<Transfer> initiate(
            @Valid @RequestBody InitiateTransferRequest request,
            @AuthenticationPrincipal String userId) {
        // Risk #8: parse via BigDecimal, convert to tiyin with longValueExact
        Money amount = Money.fromUzs(request.amountUzs());
        TransferChannel channel = request.channel() != null
                ? TransferChannel.valueOf(request.channel().toUpperCase())
                : TransferChannel.MOBILE_APP;

        Transfer transfer = initiateTransferPort.initiate(
                UUID.fromString(userId),
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
        return ResponseEntity.accepted().body(transfer);
    }

    @PatchMapping("/{id}/otp")
    public ResponseEntity<Transfer> confirmOtp(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmOtpRequest request,
            @AuthenticationPrincipal String userId) {
        Transfer transfer = confirmTransferOtpPort.confirmOtp(id, UUID.fromString(userId), request.code());
        return ResponseEntity.ok(transfer);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transfer> getTransfer(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(getTransferPort.getById(id, UUID.fromString(userId)));
    }
}
