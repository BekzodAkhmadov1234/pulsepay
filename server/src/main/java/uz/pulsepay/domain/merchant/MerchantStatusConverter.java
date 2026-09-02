package uz.pulsepay.domain.merchant;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.merchant.MerchantStatus;

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
