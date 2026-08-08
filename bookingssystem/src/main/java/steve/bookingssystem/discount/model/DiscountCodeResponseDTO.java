package steve.bookingssystem.discount.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class DiscountCodeResponseDTO {

    private UUID id;
    private String code;
    private DiscountType type;
    private BigDecimal value;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private boolean active;

    public DiscountCodeResponseDTO(UUID id, String code, DiscountType type, BigDecimal value,
                                    LocalDate validFrom, LocalDate validUntil, boolean active) {
        this.id = id;
        this.code = code;
        this.type = type;
        this.value = value;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.active = active;
    }

    public static DiscountCodeResponseDTO from(DiscountCode discountCode) {
        return new DiscountCodeResponseDTO(
                discountCode.getId(),
                discountCode.getCode(),
                discountCode.getType(),
                discountCode.getValue(),
                discountCode.getValidFrom(),
                discountCode.getValidUntil(),
                discountCode.isActive()
        );
    }
}
