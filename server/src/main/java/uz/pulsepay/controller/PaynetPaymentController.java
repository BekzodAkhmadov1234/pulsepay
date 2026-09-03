package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.dto.request.PaynetPrepaymentRequest;
import uz.pulsepay.dto.response.PaynetProviderResponse;
import uz.pulsepay.service.PaynetPaymentService;

import java.util.List;

@Tag(name = "Paynet / Utility Payments", description = "Browse Paynet utility providers and validate payment fields before initiating a P2S transfer")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/paynet")
public class PaynetPaymentController {

    private final PaynetPaymentService paynetPaymentService;

    public PaynetPaymentController(PaynetPaymentService paynetPaymentService) {
        this.paynetPaymentService = paynetPaymentService;
    }

    @Operation(summary = "List active Paynet utility providers",
               description = "Returns all active service providers (gas, water, electricity, etc.) with their required fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of active providers")
    })
    @GetMapping("/providers")
    public ResponseEntity<List<PaynetProviderResponse>> listProviders() {
        List<PaynetProviderResponse> providers = paynetPaymentService.listProviders()
                .stream()
                .map(PaynetProviderResponse::from)
                .toList();
        return ResponseEntity.ok(providers);
    }

    @Operation(summary = "Validate prepayment fields (stateless)",
               description = "Checks that all required fields for the chosen service are present. No DB write — call this before initiating a P2S transfer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fields valid — provider details returned"),
            @ApiResponse(responseCode = "400", description = "Unknown service or missing required fields"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping("/prepayment")
    public ResponseEntity<PaynetProviderResponse> validatePrepayment(
            @Valid @RequestBody PaynetPrepaymentRequest request) {
        var provider = paynetPaymentService.validatePrepayment(
                request.serviceCode(), request.serviceFields());
        return ResponseEntity.ok(PaynetProviderResponse.from(provider));
    }
}
