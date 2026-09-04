package uz.pulsepay.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fee preview response — fields: feeAmountUzs, totalAmountUzs, minAmountUzs, maxAmountUzs, commissionPercent.
 */
public record FeePreviewResponse(
        BigDecimal feeAmountUzs,
        BigDecimal totalAmountUzs,
        BigDecimal minAmountUzs,
        BigDecimal maxAmountUzs,
        String commissionPercent
) {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static FeePreviewResponse of(long amountTiyin, long feeTiyin,
                                        long minTiyin, Long maxTiyin,
                                        Integer percentageBps) {
        BigDecimal amountUzs = BigDecimal.valueOf(amountTiyin).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal feeUzs    = BigDecimal.valueOf(feeTiyin).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal totalUzs  = amountUzs.add(feeUzs);
        BigDecimal minUzs    = BigDecimal.valueOf(minTiyin).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal maxUzs    = maxTiyin != null
                ? BigDecimal.valueOf(maxTiyin).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : null;

        String commission = percentageBps != null
                ? BigDecimal.valueOf(percentageBps).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) + "%"
                : "0%";

        return new FeePreviewResponse(feeUzs, totalUzs, minUzs, maxUzs, commission);
    }

    /** Used when no fee rule is matched — returns zeros. */
    public static FeePreviewResponse noRule(long amountTiyin) {
        BigDecimal amountUzs = BigDecimal.valueOf(amountTiyin).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return new FeePreviewResponse(BigDecimal.ZERO, amountUzs, null, null, "0%");
    }
}
