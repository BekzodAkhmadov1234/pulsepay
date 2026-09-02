package uz.pulsepay.domain.reference;

public record PurposeCode(
        int id,
        String code,
        String nameUz,
        Integer applicableTransferTypeId,
        boolean isRegulatoryRequired,
        boolean isActive
) {}
