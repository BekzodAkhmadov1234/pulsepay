package uz.pulsepay.identity.domain.port.in;

import java.util.UUID;

public interface RevokeSessionPort {
    void revoke(UUID sessionId, UUID requestingUserId);
}
