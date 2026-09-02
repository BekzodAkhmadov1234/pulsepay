package uz.pulsepay.domain.settlement;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.settlement.BatchType;

@Converter(autoApply = false)
public class BatchTypeConverter implements AttributeConverter<BatchType, String> {

    @Override
    public String convertToDatabaseColumn(BatchType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public BatchType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BatchType.valueOf(dbData.toUpperCase());
    }
}
