package uz.pulsepay.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.enums.FeeType;

@Converter(autoApply = false)
public class FeeTypeConverter implements AttributeConverter<FeeType, String> {

    @Override
    public String convertToDatabaseColumn(FeeType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public FeeType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FeeType.valueOf(dbData.toUpperCase());
    }
}
