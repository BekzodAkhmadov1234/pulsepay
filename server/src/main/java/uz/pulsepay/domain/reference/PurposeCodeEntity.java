package uz.pulsepay.domain.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.reference.PurposeCode;

@Entity
@Table(name = "purpose_codes")
public class PurposeCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "name_uz", nullable = false)
    private String nameUz;

    @Column(name = "applicable_transfer_type_id")
    private Integer applicableTransferTypeId;

    @Column(name = "is_regulatory_required", nullable = false)
    private boolean isRegulatoryRequired;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    protected PurposeCodeEntity() {}

    public PurposeCode toDomain() {
        return new PurposeCode(id, code, nameUz, applicableTransferTypeId, isRegulatoryRequired, isActive);
    }
}
