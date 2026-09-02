package uz.pulsepay.utils.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.pulsepay.service.CardBalanceService;
import uz.pulsepay.domain.shared.Money;

import java.util.UUID;

/**
 * Stub bank transfer gateway for P2A (person-to-account) transfers.
 * Debits the sender's card shadow balance locally; stubs the bank-rail credit
 * until a real interbank API (SWIFT/SEPA/UzBankNet) is wired.
 */
@Slf4j
@Component
public class BankTransferGateway implements CardNetworkGateway {

    private final CardBalanceService cardBalanceService;

    public BankTransferGateway(CardBalanceService cardBalanceService) {
        this.cardBalanceService = cardBalanceService;
    }

    @Override
    public String networkCode() {
        return "stub_bank";
    }

    @Override
    public String debitCard(UUID cardId, Money amount, String referenceId) {
        String ref = "bank_debit_" + referenceId;
        log.info("[STUB] Bank gateway debit card: cardId={}, amount={}, ref={}", cardId, amount, ref);
        cardBalanceService.debit(cardId, amount.amount());
        return ref;
    }

    @Override
    public String creditCard(UUID bankAccountInstrumentId, Money amount, String referenceId) {
        String ref = "bank_credit_" + referenceId;
        log.info("[STUB] Bank gateway credit bank account: instrumentId={}, amount={}, ref={}",
                bankAccountInstrumentId, amount, ref);
        // Real integration: call UzBankNet/interbank rail API here
        return ref;
    }

    @Override
    public void reverseTransaction(String networkTransactionId) {
        log.info("[STUB] Bank gateway reversal: txId={}", networkTransactionId);
    }

    @Override
    public boolean isCardNetwork() { return false; }
}
