package uz.pulsepay.network.domain.port.in;

import uz.pulsepay.shared.domain.Money;

import java.util.UUID;

public interface ExecuteCardTransferPort {
    /**
     * Executes the debit leg (sender's card) and credit leg (recipient's card)
     * via the appropriate card network gateway.
     *
     * @param transferId      parent transfer ID
     * @param senderCardId    sender instrument (card)
     * @param recipientCardId recipient instrument (card)
     * @param debitAmount     total amount debited from sender (principal + fee when sender-pays)
     * @param creditAmount    amount credited to recipient (principal only)
     * @param routeCode       which network route to use
     */
    void execute(UUID transferId, UUID senderCardId, UUID recipientCardId,
                 Money debitAmount, Money creditAmount, String routeCode);
}
