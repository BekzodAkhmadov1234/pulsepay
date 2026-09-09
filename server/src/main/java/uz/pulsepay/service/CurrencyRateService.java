package uz.pulsepay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.pulsepay.dto.response.ExchangeRateDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fetches current exchange rates from the CBU (Central Bank of Uzbekistan) public JSON API.
 *
 * <p>The CBU endpoint provides today's official rate plus {@code Diff} (change from yesterday),
 * letting us compute the previous-day rate without any persistence layer.
 *
 * <p>A ±0.3 % spread is applied around the CBU mid-rate to produce realistic bank
 * buying / selling prices.
 */
@Slf4j
@Service
public class CurrencyRateService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CBU_URL =
            "https://cbu.uz/uz/arkhiv-kursov-valyut/json/";

    /** Sort order: USD=1, EUR=2, GBP=4, CHF=5, JPY=6 (RUB position 3 is skipped). */
    private static final Map<String, Integer> SORT_ORDER = Map.of(
            "USD", 1,
            "EUR", 2,
            "GBP", 4,
            "CHF", 5,
            "JPY", 6
    );

    /** Localized currency names keyed by ISO code and language. */
    private static final Map<String, Map<String, String>> NAMES = Map.of(
            "USD", Map.of("uz", "AQSH dollari",         "ru", "Доллар США",          "en", "US Dollar",      "zh", "美元"),
            "EUR", Map.of("uz", "Yevro",                "ru", "Евро",                "en", "Euro",           "zh", "欧元"),
            "GBP", Map.of("uz", "Funt sterling",        "ru", "Фунт стерлинг",       "en", "Pound sterling", "zh", "英镑"),
            "CHF", Map.of("uz", "Shveytsariya franki",  "ru", "Швейцарский франк",   "en", "Swiss franc",    "zh", "瑞士法郎"),
            "JPY", Map.of("uz", "Yaponiya iyenasi",     "ru", "Японская йена",        "en", "Japanese Yen",  "zh", "日元")
    );

    /** Bank buying rate: 0.3 % below the CBU mid-rate. */
    private static final double BUY_SPREAD  = 0.997;
    /** Bank selling rate: 0.3 % above the CBU mid-rate. */
    private static final double SELL_SPREAD = 1.003;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns exchange rates for the five supported currencies (USD, EUR, GBP, CHF, JPY).
     *
     * @param lang  UI language code from {@code X-Lang} header (uz / ru / en / zh / uz_c)
     * @return list of up to 5 rates; empty list when CBU API is unreachable
     */
    public List<ExchangeRateDto> getRates(String lang) {
        String resolvedLang = resolveLang(lang);
        try {
            String json = fetchCbuJson();
            return parseCbuRates(json, resolvedLang);
        } catch (Exception e) {
            log.error("Failed to fetch CBU exchange rates: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String fetchCbuJson() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CBU_URL))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("CBU API returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    private List<ExchangeRateDto> parseCbuRates(String json, String lang) throws Exception {
        JsonNode root = MAPPER.readTree(json);
        List<ExchangeRateDto> result = new ArrayList<>();

        for (JsonNode item : root) {
            String code = item.path("Ccy").asText();
            if (!SORT_ORDER.containsKey(code)) continue; // skip RUB and other currencies

            int numCode = parseIntSafe(item.path("Code").asText("0"));
            int unit    = parseIntSafe(item.path("Nominal").asText("1"));

            double cbRate = parseDoubleSafe(item.path("Rate").asText("0"));
            double diff   = parseDoubleSafe(item.path("Diff").asText("0"));

            long rateTiyin     = Math.round(cbRate * 100);
            long buyTiyin      = Math.round(rateTiyin * BUY_SPREAD);
            long sellTiyin     = Math.round(rateTiyin * SELL_SPREAD);
            long prevRateTiyin = Math.round((cbRate - diff) * 100);
            long buyPrevTiyin  = Math.round(prevRateTiyin * BUY_SPREAD);
            long sellPrevTiyin = Math.round(prevRateTiyin * SELL_SPREAD);

            String name = NAMES.getOrDefault(code, Map.of()).getOrDefault(lang, code);

            result.add(new ExchangeRateDto(
                    unit, 1, code, rateTiyin, numCode, 100,
                    String.valueOf(buyTiyin),
                    String.valueOf(sellTiyin),
                    name, "",
                    String.valueOf(buyPrevTiyin),
                    String.valueOf(sellPrevTiyin),
                    null
            ));
        }

        result.sort((a, b) ->
                Integer.compare(
                        SORT_ORDER.getOrDefault(a.code(), Integer.MAX_VALUE),
                        SORT_ORDER.getOrDefault(b.code(), Integer.MAX_VALUE)
                ));
        return result;
    }

    private static String resolveLang(String lang) {
        if (lang == null || lang.isBlank()) return "uz";
        return switch (lang) {
            case "ru" -> "ru";
            case "en" -> "en";
            case "zh" -> "zh";
            default   -> "uz"; // uz and uz_c both map to uz names
        };
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.replace(",", "").trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s.replace(",", ".").trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
