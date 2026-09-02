package uz.pulsepay.domain.shared;

import java.math.BigDecimal;

public record Money(long amount, CurrencyCode currency) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("Money amount must be non-negative");
        }
    }

    public static Money ofTiyin(long tiyin, CurrencyCode currency) {
        return new Money(tiyin, currency);
    }

    public static Money fromUzs(BigDecimal uzs) {
        long tiyin = uzs.multiply(BigDecimal.valueOf(100)).longValueExact();
        return new Money(tiyin, CurrencyCode.UZS);
    }

    public BigDecimal toUzs() {
        return BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100));
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add money of different currencies");
        }
        return new Money(this.amount + other.amount, this.currency);
    }
}
