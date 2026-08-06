package uz.pulsepay.ledger.adapter.out.jpa.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.ledger.domain.model.EntryDirection;

@Converter(autoApply = false)
public class EntryDirectionConverter implements AttributeConverter<EntryDirection, String> {

    @Override
    public String convertToDatabaseColumn(EntryDirection attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public EntryDirection convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EntryDirection.valueOf(dbData.toUpperCase());
    }
}
