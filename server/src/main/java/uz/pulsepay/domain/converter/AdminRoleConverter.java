package uz.pulsepay.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.enums.AdminRole;

@Converter(autoApply = false)
public class AdminRoleConverter implements AttributeConverter<AdminRole, String> {

    @Override
    public String convertToDatabaseColumn(AdminRole attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public AdminRole convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AdminRole.valueOf(dbData.toUpperCase());
    }
}
