package steve.bookingssystem.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import steve.bookingssystem.security.AuthorizationService;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin-Dashboard", description = "Kennzahlen für den Admin-Bereich (nur Admin)")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;
    @Autowired
    private AuthorizationService authorizationService;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard-Kennzahlen abrufen",
            description = "Verfügbare/belegte Räume, Nutzerzahl, Umsatz, offene Zahlungen, meistgebuchte " +
                    "Räume und aktivste Kunden.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kennzahlen geliefert"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto")
    })
    public AdminDashboardDto getDashboard() {
        authorizationService.requireAdmin();
        return adminDashboardService.getDashboard();
    }
}
