package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.domain.reference.Bank;
import uz.pulsepay.service.BankService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Banks", description = "Reference list of active banks for P2A transfers")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/banks")
public class BankListController {

    private final BankService bankService;

    public BankListController(BankService bankService) {
        this.bankService = bankService;
    }

    @Operation(summary = "List active banks", description = "Returns all active banks available as P2A transfer destinations.")
    @GetMapping
    public ResponseEntity<List<BankDto>> listBanks() {
        List<BankDto> banks = bankService.listActiveBanks().stream()
                .map(BankDto::from).toList();
        return ResponseEntity.ok(banks);
    }

    public record BankDto(UUID id, String mfoCode, String name) {
        public static BankDto from(Bank bank) {
            return new BankDto(bank.id(), bank.mfoCode(), bank.name());
        }
    }
}
