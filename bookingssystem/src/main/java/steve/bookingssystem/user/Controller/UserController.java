package steve.bookingssystem.user.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.user.model.UpdateUserRequest;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserDTO;
import steve.bookingssystem.user.service.UserService;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/user")
@Tag(name = "Benutzer", description = "Eigenes Profil verwalten (Besitzer/Admin) - Konto löschen und Alle-Liste nur Admin")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private AuthorizationService authorizationService;


    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Konto löschen (nur Admin)",
            description = "Harter Delete, kein Soft-Delete. Schlägt mit 409 fehl, wenn das Konto noch " +
                    "referenzierte Daten hat (z. B. Buchungen) - die DB-Fremdschlüsselbeschränkung greift, " +
                    "bevor gelöscht wird.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Konto gelöscht"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto"),
            @ApiResponse(responseCode = "404", description = "Konto nicht gefunden"),
            @ApiResponse(responseCode = "409", description = "Konto hat noch verknüpfte Daten (z. B. Buchungen)")
    })
    public void deleteUser(@Parameter(description = "ID des Kontos") @PathVariable UUID id) {
        authorizationService.requireAdmin();
        userService.deleteUser(id);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "Konto abrufen", description = "Nur das eigene Konto oder, als Admin, jedes beliebige.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Konto geliefert"),
            @ApiResponse(responseCode = "403", description = "Fremdes Konto, kein Admin"),
            @ApiResponse(responseCode = "404", description = "Konto nicht gefunden")
    })
    public UserDTO getUser(@Parameter(description = "ID des Kontos") @PathVariable UUID id) {
        authorizationService.requireOwnerOrAdmin(id);
        return User.getUserDTO(userService.getUserById(id));
    }

    @GetMapping("/getAll")
    @Operation(summary = "Alle Konten auflisten (nur Admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kontoliste geliefert"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto")
    })
    public List<UserDTO> getUsers() {
        authorizationService.requireAdmin();
        return userService.getAllUsers();
    }

    @PutMapping("update/{id}")
    @Operation(summary = "Eigenes Profil bearbeiten", description = "Nur das eigene Konto oder, als Admin, jedes beliebige.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Konto aktualisiert"),
            @ApiResponse(responseCode = "403", description = "Fremdes Konto, kein Admin"),
            @ApiResponse(responseCode = "404", description = "Konto nicht gefunden")
    })
    public void updateUser(@Parameter(description = "ID des Kontos") @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        authorizationService.requireOwnerOrAdmin(id);
        userService.updateUser(id, request);
    }



}
