package uz.pulsepay.merchant.adapter.out.jpa.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.merchant.domain.model.MerchantAccountStatus;

@Converter(autoApply = false)
public class MerchantAccountStatusConverter implements AttributeConverter<MerchantAccountStatus, String> {

    @Override
    public String convertToDatabaseColumn(MerchantAccountStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public MerchantAccountStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MerchantAccountStatus.valueOf(dbData.toUpperCase());
    }
}
