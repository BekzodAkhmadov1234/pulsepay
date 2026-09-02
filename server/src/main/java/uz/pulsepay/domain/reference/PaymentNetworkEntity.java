package uz.pulsepay.domain.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.reference.PaymentNetwork;

import java.time.Instant;

@Entity
@Table(name = "payment_networks")
public class PaymentNetworkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "tin", length = 9)
    private String tin;

    @Column(name = "settlement_system", length = 30)
    private String settlementSystem;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentNetworkEntity() {}

    public PaymentNetwork toDomain() {
        return new PaymentNetwork(id, code, legalName, tin, settlementSystem, isActive, createdAt);
    }
}
