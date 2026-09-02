package uz.pulsepay.domain.compliance;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.compliance.FlagType;

@Converter(autoApply = false)
public class FlagTypeConverter implements AttributeConverter<FlagType, String> {

    @Override
    public String convertToDatabaseColumn(FlagType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public FlagType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FlagType.valueOf(dbData.toUpperCase());
    }
}
