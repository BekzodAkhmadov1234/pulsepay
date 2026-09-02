package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "transfer_types")
public class TransferType {

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

    protected TransferType() {}

    public int getId() { return id; }
    public String getCode() { return code; }
    public String getNameUz() { return nameUz; }
    public boolean isRequiresKyc() { return requiresKyc; }
    public boolean isRequiresKyb() { return requiresKyb; }
    public List<String> getAllowedSenderPartyTypes() {
        return allowedSenderPartyTypes == null ? List.of() : Arrays.asList(allowedSenderPartyTypes);
    }
    public List<String> getAllowedRecipientPartyTypes() {
        return allowedRecipientPartyTypes == null ? List.of() : Arrays.asList(allowedRecipientPartyTypes);
    }
    public int getLaunchStage() { return launchStage; }
    public boolean isActive() { return isActive; }
}
