package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.dto.request.AddTierRequest;
import uz.pulsepay.dto.request.CreateFeeRuleRequest;
import uz.pulsepay.dto.response.FeeRuleResponse;
import uz.pulsepay.dto.response.FeeRuleTierResponse;
import uz.pulsepay.domain.fee.FeeRule;
import uz.pulsepay.domain.fee.FeeRuleTier;
import uz.pulsepay.service.FeeService;
import uz.pulsepay.service.FeeService.AddTierCommand;
import uz.pulsepay.service.FeeService.CreateFeeRuleCommand;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin — Fee Rules", description = "Fee rule lifecycle management (admin only)")
@RestController
@RequestMapping("/admin/v1/fee-rules")
public class FeeRuleAdminController {

    private final FeeService feeService;

    public FeeRuleAdminController(FeeService feeService) {
        this.feeService = feeService;
    }

    @Operation(summary = "Create a new fee rule")
    @PostMapping
    public ResponseEntity<FeeRuleResponse> create(@RequestBody @Valid CreateFeeRuleRequest request) {
        FeeRule rule = feeService.createRule(toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(rule));
    }

    @Operation(summary = "List all fee rules (active and inactive)")
    @GetMapping
    public ResponseEntity<List<FeeRuleResponse>> listAll() {
        List<FeeRuleResponse> rules = feeService.listAll().stream()
                .map(this::toResponse).toList();
        return ResponseEntity.ok(rules);
    }

    @Operation(summary = "Get one fee rule with its tiers")
    @GetMapping("/{id}")
    public ResponseEntity<FeeRuleResponse> getOne(@PathVariable UUID id) {
        FeeRule rule = feeService.getRule(id);
        return ResponseEntity.ok(toResponse(rule));
    }

    @Operation(summary = "Soft-deactivate a fee rule")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<FeeRuleResponse> deactivate(@PathVariable UUID id) {
        FeeRule rule = feeService.deactivate(id);
        return ResponseEntity.ok(toResponse(rule));
    }

    @Operation(summary = "Re-activate a fee rule")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<FeeRuleResponse> activate(@PathVariable UUID id) {
        FeeRule rule = feeService.activate(id);
        return ResponseEntity.ok(toResponse(rule));
    }

    @Operation(summary = "Supersede a fee rule")
    @PutMapping("/{id}/supersede")
    public ResponseEntity<FeeRuleResponse> supersede(@PathVariable UUID id,
                                                      @RequestBody @Valid CreateFeeRuleRequest request) {
        FeeRule rule = feeService.supersede(id, toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(rule));
    }

    @Operation(summary = "Add a tier to a TIERED fee rule")
    @PostMapping("/{id}/tiers")
    public ResponseEntity<FeeRuleTierResponse> addTier(@PathVariable UUID id,
                                                        @RequestBody @Valid AddTierRequest request) {
        FeeRuleTier tier = feeService.addTier(id, toTierCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(FeeRuleTierResponse.from(tier));
    }

    @Operation(summary = "Remove a tier from a TIERED fee rule")
    @DeleteMapping("/{id}/tiers/{tierId}")
    public ResponseEntity<Void> removeTier(@PathVariable UUID id, @PathVariable UUID tierId) {
        feeService.removeTier(id, tierId);
        return ResponseEntity.noContent().build();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private FeeRuleResponse toResponse(FeeRule rule) {
        List<FeeRuleTierResponse> tiers = feeService.getTiersForRule(rule.id())
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
