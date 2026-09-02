package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.domain.card.Card;
import uz.pulsepay.domain.identity.User;
import uz.pulsepay.service.CardService;
import uz.pulsepay.service.UserAuthService;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;

import java.util.List;
import java.util.UUID;

@Tag(name = "Recipients", description = "Look up a recipient by phone number or card for P2P transfer")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/recipients")
public class RecipientController {

    private final UserAuthService userAuthService;
    private final CardService cardService;

    public RecipientController(UserAuthService userAuthService,
                               CardService cardService) {
        this.userAuthService = userAuthService;
        this.cardService     = cardService;
    }

    @Operation(summary = "Find recipient by phone or card number")
    @GetMapping("/search")
    public ResponseEntity<RecipientDto> search(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String cardNumber) {

        if (phone != null) {
            User user = userAuthService.findByPhone(phone);
            if (!user.isActive()) throw new NotFoundException("No active user found for phone: " + phone);

            List<CardSummary> cards = cardService.listCards(user.id()).stream()
                    .map(c -> new CardSummary(c.id(), c.maskedPan(), c.cardNetwork(), c.isDefault()))
                    .toList();

            return ResponseEntity.ok(new RecipientDto(user.id(), user.fullName(), user.phoneE164(), cards));

        } else if (cardNumber != null && cardNumber.length() >= 10) {
            String first6 = cardNumber.substring(0, 6);
            String last4  = cardNumber.substring(cardNumber.length() - 4);

            Card card = cardService.findByMaskedPanPattern(first6, last4).stream()
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("No card found for that number"));

            UUID ownerPartyId = cardService.findOwnerIdByCardId(card.id());

            User user = userAuthService.findById(ownerPartyId);
            if (!user.isActive()) throw new NotFoundException("No active user found for card owner");

            List<CardSummary> cards = cardService.listCards(user.id()).stream()
                    .map(c -> new CardSummary(c.id(), c.maskedPan(), c.cardNetwork(), c.isDefault()))
                    .toList();

            return ResponseEntity.ok(new RecipientDto(user.id(), user.fullName(), user.phoneE164(), cards));

        } else {
            throw new DomainException("Provide either phone or cardNumber");
        }
    }

    public record RecipientDto(UUID id, String fullName, String phoneE164, List<CardSummary> cards) {}
    public record CardSummary(UUID id, String maskedPan, String cardNetwork, boolean isDefault) {}
}
