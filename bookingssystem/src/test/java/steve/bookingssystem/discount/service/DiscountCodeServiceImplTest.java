package steve.bookingssystem.discount.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import steve.bookingssystem.discount.model.DiscountCode;
import steve.bookingssystem.discount.model.DiscountType;
import steve.bookingssystem.discount.repository.DiscountCodeRepository;
import steve.bookingssystem.security.AuthorizationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountCodeServiceImplTest {

    @Mock
    private DiscountCodeRepository discountCodeRepository;
    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private DiscountCodeServiceImpl discountCodeService;

    private DiscountCode code(DiscountType type, String value, LocalDate from, LocalDate until, boolean active) {
        DiscountCode discountCode = new DiscountCode();
        discountCode.setCode("CODE");
        discountCode.setType(type);
        discountCode.setValue(new BigDecimal(value));
        discountCode.setValidFrom(from);
        discountCode.setValidUntil(until);
        discountCode.setActive(active);
        return discountCode;
    }

    @Test
    void applyDiscount_percentReducesAmount() {
        when(discountCodeRepository.findByCode("CODE")).thenReturn(Optional.of(
                code(DiscountType.PERCENT, "10", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), true)));

        BigDecimal result = discountCodeService.applyDiscount("CODE", new BigDecimal("200.00"));

        assertThat(result).isEqualByComparingTo("180.00");
    }

    @Test
    void applyDiscount_absoluteIsCappedAtZero() {
        when(discountCodeRepository.findByCode("CODE")).thenReturn(Optional.of(
                code(DiscountType.ABSOLUTE, "500", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), true)));

        BigDecimal result = discountCodeService.applyDiscount("CODE", new BigDecimal("100.00"));

        assertThat(result).isEqualByComparingTo("0");
    }

    @Test
    void applyDiscount_rejectsExpiredCode() {
        when(discountCodeRepository.findByCode("CODE")).thenReturn(Optional.of(
                code(DiscountType.PERCENT, "10", LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), true)));

        assertThatThrownBy(() -> discountCodeService.applyDiscount("CODE", new BigDecimal("100.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyDiscount_rejectsNotYetValidCode() {
        when(discountCodeRepository.findByCode("CODE")).thenReturn(Optional.of(
                code(DiscountType.PERCENT, "10", LocalDate.now().plusDays(1), LocalDate.now().plusDays(10), true)));

        assertThatThrownBy(() -> discountCodeService.applyDiscount("CODE", new BigDecimal("100.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyDiscount_rejectsInactiveCode() {
        when(discountCodeRepository.findByCode("CODE")).thenReturn(Optional.of(
                code(DiscountType.PERCENT, "10", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), false)));

        assertThatThrownBy(() -> discountCodeService.applyDiscount("CODE", new BigDecimal("100.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyDiscount_rejectsUnknownCode() {
        when(discountCodeRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountCodeService.applyDiscount("MISSING", new BigDecimal("100.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
