package uz.pulsepay.domain.paynet;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists String[] as a comma-separated VARCHAR in the database.
 * e.g. ["account_number","reference"] ↔ "account_number,reference"
 */
@Converter(autoApply = false)
public class StringArrayConverter implements AttributeConverter<String[], String> {

    @Override
    public String convertToDatabaseColumn(String[] attribute) {
        if (attribute == null || attribute.length == 0) return "";
        return String.join(",", attribute);
    }

    @Override
    public String[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new String[0];
        return dbData.split(",");
    }
}
