package uz.pulsepay.dto.response;

public record PaynetCategoryResponse(
        String category,
        String displayName,
        int providerCount
) {}
