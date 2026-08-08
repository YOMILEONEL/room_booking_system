package steve.bookingssystem.discount.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "discount_codes")
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType type;

    @NotNull
    @Positive
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @NotNull
    private LocalDate validFrom;

    @NotNull
    private LocalDate validUntil;

    @Column(nullable = false)
    private boolean active = true;

}
