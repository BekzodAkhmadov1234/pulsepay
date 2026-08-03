package uz.pulsepay.routing.domain.port.in;

import uz.pulsepay.routing.domain.model.TransferRoute;
import uz.pulsepay.shared.domain.Money;

public interface ResolveRoutePort {
    TransferRoute resolve(String sourceNetwork, String destNetwork, int transferTypeId, Money amount);
}
