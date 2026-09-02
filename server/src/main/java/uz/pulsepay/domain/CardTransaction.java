package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class CardTransaction {

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

    protected CardTransaction() {}

    public CardTransaction(UUID id, UUID transferId, int transactionTypeId, UUID cardId,
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

    public UUID getId() { return id; }
    public UUID getTransferId() { return transferId; }
    public int getTransactionTypeId() { return transactionTypeId; }
    public UUID getCardId() { return cardId; }
    public long getAmount() { return amount; }
    public String getNetworkStatusCode() { return networkStatusCode; }
    public Integer getFailureReasonId() { return failureReasonId; }
    public String getStatus() { return status; }
    public Instant getProcessedAt() { return processedAt; }
    public Integer getPaymentNetworkId() { return paymentNetworkId; }

    public void setStatus(String status) { this.status = status; }
    public void setNetworkStatusCode(String networkStatusCode) { this.networkStatusCode = networkStatusCode; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public void setFailureReasonId(Integer failureReasonId) { this.failureReasonId = failureReasonId; }
}
