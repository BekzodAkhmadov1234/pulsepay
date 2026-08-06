package uz.pulsepay.card.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.card.domain.model.Card;
import uz.pulsepay.card.domain.port.out.CardRepository;
import uz.pulsepay.identity.domain.model.User;
import uz.pulsepay.identity.domain.port.out.UserRepository;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Recipient lookup for P2P transfer initiation.
 * Lives in the card module because both UserRepository (identity) and
 * CardRepository (card) are needed — and card may import identity.
 */
@Tag(name = "Recipients", description = "Look up a recipient by phone number or card for P2P transfer")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/recipients")
public class RecipientController {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public RecipientController(UserRepository userRepository, CardRepository cardRepository) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }

    @Operation(summary = "Find recipient by phone or card number",
               description = "Looks up an active user by E.164 phone number or 16-digit card number and returns their name and cards.")
    @GetMapping("/search")
    public ResponseEntity<RecipientDto> search(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String cardNumber) {

        if (phone != null) {
            User user = userRepository.findByPhoneE164(phone)
                    .filter(User::isActive)
                    .orElseThrow(() -> new NotFoundException("No active user found for phone: " + phone));

            List<CardSummary> cards = cardRepository.findByOwnerUserId(user.id()).stream()
                    .map(c -> new CardSummary(c.id(), c.maskedPan(), c.cardNetwork(), c.isDefault()))
                    .toList();

            return ResponseEntity.ok(new RecipientDto(user.id(), user.fullName(), user.phoneE164(), cards));

        } else if (cardNumber != null && cardNumber.length() >= 10) {
            String first6 = cardNumber.substring(0, 6);
            String last4 = cardNumber.substring(cardNumber.length() - 4);

            Card card = cardRepository.findByMaskedPanPattern(first6, last4)
                    .orElseThrow(() -> new NotFoundException("No card found for that number"));

            UUID ownerPartyId = cardRepository.findOwnerIdByCardId(card.id())
                    .orElseThrow(() -> new NotFoundException("Card owner not found"));

            User user = userRepository.findById(ownerPartyId)
                    .filter(User::isActive)
                    .orElseThrow(() -> new NotFoundException("No active user found for card owner"));

            List<CardSummary> cards = cardRepository.findByOwnerUserId(user.id()).stream()
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
