package uz.pulsepay.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.enums.InstrumentType;

/**
 * Maps InstrumentType enum (CARD, BANK_ACCOUNT…) ↔ lowercase DB values ('card', 'bank_account'…).
 * The instruments table check constraint enforces lowercase values.
 */
@Converter(autoApply = false)
public class InstrumentTypeConverter implements AttributeConverter<InstrumentType, String> {

    @Override
    public String convertToDatabaseColumn(InstrumentType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public InstrumentType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : InstrumentType.valueOf(dbData.toUpperCase());
    }
}
