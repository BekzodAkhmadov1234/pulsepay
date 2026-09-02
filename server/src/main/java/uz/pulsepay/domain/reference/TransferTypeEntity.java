package uz.pulsepay.domain.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.pulsepay.domain.reference.TransferType;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "transfer_types")
public class TransferTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name_uz", nullable = false)
    private String nameUz;

    @Column(name = "requires_kyc", nullable = false)
    private boolean requiresKyc;

    @Column(name = "requires_kyb", nullable = false)
    private boolean requiresKyb;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_sender_party_types", columnDefinition = "text[]")
    private String[] allowedSenderPartyTypes;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_recipient_party_types", columnDefinition = "text[]")
    private String[] allowedRecipientPartyTypes;

    @Column(name = "launch_stage", nullable = false)
    private int launchStage;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    protected TransferTypeEntity() {}

    public TransferType toDomain() {
        List<String> senderTypes = allowedSenderPartyTypes == null
                ? List.of() : Arrays.asList(allowedSenderPartyTypes);
        List<String> recipientTypes = allowedRecipientPartyTypes == null
                ? List.of() : Arrays.asList(allowedRecipientPartyTypes);
        return new TransferType(id, code, nameUz, requiresKyc, requiresKyb,
                senderTypes, recipientTypes, launchStage, isActive);
    }
}
