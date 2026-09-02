package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.dto.request.CreateRouteRequest;
import uz.pulsepay.dto.response.RouteResponse;
import uz.pulsepay.dto.request.UpdateProcessorRequest;
import uz.pulsepay.service.RoutingService;
import uz.pulsepay.service.RoutingService.CreateRouteCommand;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin — Routes", description = "Transfer route and intermediary management (admin only)")
@RestController
@RequestMapping("/admin/v1/routes")
public class RouteAdminController {

    private final RoutingService routingService;

    public RouteAdminController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @Operation(summary = "List all transfer routes")
    @GetMapping
    public ResponseEntity<List<RouteResponse>> listAll() {
        return ResponseEntity.ok(routingService.listAll().stream().map(RouteResponse::from).toList());
    }

    @Operation(summary = "Get a single route by ID")
    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(RouteResponse.from(routingService.getRoute(id)));
    }

    @Operation(summary = "Create a new route")
    @PostMapping
    public ResponseEntity<RouteResponse> create(@RequestBody @Valid CreateRouteRequest req) {
        var cmd = new CreateRouteCommand(
                req.routeCode(), req.sourceNetwork(), req.destinationNetwork(),
                req.processorName(), req.maxAmount(), req.priority(),
                req.avgProcessingSeconds(), req.transferTypeId(),
                req.effectiveFrom(), req.effectiveTo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RouteResponse.from(routingService.createRoute(cmd)));
    }

    @Operation(summary = "Change the intermediary/processor for a route")
    @PatchMapping("/{id}/processor")
    public ResponseEntity<RouteResponse> updateProcessor(@PathVariable UUID id,
                                                          @RequestBody @Valid UpdateProcessorRequest req) {
        return ResponseEntity.ok(RouteResponse.from(routingService.updateProcessor(id, req.processorName())));
    }

    @Operation(summary = "Activate a route")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<RouteResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(RouteResponse.from(routingService.activate(id)));
    }

    @Operation(summary = "Deactivate a route")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<RouteResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(RouteResponse.from(routingService.deactivate(id)));
    }
}
