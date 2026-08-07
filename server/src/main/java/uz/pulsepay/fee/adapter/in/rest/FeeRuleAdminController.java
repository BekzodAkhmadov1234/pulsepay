package uz.pulsepay.fee.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.pulsepay.fee.adapter.in.rest.dto.AddTierRequest;
import uz.pulsepay.fee.adapter.in.rest.dto.CreateFeeRuleRequest;
import uz.pulsepay.fee.adapter.in.rest.dto.FeeRuleResponse;
import uz.pulsepay.fee.adapter.in.rest.dto.FeeRuleTierResponse;
import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.fee.domain.model.FeeRuleTier;
import uz.pulsepay.fee.domain.port.in.ManageFeeRulePort;
import uz.pulsepay.fee.domain.port.in.ManageFeeRulePort.AddTierCommand;
import uz.pulsepay.fee.domain.port.in.ManageFeeRulePort.CreateFeeRuleCommand;
import uz.pulsepay.fee.domain.port.out.FeeRuleTierRepository;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin — Fee Rules", description = "Fee rule lifecycle management (admin only)")
@RestController
@RequestMapping("/admin/v1/fee-rules")
public class FeeRuleAdminController {

    private final ManageFeeRulePort manageFeeRulePort;
    private final FeeRuleTierRepository feeRuleTierRepository;

    public FeeRuleAdminController(ManageFeeRulePort manageFeeRulePort,
                                   FeeRuleTierRepository feeRuleTierRepository) {
        this.manageFeeRulePort    = manageFeeRulePort;
        this.feeRuleTierRepository = feeRuleTierRepository;
    }

    @Operation(summary = "Create a new fee rule")
    @PostMapping
    public ResponseEntity<FeeRuleResponse> create(@RequestBody @Valid CreateFeeRuleRequest request) {
        FeeRule rule = manageFeeRulePort.createRule(toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(rule));
    }

    @Operation(summary = "List all fee rules (active and inactive)")
    @GetMapping
    public ResponseEntity<List<FeeRuleResponse>> listAll() {
        List<FeeRuleResponse> rules = manageFeeRulePort.listAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(rules);
    }

    @Operation(summary = "Get one fee rule with its tiers")
    @GetMapping("/{id}")
    public ResponseEntity<FeeRuleResponse> getOne(@PathVariable UUID id) {
        FeeRule rule = manageFeeRulePort.getRule(id);
        return ResponseEntity.ok(toResponse(rule));
    }

    @Operation(summary = "Soft-deactivate a fee rule")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<FeeRuleResponse> deactivate(@PathVariable UUID id) {
        FeeRule rule = manageFeeRulePort.deactivate(id);
        return ResponseEntity.ok(toResponse(rule));
    }

    @Operation(summary = "Supersede a fee rule — deactivates old and creates new replacement atomically")
    @PutMapping("/{id}/supersede")
    public ResponseEntity<FeeRuleResponse> supersede(
            @PathVariable UUID id,
            @RequestBody @Valid CreateFeeRuleRequest request) {
        FeeRule rule = manageFeeRulePort.supersede(id, toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(rule));
    }

    @Operation(summary = "Add a tier to a TIERED fee rule")
    @PostMapping("/{id}/tiers")
    public ResponseEntity<FeeRuleTierResponse> addTier(
            @PathVariable UUID id,
            @RequestBody @Valid AddTierRequest request) {
        FeeRuleTier tier = manageFeeRulePort.addTier(id, toTierCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(FeeRuleTierResponse.from(tier));
    }

    @Operation(summary = "Remove a tier from a TIERED fee rule")
    @DeleteMapping("/{id}/tiers/{tierId}")
    public ResponseEntity<Void> removeTier(@PathVariable UUID id, @PathVariable UUID tierId) {
        manageFeeRulePort.removeTier(id, tierId);
        return ResponseEntity.noContent().build();
    }

    // ─── mapping helpers ──────────────────────────────────────────────────────

    private FeeRuleResponse toResponse(FeeRule rule) {
        List<FeeRuleTierResponse> tiers = feeRuleTierRepository.findByFeeRuleId(rule.id())
                .stream().map(FeeRuleTierResponse::from).toList();
        return FeeRuleResponse.from(rule, tiers);
    }

    private static CreateFeeRuleCommand toCommand(CreateFeeRuleRequest r) {
        List<AddTierCommand> tiers = r.tiers() == null ? null :
                r.tiers().stream().map(FeeRuleAdminController::toTierCommand).toList();
        return new CreateFeeRuleCommand(
                r.name(), r.sourceNetwork(), r.destinationNetwork(),
                r.minAmount(), r.maxAmount(),
                r.feeType(), r.fixedAmount(), r.percentageBps(),
                r.minFeeAmount(), r.maxFeeAmount(),
                r.currencyCode(), r.priority(),
                r.effectiveFrom(), r.effectiveTo(),
                r.transferTypeId(), r.feePayer(), r.feeRecipient(), tiers);
    }

    private static AddTierCommand toTierCommand(AddTierRequest r) {
        return new AddTierCommand(r.tierMinAmount(), r.tierMaxAmount(),
                r.fixedAmount(), r.percentageBps());
    }
}
