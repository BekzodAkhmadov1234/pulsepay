package uz.pulsepay.card.adapter.in.rest;

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
import uz.pulsepay.card.adapter.in.rest.dto.AddCardRequest;
import uz.pulsepay.card.adapter.in.rest.dto.CardResponse;
import uz.pulsepay.card.domain.port.in.AddCardPort;
import uz.pulsepay.card.domain.port.in.ListCardsPort;
import uz.pulsepay.card.domain.port.in.RemoveCardPort;
import uz.pulsepay.card.domain.port.in.SetDefaultCardPort;

import java.util.List;
import java.util.UUID;

@Tag(name = "Cards", description = "Bind, list, and remove UzCard / HUMO cards")
@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final AddCardPort        addCardPort;
    private final ListCardsPort      listCardsPort;
    private final RemoveCardPort     removeCardPort;
    private final SetDefaultCardPort setDefaultCardPort;

    public CardController(AddCardPort addCardPort,
                          ListCardsPort listCardsPort,
                          RemoveCardPort removeCardPort,
                          SetDefaultCardPort setDefaultCardPort) {
        this.addCardPort        = addCardPort;
        this.listCardsPort      = listCardsPort;
        this.removeCardPort     = removeCardPort;
        this.setDefaultCardPort = setDefaultCardPort;
    }

    @Operation(summary = "Bind a card", description = """
            Adds a UzCard (BIN 8600) or HUMO (BIN 9860) card to the authenticated user's account.
            The card network is auto-detected from the masked PAN BIN prefix.

            **Temporary:** cards are immediately VERIFIED — OTP confirmation via PSP will be
            re-inserted when a gateway is integrated.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Card bound successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    public ResponseEntity<CardResponse> addCard(
            @Valid @RequestBody AddCardRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);

        var card = addCardPort.addCard(
                userId,
                request.cardToken(),
                request.maskedPan(),
                null,                    // cardNetwork — auto-detected in use case from BIN
                request.cardHolderName(),
                request.expMonth(),
                request.expYear()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(CardResponse.from(card));
    }

    @Operation(summary = "List bound cards", description = "Returns all active (non-removed) cards for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public List<CardResponse> listCards(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return listCardsPort.listCards(userId).stream().map(CardResponse::from).toList();
    }

    @Operation(summary = "Remove a card", description = "Soft-deletes a card from the user's account. The card cannot be used for transfers after removal.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Card removed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Card not found or does not belong to this user")
    })
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> removeCard(@PathVariable UUID cardId,
                                           Authentication authentication) {
        UUID userId = extractUserId(authentication);
        removeCardPort.removeCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set default card", description = "Marks the given card as the user's default, clearing the default flag from all other cards.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default card updated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Card not found or does not belong to this user")
    })
    @PatchMapping("/{cardId}/default")
    public ResponseEntity<CardResponse> setDefault(@PathVariable UUID cardId,
                                                   Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(CardResponse.from(setDefaultCardPort.setDefault(cardId, userId)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static UUID extractUserId(Authentication authentication) {
        String raw = (String) ((UsernamePasswordAuthenticationToken) authentication).getDetails();
        return UUID.fromString(raw);
    }
}
