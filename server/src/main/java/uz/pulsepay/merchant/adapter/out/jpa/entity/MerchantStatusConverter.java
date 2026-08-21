package uz.pulsepay.merchant.adapter.out.jpa.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.merchant.domain.model.MerchantStatus;

@Converter(autoApply = false)
public class MerchantStatusConverter implements AttributeConverter<MerchantStatus, String> {

    @Override
    public String convertToDatabaseColumn(MerchantStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public MerchantStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MerchantStatus.valueOf(dbData.toUpperCase());
    }
}
