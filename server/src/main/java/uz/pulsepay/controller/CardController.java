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
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.dto.request.AddCardRequest;
import uz.pulsepay.dto.response.CardResponse;
import uz.pulsepay.service.CardService;

import java.util.List;
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

    private static UUID extractUserId(Authentication authentication) {
        String raw = (String) ((UsernamePasswordAuthenticationToken) authentication).getDetails();
        return UUID.fromString(raw);
    }
}
