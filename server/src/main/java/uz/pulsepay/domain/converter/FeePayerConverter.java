package uz.pulsepay.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.enums.FeePayer;

@Converter(autoApply = false)
public class FeePayerConverter implements AttributeConverter<FeePayer, String> {

    @Override
    public String convertToDatabaseColumn(FeePayer attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public FeePayer convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FeePayer.valueOf(dbData.toUpperCase());
    }
}
