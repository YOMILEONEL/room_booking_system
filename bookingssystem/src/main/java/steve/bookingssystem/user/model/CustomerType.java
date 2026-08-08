package steve.bookingssystem.user.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum CustomerType {
    ORGANISATION, KUNDE;

    private static final BigDecimal ORGANISATION_DISCOUNT_FACTOR = new BigDecimal("0.90");

    public BigDecimal applyPricing(BigDecimal amount) {
        return this == ORGANISATION
                ? amount.multiply(ORGANISATION_DISCOUNT_FACTOR).setScale(2, RoundingMode.HALF_UP)
                : amount;
    }
}
