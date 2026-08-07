package steve.bookingssystem.discount.service;

import steve.bookingssystem.discount.model.DiscountCode;

import java.math.BigDecimal;
import java.util.List;

public interface DiscountCodeService {
    DiscountCode create(DiscountCode discountCode);
    List<DiscountCode> findAll();
    void delete(Long id);
    BigDecimal applyDiscount(String code, BigDecimal amount);
}
