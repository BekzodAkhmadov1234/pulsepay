package uz.pulsepay.dto.response;

import uz.pulsepay.domain.paynet.PaynetProvider;

import java.util.UUID;

public record PaynetProviderResponse(
        UUID id,
        String serviceCode,
        String serviceName,
        String category,
        String[] fieldNames
) {
    public static PaynetProviderResponse from(PaynetProvider p) {
        return new PaynetProviderResponse(p.id(), p.serviceCode(), p.serviceName(),
                p.category(), p.fieldNames());
    }
}
