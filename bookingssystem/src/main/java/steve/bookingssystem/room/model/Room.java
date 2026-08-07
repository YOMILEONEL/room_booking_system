package steve.bookingssystem.room.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;


@Data
@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Positive
    private int capacity;

    @NotBlank
    private String location;

    @NotNull
    @Positive
    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    private Status roomStatus = Status.VERFUGBAR;


}
