package uz.pulsepay.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.enums.TransferChannel;

/**
 * Maps TransferChannel ↔ lowercase DB values ('mobile_app', 'pos', 'e_pos', 'api', 'web').
 */
@Converter(autoApply = false)
public class TransferChannelConverter implements AttributeConverter<TransferChannel, String> {

    @Override
    public String convertToDatabaseColumn(TransferChannel attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public TransferChannel convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TransferChannel.valueOf(dbData.toUpperCase());
    }
}
