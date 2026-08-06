package uz.pulsepay.card.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.card.domain.model.Card;
import uz.pulsepay.card.domain.model.CardStatus;
import uz.pulsepay.card.domain.port.in.AddCardPort;
import uz.pulsepay.card.domain.port.out.CardRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case: bind a payment card to a user account.
 *
 * <p><strong>Temporary:</strong> cards are auto-verified immediately (status=VERIFIED) because
 * no PSP gateway is integrated yet. Once PaySys / MONTRA is wired, this use case will:
 * <ol>
 *   <li>Call the PSP card-registration API to obtain a {@code cardToken}.</li>
 *   <li>Save the card as {@code UNVERIFIED}.</li>
 *   <li>Trigger an OTP SMS; transition to {@code VERIFIED} only after {@code confirmCard()} succeeds.</li>
 * </ol>
 */
@Slf4j
@Service
public class AddCardUseCase implements AddCardPort {

    /** Default stub balance: 50 000 000 UZS in tiyin. Well above the 30M UZS per-tx limit. */
    private static final long DEFAULT_BALANCE_TIYIN = 5_000_000_000L;

    private final CardRepository cardRepository;

    public AddCardUseCase(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional
    public Card addCard(UUID userId, String cardToken, String maskedPan,
                        String cardNetwork, String cardHolderName,
                        short expMonth, short expYear) {

        log.info("Add card attempt: userId={}, network={}", userId, cardNetwork);

        String resolvedNetwork = resolveNetwork(maskedPan, cardNetwork);

        boolean isFirst = cardRepository.findByOwnerUserId(userId).isEmpty();

        Instant now = Instant.now();
        Card card = new Card(
                UUID.randomUUID(),
                cardToken,
                maskedPan,
                resolvedNetwork,
                null,          // paymentNetworkId — resolved by gateway in future
                null,          // issuerBankId — resolved by gateway in future
                cardHolderName,
                expMonth,
                expYear,
                CardStatus.VERIFIED,   // Temporary: auto-verify until PSP OTP is integrated
                now,                   // verifiedAt
                isFirst,               // first card added becomes the default
                false,
                null,
                DEFAULT_BALANCE_TIYIN
        );

        Card saved = cardRepository.save(card, userId);
        log.info("Card added: cardId={}, userId={}, network={}, isDefault={}",
                saved.id(), userId, resolvedNetwork, isFirst);
        return saved;
    }

    /**
     * Detects the card network from the masked PAN BIN prefix if not explicitly supplied.
     * UzCard BIN 8600 → "uzcard"; HUMO BIN 9860 → "humo".
     */
    private static String resolveNetwork(String maskedPan, String cardNetwork) {
        if (cardNetwork != null && !cardNetwork.isBlank()) {
            return cardNetwork.toLowerCase();
        }
        if (maskedPan != null) {
            if (maskedPan.startsWith("8600")
                    || maskedPan.startsWith("5614")
                    || maskedPan.startsWith("6262")) return "uzcard";
            if (maskedPan.startsWith("9860")) return "humo";
        }
        return null;
    }
}
