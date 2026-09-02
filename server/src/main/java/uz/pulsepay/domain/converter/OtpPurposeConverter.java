package uz.pulsepay.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uz.pulsepay.domain.enums.OtpPurpose;

@Converter(autoApply = false)
public class OtpPurposeConverter implements AttributeConverter<OtpPurpose, String> {

    @Override
    public String convertToDatabaseColumn(OtpPurpose attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public OtpPurpose convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OtpPurpose.valueOf(dbData.toUpperCase());
    }
}
