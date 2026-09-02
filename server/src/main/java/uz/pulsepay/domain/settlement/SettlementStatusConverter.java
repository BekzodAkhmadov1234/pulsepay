package uz.pulsepay.domain.settlement;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.settlement.SettlementStatus;

@Converter(autoApply = false)
public class SettlementStatusConverter implements AttributeConverter<SettlementStatus, String> {

    @Override
    public String convertToDatabaseColumn(SettlementStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public SettlementStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SettlementStatus.valueOf(dbData.toUpperCase());
    }
}
