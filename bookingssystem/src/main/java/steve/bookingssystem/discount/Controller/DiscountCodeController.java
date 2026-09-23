package steve.bookingssystem.discount.Controller;

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
import steve.bookingssystem.discount.model.DiscountCode;
import steve.bookingssystem.discount.model.DiscountCodeResponseDTO;
import steve.bookingssystem.discount.service.DiscountCodeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/discount-code")
@Tag(name = "Rabattcodes", description = "Rabattcode-Verwaltung (nur Admin) - nicht für Organisationen einlösbar")
@SecurityRequirement(name = "bearerAuth")
public class DiscountCodeController {

    @Autowired
    private DiscountCodeService discountCodeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Rabattcode anlegen", description = "Prozentual oder absolut, mit Gültigkeitszeitraum.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rabattcode angelegt"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto")
    })
    public DiscountCodeResponseDTO create(@Valid @RequestBody DiscountCode discountCode) {
        return DiscountCodeResponseDTO.from(discountCodeService.create(discountCode));
    }

    @GetMapping
    @Operation(summary = "Alle Rabattcodes abrufen")
    @ApiResponse(responseCode = "200", description = "Rabattcode-Liste geliefert")
    public List<DiscountCodeResponseDTO> getAll() {
        return discountCodeService.findAll().stream().map(DiscountCodeResponseDTO::from).toList();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Rabattcode löschen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rabattcode gelöscht"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto"),
            @ApiResponse(responseCode = "404", description = "Rabattcode nicht gefunden")
    })
    public void delete(@Parameter(description = "ID des Rabattcodes") @PathVariable UUID id) {
        discountCodeService.delete(id);
    }

}
