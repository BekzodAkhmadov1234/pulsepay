package uz.pulsepay.domain.paynet;

import java.util.UUID;

public record PaynetProvider(
        UUID id,
        String serviceCode,
        String serviceName,
        String category,
        String[] fieldNames,
        boolean isActive,
        UUID partyId,
        UUID instrumentId,
        int sortOrder
) {}
