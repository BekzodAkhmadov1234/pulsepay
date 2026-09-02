package uz.pulsepay.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.enums.EntryDirection;

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
