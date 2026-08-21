package uz.pulsepay.merchant.adapter.out.jpa.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.merchant.domain.model.KybStatus;

@Converter(autoApply = false)
public class KybStatusConverter implements AttributeConverter<KybStatus, String> {

    @Override
    public String convertToDatabaseColumn(KybStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public KybStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : KybStatus.valueOf(dbData.toUpperCase());
    }
}
