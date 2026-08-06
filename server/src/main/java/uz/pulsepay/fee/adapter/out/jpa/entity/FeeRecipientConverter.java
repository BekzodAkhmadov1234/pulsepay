package uz.pulsepay.fee.adapter.out.jpa.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.fee.domain.model.FeeRecipient;

@Converter(autoApply = false)
public class FeeRecipientConverter implements AttributeConverter<FeeRecipient, String> {

    @Override
    public String convertToDatabaseColumn(FeeRecipient attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public FeeRecipient convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FeeRecipient.valueOf(dbData.toUpperCase());
    }
}
