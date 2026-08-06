package uz.pulsepay.compliance.adapter.out.jpa.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.compliance.domain.model.FlagType;

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
