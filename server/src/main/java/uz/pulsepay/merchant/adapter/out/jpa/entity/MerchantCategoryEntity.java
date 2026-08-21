package uz.pulsepay.merchant.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.merchant.domain.model.MerchantCategory;

@Entity
@Table(name = "merchant_categories")
public class MerchantCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "mcc_code", nullable = false, unique = true, length = 4)
    private String mccCode;

    @Column(name = "name_uz")
    private String nameUz;

    @Column(name = "risk_tier", nullable = false, length = 10)
    private String riskTier;

    protected MerchantCategoryEntity() {}

    public MerchantCategory toDomain() {
        return new MerchantCategory(id, mccCode, nameUz, riskTier);
    }
}
