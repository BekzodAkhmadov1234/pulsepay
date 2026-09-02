package uz.pulsepay.domain.reference;

import java.util.List;

public record TransferType(
        int id,
        String code,
        String nameUz,
        boolean requiresKyc,
        boolean requiresKyb,
        List<String> allowedSenderPartyTypes,
        List<String> allowedRecipientPartyTypes,
        int launchStage,
        boolean isActive
) {}
