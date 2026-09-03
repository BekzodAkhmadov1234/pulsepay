package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.dto.request.MobileTopUpRequest;
import uz.pulsepay.dto.request.PaynetPrepaymentRequest;
import uz.pulsepay.dto.response.PaynetCategoryResponse;
import uz.pulsepay.dto.response.PaynetProviderResponse;
import uz.pulsepay.service.PaynetPaymentService;

import java.util.List;

@Tag(name = "Paynet / Utility Payments", description = "Browse providers by category, search, popular picks, and mobile top-up shortcut")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/paynet")
public class PaynetPaymentController {

    private final PaynetPaymentService paynetPaymentService;

    public PaynetPaymentController(PaynetPaymentService paynetPaymentService) {
        this.paynetPaymentService = paynetPaymentService;
    }

    // ── Provider listing ──────────────────────────────────────────────────────

    @Operation(summary = "List active Paynet providers",
               description = "Returns all active providers sorted by display order. " +
                             "Pass `?category=gas` to filter by category.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Provider list")
    })
    @GetMapping("/providers")
    public ResponseEntity<List<PaynetProviderResponse>> listProviders(
            @Parameter(description = "Optional category filter (gas, water, electricity, mobile, internet)")
            @RequestParam(required = false) String category) {

        List<PaynetProviderResponse> list = (category != null && !category.isBlank()
                ? paynetPaymentService.listByCategory(category)
                : paynetPaymentService.listProviders())
                .stream()
                .map(PaynetProviderResponse::from)
                .toList();

        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Search providers by name",
               description = "Case-insensitive substring match on service_name. Returns all providers when query is blank.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching providers")
    })
    @GetMapping("/providers/search")
    public ResponseEntity<List<PaynetProviderResponse>> search(
            @Parameter(description = "Search keyword (e.g. 'gaz', 'suv', 'uzm')") @RequestParam String q) {

        return ResponseEntity.ok(
                paynetPaymentService.searchProviders(q).stream()
                        .map(PaynetProviderResponse::from)
                        .toList());
    }

    @Operation(summary = "Popular providers",
               description = "Returns the top N providers by display order (default 5, max 20).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Popular provider list")
    })
    @GetMapping("/providers/popular")
    public ResponseEntity<List<PaynetProviderResponse>> popular(
            @Parameter(description = "How many to return (1–20)") @RequestParam(defaultValue = "5") int count) {

        return ResponseEntity.ok(
                paynetPaymentService.listPopular(count).stream()
                        .map(PaynetProviderResponse::from)
                        .toList());
    }

    // ── Categories ────────────────────────────────────────────────────────────

    @Operation(summary = "List provider categories",
               description = "Returns distinct categories derived from active providers, with localised display names and provider counts.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category list")
    })
    @GetMapping("/categories")
    public ResponseEntity<List<PaynetCategoryResponse>> listCategories() {
        return ResponseEntity.ok(paynetPaymentService.listCategories());
    }

    // ── Prepayment & mobile shortcut ──────────────────────────────────────────

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

    @Operation(summary = "Mobile top-up shortcut",
               description = "Validates a phone number against the default mobile top-up provider (UzMobile). " +
                             "Optionally pass `serviceCode` to target a different mobile operator. " +
                             "On success, proceed with a normal P2S transfer using the returned provider.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Phone valid — provider details returned"),
            @ApiResponse(responseCode = "400", description = "Invalid phone or unknown service"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping("/mobile")
    public ResponseEntity<PaynetProviderResponse> mobileTopUp(
            @Valid @RequestBody MobileTopUpRequest request) {
        var provider = paynetPaymentService.validateMobileTopUp(request.phone(), request.serviceCode());
        return ResponseEntity.ok(PaynetProviderResponse.from(provider));
    }
}
