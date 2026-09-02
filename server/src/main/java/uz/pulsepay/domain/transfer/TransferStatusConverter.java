package uz.pulsepay.domain.transfer;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.transfer.TransferStatus;

/**
 * Maps TransferStatus ↔ lowercase DB values ('initiated', 'otp_pending', etc.).
 */
@Converter(autoApply = false)
public class TransferStatusConverter implements AttributeConverter<TransferStatus, String> {

    @Override
    public String convertToDatabaseColumn(TransferStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public TransferStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TransferStatus.valueOf(dbData.toUpperCase());
    }
}
