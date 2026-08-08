package steve.bookingssystem.room.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;


@Data
@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String name;

    @Positive
    private int capacity;

    // Same reasoning as city/description below: no validation annotation, so Hibernate
    // never generates this as NOT NULL on ddl-auto=update.
    private Double sizeSquareMeters;

    @NotBlank
    private String location;

    // Not @NotBlank on purpose: that annotation makes Hibernate generate the column as
    // NOT NULL on ddl-auto=update, which fails against existing rows that predate this field
    // (Postgres rolls the whole ALTER TABLE back). "Required" is enforced client-side instead.
    private String city;

    @Column(length = 2000)
    private String description;

    @NotNull
    @Positive
    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Enumerated(EnumType.STRING)
    private Status roomStatus = Status.VERFUGBAR;

    private String imageUrl;

    private boolean active = true;

}
