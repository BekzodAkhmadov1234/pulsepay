package uz.pulsepay.identity.domain.port.in;

import uz.pulsepay.identity.domain.model.Session;

public interface RefreshSessionPort {
    Session refresh(String rawRefreshToken);
}
