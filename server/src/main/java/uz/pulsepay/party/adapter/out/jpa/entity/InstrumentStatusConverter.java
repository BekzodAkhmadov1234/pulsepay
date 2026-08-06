package uz.pulsepay.party.adapter.out.jpa.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.party.domain.model.InstrumentStatus;

/**
 * Maps InstrumentStatus enum (ACTIVE, REMOVED…) ↔ lowercase DB values ('active', 'removed'…).
 * The instruments table check constraint enforces lowercase values.
 */
@Converter(autoApply = false)
public class InstrumentStatusConverter implements AttributeConverter<InstrumentStatus, String> {

    @Override
    public String convertToDatabaseColumn(InstrumentStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public InstrumentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : InstrumentStatus.valueOf(dbData.toUpperCase());
    }
}
