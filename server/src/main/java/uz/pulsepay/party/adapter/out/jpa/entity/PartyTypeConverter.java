package uz.pulsepay.party.adapter.out.jpa.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.party.domain.model.PartyType;

/**
 * Maps PartyType (PERSON, BUSINESS, MERCHANT) ↔ lowercase DB values ('person', 'business', 'merchant').
 */
@Converter(autoApply = false)
public class PartyTypeConverter implements AttributeConverter<PartyType, String> {

    @Override
    public String convertToDatabaseColumn(PartyType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public PartyType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PartyType.valueOf(dbData.toUpperCase());
    }
}
