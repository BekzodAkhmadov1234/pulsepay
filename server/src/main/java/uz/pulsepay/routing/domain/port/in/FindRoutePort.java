package uz.pulsepay.routing.domain.port.in;

import uz.pulsepay.routing.domain.model.TransferRoute;

import java.util.Optional;
import java.util.UUID;

public interface FindRoutePort {
    Optional<TransferRoute> findById(UUID id);
}
