package uz.pulsepay.domain.transfer;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.transfer.ParticipantRole;

/**
 * Maps ParticipantRole (SENDER, RECIPIENT) ↔ lowercase DB values ('sender', 'recipient').
 */
@Converter(autoApply = false)
public class ParticipantRoleConverter implements AttributeConverter<ParticipantRole, String> {

    @Override
    public String convertToDatabaseColumn(ParticipantRole attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public ParticipantRole convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ParticipantRole.valueOf(dbData.toUpperCase());
    }
}
