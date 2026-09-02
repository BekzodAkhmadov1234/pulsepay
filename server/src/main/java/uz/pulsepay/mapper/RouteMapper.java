package uz.pulsepay.mapper;

import org.springframework.stereotype.Component;
import uz.pulsepay.dto.response.RouteResponse;
import uz.pulsepay.domain.routing.TransferRoute;

@Component
public class RouteMapper {

    public RouteResponse toResponse(TransferRoute route) {
        return new RouteResponse(
                route.id(), route.routeCode(), route.sourceNetwork(), route.destinationNetwork(),
                route.processorName(), route.maxAmount(), route.priority(), route.avgProcessingSeconds(),
                route.isActive(), route.transferTypeId(), route.effectiveFrom(), route.effectiveTo()
        );
    }
}
