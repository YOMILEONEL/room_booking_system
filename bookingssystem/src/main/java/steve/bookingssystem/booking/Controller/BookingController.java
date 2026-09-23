package steve.bookingssystem.booking.Controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.booking.model.AdminBookingDTO;
import steve.bookingssystem.booking.model.BookingDTO;
import steve.bookingssystem.booking.model.BookingResponseDTO;
import steve.bookingssystem.booking.service.BookingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/booking")
@Tag(name = "Buchungen", description = "Räume buchen, einsehen und stornieren")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Raum für sich selbst buchen",
            description = "Legt eine neue Buchung für die anfragende Person an (Admins können hierüber " +
                    "nicht buchen, siehe /booking/admin-add). Erzeugt automatisch eine offene Zahlung.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Buchung angelegt"),
            @ApiResponse(responseCode = "400", description = "Enddatum vor Startdatum"),
            @ApiResponse(responseCode = "403", description = "Admin-Konto oder fremde userId"),
            @ApiResponse(responseCode = "404", description = "Raum nicht gefunden"),
            @ApiResponse(responseCode = "409", description = "Raum in diesem Zeitraum bereits gebucht")
    })
    public BookingResponseDTO saveBooking(@Valid @RequestBody BookingDTO booking) {
        return bookingService.addBooking(booking);
    }

    @PostMapping("/admin-add")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Raum im Namen eines Kunden buchen (nur Admin)",
            description = "Sucht den Kunden per E-Mail-Adresse und legt die Buchung für ihn an.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Buchung angelegt"),
            @ApiResponse(responseCode = "400", description = "Enddatum vor Startdatum oder E-Mail gehört zu keinem Kunden-Konto"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto"),
            @ApiResponse(responseCode = "404", description = "Kunde oder Raum nicht gefunden"),
            @ApiResponse(responseCode = "409", description = "Raum in diesem Zeitraum bereits gebucht")
    })
    public BookingResponseDTO saveBookingForCustomer(@Valid @RequestBody AdminBookingDTO booking) {
        return bookingService.addBookingForCustomer(booking);
    }

    @GetMapping("/getAll")
    @Operation(summary = "Buchungen abrufen",
            description = "Für Kunden/Organisationen: nur die eigenen Buchungen. Für Admins: alle Buchungen.")
    @ApiResponse(responseCode = "200", description = "Buchungsliste geliefert (ggf. leer)")
    public List<BookingResponseDTO> getBookings() {
        return bookingService.getBookings();
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Buchung stornieren",
            description = "Nicht möglich, wenn die Buchung bereits bezahlt ist oder ihr Zeitraum den " +
                    "heutigen Tag einschließt - gilt für Kunden und Admins gleichermaßen.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Buchung gelöscht"),
            @ApiResponse(responseCode = "403", description = "Fremde Buchung, kein Admin"),
            @ApiResponse(responseCode = "404", description = "Buchung nicht gefunden"),
            @ApiResponse(responseCode = "409", description = "Bezahlte oder laufende Buchung kann nicht gelöscht werden")
    })
    public void deleteBooking(@Parameter(description = "bookingId der Buchung") @PathVariable UUID id) {
        bookingService.deleteBooking(id);
    }

    @GetMapping("get/{id}")
    @Operation(summary = "Einzelne Buchung abrufen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Buchung geliefert"),
            @ApiResponse(responseCode = "403", description = "Fremde Buchung, kein Admin"),
            @ApiResponse(responseCode = "404", description = "Buchung nicht gefunden")
    })
    public BookingResponseDTO getBooking(@Parameter(description = "bookingId der Buchung") @PathVariable UUID id) {
        return bookingService.getBooking(id);
    }

}
