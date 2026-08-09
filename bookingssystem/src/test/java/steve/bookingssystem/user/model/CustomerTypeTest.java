package steve.bookingssystem.user.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// Regression test for docs/code-review.md, section 4: the 10% organisation discount had zero
// test coverage despite being the core of the org/customer pricing model.
class CustomerTypeTest {

    @Test
    void organisation_getsTenPercentOff() {
        BigDecimal result = CustomerType.ORGANISATION.applyPricing(new BigDecimal("200.00"));

        assertThat(result).isEqualByComparingTo("180.00");
    }

    @Test
    void kunde_paysFullPrice() {
        BigDecimal result = CustomerType.KUNDE.applyPricing(new BigDecimal("200.00"));

        assertThat(result).isEqualByComparingTo("200.00");
    }

    @Test
    void organisation_discountRoundsToTwoDecimals() {
        // 99.99 * 0.90 = 89.991 - would carry a third decimal without rounding.
        BigDecimal result = CustomerType.ORGANISATION.applyPricing(new BigDecimal("99.99"));

        assertThat(result).isEqualByComparingTo("89.99");
        assertThat(result.scale()).isEqualTo(2);
    }
}
