package uz.pulsepay.domain.compliance;

import java.time.Instant;
import java.util.UUID;

public record ComplianceFlag(
        UUID id,
        UUID transferId,
        UUID partyId,
        FlagType flagType,
        UUID regulatoryParameterId,
        String status,
        Instant detectedAt,
        Instant resolvedAt,
        UUID resolvedByAdminId,
        String notes
) {}
