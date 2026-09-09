package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.dto.request.AddCardRequest;
import uz.pulsepay.dto.request.SetCardLimitsRequest;
import uz.pulsepay.dto.request.SetCardPinRequest;
import uz.pulsepay.dto.response.CardLimitDto;
import uz.pulsepay.dto.response.CardLimitTypeDto;
import uz.pulsepay.dto.response.CardResponse;
import uz.pulsepay.dto.response.CardStatementEntry;
import uz.pulsepay.service.CardService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Cards", description = "Bind, list, and remove UzCard / HUMO cards")
@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @Operation(summary = "Bind a card")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Card bound successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    public ResponseEntity<CardResponse> addCard(@Valid @RequestBody AddCardRequest request,
                                                Authentication authentication) {
        UUID userId = extractUserId(authentication);
        var card = cardService.addCard(userId, request.cardToken(), request.maskedPan(),
                null, request.cardHolderName(), request.expMonth(), request.expYear());
        return ResponseEntity.status(HttpStatus.CREATED).body(CardResponse.from(card));
    }

    @Operation(summary = "List bound cards")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public List<CardResponse> listCards(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return cardService.listCards(userId).stream().map(CardResponse::from).toList();
    }

    @Operation(summary = "Remove a card")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Card removed"),
            @ApiResponse(responseCode = "404", description = "Card not found or does not belong to this user")
    })
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> removeCard(@PathVariable UUID cardId,
                                           Authentication authentication) {
        UUID userId = extractUserId(authentication);
        cardService.removeCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set default card")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default card updated"),
            @ApiResponse(responseCode = "404", description = "Card not found or does not belong to this user")
    })
    @PatchMapping("/{cardId}/default")
    public ResponseEntity<CardResponse> setDefault(@PathVariable UUID cardId,
                                                   Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(CardResponse.from(cardService.setDefault(cardId, userId)));
    }

    // ── Block / Unblock ───────────────────────────────────────────────────────

    @Operation(summary = "Block a card (user-initiated soft block)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card blocked"),
            @ApiResponse(responseCode = "422", description = "Card not in VERIFIED state")
    })
    @PostMapping("/{cardId}/block")
    public ResponseEntity<CardResponse> blockCard(@PathVariable UUID cardId,
                                                   Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(CardResponse.from(cardService.blockCard(cardId, userId)));
    }

    @Operation(summary = "Unblock a card (reverse user-initiated block)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card unblocked"),
            @ApiResponse(responseCode = "422", description = "Card not in INACTIVE state")
    })
    @PostMapping("/{cardId}/unblock")
    public ResponseEntity<CardResponse> unblockCard(@PathVariable UUID cardId,
                                                     Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(CardResponse.from(cardService.unblockCard(cardId, userId)));
    }

    // ── Statement ────────────────────────────────────────────────────────────

    @Operation(summary = "Get card statement (transaction history)")
    @ApiResponse(responseCode = "200", description = "Statement returned")
    @GetMapping("/{cardId}/statement")
    public List<CardStatementEntry> getStatement(
            @PathVariable UUID cardId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return cardService.getStatement(cardId, userId, startDate, endDate);
    }

    // ── Limits ────────────────────────────────────────────────────────────────

    @Operation(summary = "List all supported HUMO limit types")
    @GetMapping("/limit-types")
    public List<CardLimitTypeDto> getLimitTypes() {
        return cardService.getLimitTypes();
    }

    @Operation(summary = "Get active spending limits for a card")
    @GetMapping("/{cardId}/limits")
    public List<CardLimitDto> getLimits(@PathVariable UUID cardId,
                                         Authentication authentication) {
        return cardService.getLimits(cardId, extractUserId(authentication));
    }

    @Operation(summary = "Set spending limits on a card")
    @PostMapping("/{cardId}/limits")
    public List<CardLimitDto> setLimits(@PathVariable UUID cardId,
                                         @Valid @RequestBody SetCardLimitsRequest request,
                                         Authentication authentication) {
        return cardService.setLimits(cardId, extractUserId(authentication), request.limits());
    }

    @Operation(summary = "Remove a single spending limit from a card")
    @ApiResponse(responseCode = "204", description = "Limit removed")
    @DeleteMapping("/{cardId}/limits/{limitType}")
    public ResponseEntity<Void> removeLimit(@PathVariable UUID cardId,
                                             @PathVariable String limitType,
                                             Authentication authentication) {
        cardService.removeLimit(cardId, extractUserId(authentication), limitType);
        return ResponseEntity.noContent().build();
    }

    // ── PIN change ────────────────────────────────────────────────────────────

    @Operation(summary = "Change PIN for a HUMO card")
    @ApiResponse(responseCode = "200", description = "PIN changed successfully")
    @PostMapping("/{cardId}/pin/humo")
    public ResponseEntity<Map<String, String>> setPinHumo(
            @PathVariable UUID cardId,
            @Valid @RequestBody SetCardPinRequest request,
            Authentication authentication) {
        cardService.setPinHumo(cardId, extractUserId(authentication));
        return ResponseEntity.ok(Map.of("message", "PIN changed successfully"));
    }

    @Operation(summary = "Change PIN for a UzCard card")
    @ApiResponse(responseCode = "200", description = "PIN changed successfully")
    @PostMapping("/{cardId}/pin/uzcard")
    public ResponseEntity<Map<String, String>> setPinUzcard(
            @PathVariable UUID cardId,
            @Valid @RequestBody SetCardPinRequest request,
            Authentication authentication) {
        cardService.setPinUzcard(cardId, extractUserId(authentication));
        return ResponseEntity.ok(Map.of("message", "PIN changed successfully"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static UUID extractUserId(Authentication authentication) {
        String raw = (String) ((UsernamePasswordAuthenticationToken) authentication).getDetails();
        return UUID.fromString(raw);
    }
}
