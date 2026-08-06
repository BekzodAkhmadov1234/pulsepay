package uz.pulsepay.network.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.network.domain.model.CardTransaction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class CardTransactionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "transaction_type_id", nullable = false)
    private int transactionTypeId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "network_status_code", length = 10)
    private String networkStatusCode;

    @Column(name = "failure_reason_id")
    private Integer failureReasonId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "payment_network_id")
    private Integer paymentNetworkId;

    protected CardTransactionEntity() {}

    public CardTransactionEntity(UUID id, UUID transferId, int transactionTypeId, UUID cardId,
                                  long amount, String networkStatusCode, Integer failureReasonId,
                                  String status, Instant processedAt, Integer paymentNetworkId) {
        this.id = id;
        this.transferId = transferId;
        this.transactionTypeId = transactionTypeId;
        this.cardId = cardId;
        this.amount = amount;
        this.networkStatusCode = networkStatusCode;
        this.failureReasonId = failureReasonId;
        this.status = status;
        this.processedAt = processedAt;
        this.paymentNetworkId = paymentNetworkId;
    }

    public CardTransaction toDomain() {
        return new CardTransaction(id, transferId, transactionTypeId, cardId, amount,
                networkStatusCode, failureReasonId, status, processedAt, paymentNetworkId);
    }

    public static CardTransactionEntity fromDomain(CardTransaction t) {
        return new CardTransactionEntity(t.id(), t.transferId(), t.transactionTypeId(), t.cardId(),
                t.amount(), t.networkStatusCode(), t.failureReasonId(), t.status(),
                t.processedAt(), t.paymentNetworkId());
    }
}
