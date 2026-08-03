package uz.pulsepay.reference.domain.model;

public record PurposeCode(
        int id,
        String code,
        String nameUz,
        Integer applicableTransferTypeId,
        boolean isRegulatoryRequired,
        boolean isActive
) {}
