package uz.pulsepay.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Exchange rate response DTO.
 *
 * <p>buy/sell/buyPrev/sellPrev are string representations of tiyin values
 * (integer, rate × 100). The frontend divides by 100 to get UZS.
 *
 * <p>Data source: CBU (Central Bank of Uzbekistan) public API.
 * A ±0.3 % spread is applied around the official CBU mid-rate to produce
 * bank buying / selling prices.
 */
public record ExchangeRateDto(
        int unit,
        int min,
        String code,
        long rate,
        @JsonProperty("num_code") int numCode,
        int max,
        String buy,
        String sell,
        String name,
        String icon,
        @JsonProperty("buy_prev")  String buyPrev,
        @JsonProperty("sell_prev") String sellPrev,
        @JsonProperty("error_text") Object errorText
) {}
