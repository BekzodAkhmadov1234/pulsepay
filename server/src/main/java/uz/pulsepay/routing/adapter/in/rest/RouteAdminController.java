package uz.pulsepay.routing.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.pulsepay.routing.adapter.in.rest.dto.CreateRouteRequest;
import uz.pulsepay.routing.adapter.in.rest.dto.RouteResponse;
import uz.pulsepay.routing.adapter.in.rest.dto.UpdateProcessorRequest;
import uz.pulsepay.routing.domain.port.in.ManageRoutePort;
import uz.pulsepay.routing.domain.port.in.ManageRoutePort.CreateRouteCommand;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin — Routes", description = "Transfer route and intermediary management (admin only)")
@RestController
@RequestMapping("/admin/v1/routes")
public class RouteAdminController {

    private final ManageRoutePort manageRoutePort;

    public RouteAdminController(ManageRoutePort manageRoutePort) {
        this.manageRoutePort = manageRoutePort;
    }

    @Operation(summary = "List all transfer routes")
    @GetMapping
    public ResponseEntity<List<RouteResponse>> listAll() {
        return ResponseEntity.ok(
                manageRoutePort.listAll().stream().map(RouteResponse::from).toList());
    }

    @Operation(summary = "Get a single route by ID")
    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(RouteResponse.from(manageRoutePort.getRoute(id)));
    }

    @Operation(summary = "Create a new route with a specific intermediary/processor")
    @PostMapping
    public ResponseEntity<RouteResponse> create(@RequestBody @Valid CreateRouteRequest req) {
        var cmd = new CreateRouteCommand(
                req.routeCode(), req.sourceNetwork(), req.destinationNetwork(),
                req.processorName(), req.maxAmount(), req.priority(),
                req.avgProcessingSeconds(), req.transferTypeId(),
                req.effectiveFrom(), req.effectiveTo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RouteResponse.from(manageRoutePort.createRoute(cmd)));
    }

    @Operation(summary = "Change the intermediary/processor for a route")
    @PatchMapping("/{id}/processor")
    public ResponseEntity<RouteResponse> updateProcessor(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateProcessorRequest req) {
        return ResponseEntity.ok(
                RouteResponse.from(manageRoutePort.updateProcessor(id, req.processorName())));
    }

    @Operation(summary = "Activate a route")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<RouteResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(RouteResponse.from(manageRoutePort.activate(id)));
    }

    @Operation(summary = "Deactivate a route")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<RouteResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(RouteResponse.from(manageRoutePort.deactivate(id)));
    }
}
