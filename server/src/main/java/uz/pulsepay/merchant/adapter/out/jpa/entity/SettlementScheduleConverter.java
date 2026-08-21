package uz.pulsepay.merchant.adapter.out.jpa.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.merchant.domain.model.SettlementSchedule;

@Converter(autoApply = false)
public class SettlementScheduleConverter implements AttributeConverter<SettlementSchedule, String> {

    @Override
    public String convertToDatabaseColumn(SettlementSchedule attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public SettlementSchedule convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        // DB stores "on_demand", enum is ON_DEMAND
        return SettlementSchedule.valueOf(dbData.toUpperCase());
    }
}
