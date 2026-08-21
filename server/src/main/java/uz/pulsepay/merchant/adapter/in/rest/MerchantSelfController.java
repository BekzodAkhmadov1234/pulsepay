package uz.pulsepay.merchant.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uz.pulsepay.card.domain.port.out.CardRepository;
import uz.pulsepay.identity.domain.model.User;
import uz.pulsepay.identity.domain.port.out.UserRepository;
import uz.pulsepay.merchant.domain.model.Merchant;
import uz.pulsepay.merchant.domain.model.MerchantAccount;
import uz.pulsepay.merchant.domain.port.in.MerchantSelfServicePort;
import uz.pulsepay.merchant.domain.port.in.VirtualTerminalPort;
import uz.pulsepay.shared.exception.NotFoundException;
import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.model.TransferSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Merchant Portal", description = "Merchant self-service and virtual terminal")
@RestController
@RequestMapping("/merchant/v1")
public class MerchantSelfController {

    private final MerchantSelfServicePort selfServicePort;
    private final VirtualTerminalPort virtualTerminalPort;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public MerchantSelfController(MerchantSelfServicePort selfServicePort,
                                   VirtualTerminalPort virtualTerminalPort,
                                   UserRepository userRepository,
                                   CardRepository cardRepository) {
        this.selfServicePort    = selfServicePort;
        this.virtualTerminalPort = virtualTerminalPort;
        this.userRepository     = userRepository;
        this.cardRepository     = cardRepository;
    }

    public record ProfileResponse(UUID id, String legalTradeName, String kybStatus,
                                   String status, String email, Instant createdAt) {
        static ProfileResponse from(Merchant m) {
            return new ProfileResponse(m.id(), m.legalTradeName(),
                    m.kybStatus().name().toLowerCase(), m.status().name().toLowerCase(),
                    m.email(), m.createdAt());
        }
    }

    public record AccountResponse(UUID id, UUID merchantId, String currencyCode,
                                   long minPayoutThreshold, String settlementSchedule,
                                   String status, Instant createdAt) {
        static AccountResponse from(MerchantAccount a) {
            return new AccountResponse(a.id(), a.merchantId(), a.currencyCode(),
                    a.minPayoutThreshold(), a.settlementSchedule().name().toLowerCase(),
                    a.status().name().toLowerCase(), a.createdAt());
        }
    }

    public record ChargeRequest(
            @NotBlank String customerPhone,
            @NotNull UUID customerInstrumentId,
            @NotBlank String cardNetwork,
            @NotNull @Positive Long amountUzs,
            Integer purposeCodeId
    ) {}

    public record ChargeResponse(UUID transferId, String status, long amountTiyin,
                                  long feeTiyin, Instant completedAt) {}

    @Operation(summary = "Get merchant profile")
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(Authentication auth) {
        UUID merchantId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ProfileResponse.from(selfServicePort.getProfile(merchantId)));
    }

    @Operation(summary = "Get merchant account details")
    @GetMapping("/account")
    public ResponseEntity<AccountResponse> getAccount(Authentication auth) {
        UUID merchantId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(AccountResponse.from(selfServicePort.getMyAccount(merchantId)));
    }

    @Operation(summary = "List C2B transfers received by this merchant")
    @GetMapping("/transfers")
    public ResponseEntity<List<TransferSummary>> getTransfers(Authentication auth) {
        UUID merchantId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(selfServicePort.getTransfers(merchantId));
    }

    public record CustomerCardSummary(UUID instrumentId, String maskedPan, String cardNetwork) {}
    public record CustomerSearchResponse(UUID id, String fullName, String phoneE164,
                                          List<CustomerCardSummary> cards) {}

    @Operation(summary = "Search for a customer by phone number (for virtual terminal)")
    @GetMapping("/customers/search")
    public ResponseEntity<CustomerSearchResponse> searchCustomer(@RequestParam String phone) {
        User user = userRepository.findByPhoneE164(phone)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("No active user found for phone: " + phone));
        List<CustomerCardSummary> cards = cardRepository.findByOwnerUserId(user.id()).stream()
                .map(c -> new CustomerCardSummary(c.id(), c.maskedPan(), c.cardNetwork()))
                .toList();
        return ResponseEntity.ok(new CustomerSearchResponse(user.id(), user.fullName(),
                user.phoneE164(), cards));
    }

    @Operation(summary = "Virtual terminal — charge a customer card")
    @PostMapping("/terminal/charge")
    public ResponseEntity<ChargeResponse> charge(@RequestBody @Valid ChargeRequest req,
                                                  Authentication auth) {
        UUID merchantId = UUID.fromString(auth.getName());
        Transfer transfer = virtualTerminalPort.charge(merchantId,
                new VirtualTerminalPort.ChargeCommand(
                        req.customerPhone(), req.customerInstrumentId(),
                        req.cardNetwork(), req.amountUzs() * 100L, req.purposeCodeId()));
        return ResponseEntity.ok(new ChargeResponse(
                transfer.id(), transfer.status().name().toLowerCase(),
                transfer.amount().amount(), transfer.feeAmount().amount(), transfer.completedAt()));
    }
}
