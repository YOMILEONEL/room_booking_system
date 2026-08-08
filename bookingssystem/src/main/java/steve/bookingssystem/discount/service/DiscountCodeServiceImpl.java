package steve.bookingssystem.discount.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import steve.bookingssystem.discount.model.DiscountCode;
import steve.bookingssystem.discount.model.DiscountType;
import steve.bookingssystem.discount.repository.DiscountCodeRepository;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.security.AuthorizationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DiscountCodeServiceImpl implements DiscountCodeService {

    @Autowired
    private DiscountCodeRepository discountCodeRepository;
    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public DiscountCode create(DiscountCode discountCode) {
        authorizationService.requireAdmin();

        if (discountCode.getType() == DiscountType.PERCENT
                && discountCode.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("A PERCENT discount cannot exceed 100");
        }

        return discountCodeRepository.save(discountCode);
    }

    @Override
    public List<DiscountCode> findAll() {
        authorizationService.requireAdmin();
        return discountCodeRepository.findAll();
    }

    @Override
    public void delete(UUID id) {
        authorizationService.requireAdmin();
        DiscountCode discountCode = discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount code not found: " + id));
        discountCodeRepository.delete(discountCode);
    }

    @Override
    public BigDecimal applyDiscount(String code, BigDecimal amount) {
        DiscountCode discountCode = discountCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown discount code: " + code));

        LocalDate today = LocalDate.now();
        if (!discountCode.isActive()
                || today.isBefore(discountCode.getValidFrom())
                || today.isAfter(discountCode.getValidUntil())) {
            throw new IllegalArgumentException("Discount code is not currently valid: " + code);
        }

        BigDecimal value = discountCode.getValue();
        if (discountCode.getType() == DiscountType.PERCENT) {
            BigDecimal factor = BigDecimal.ONE.subtract(value.divide(BigDecimal.valueOf(100)));
            return amount.multiply(factor).max(BigDecimal.ZERO);
        }

        return amount.subtract(value).max(BigDecimal.ZERO);
    }
}
