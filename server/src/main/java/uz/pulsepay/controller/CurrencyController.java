package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.dto.response.ExchangeRateDto;
import uz.pulsepay.service.CurrencyRateService;

import java.util.List;

/**
 * Public exchange-rate endpoint — no authentication required.
 *
 * <p>No auth middleware is applied. The UI language is derived from the
 * {@code X-Lang} request header set by {@link uz.pulsepay.config.LocaleConfig}.
 */
@Tag(name = "Currency", description = "Exchange rates from CBU (Central Bank of Uzbekistan)")
@RestController
@RequestMapping("/api/v1/currency")
public class CurrencyController {

    private final CurrencyRateService currencyRateService;

    public CurrencyController(CurrencyRateService currencyRateService) {
        this.currencyRateService = currencyRateService;
    }

    @Operation(
            summary = "Get current exchange rates",
            description = "Returns today's exchange rates for USD, EUR, GBP, CHF, JPY from the CBU " +
                          "public API. Includes buy/sell spread (±0.3 % around mid-rate) and previous " +
                          "day rates for trend indicators. No authentication required."
    )
    @GetMapping("/rates")
    public ResponseEntity<List<ExchangeRateDto>> getRates() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return ResponseEntity.ok(currencyRateService.getRates(lang));
    }
}
