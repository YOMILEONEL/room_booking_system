package steve.bookingssystem.assistant.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.assistant.model.AssistantSessionDTO;
import steve.bookingssystem.assistant.service.AssistantSessionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/assistant/sessions")
@Tag(name = "KI-Assistent-Sessions", description = "Chat-Sessions mit dem KI-Assistenten (eigene Sessions, Kunde/Organisation)")
@SecurityRequirement(name = "bearerAuth")
public class AssistantSessionController {

    @Autowired
    private AssistantSessionService assistantSessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Neue Chat-Session anlegen",
            description = "Wird u. a. automatisch bei jedem Login aufgerufen, damit die Person auf /assistant " +
                    "immer eine frische, leere Session vorfindet.")
    @ApiResponse(responseCode = "201", description = "Session angelegt")
    public AssistantSessionDTO create() {
        return assistantSessionService.create();
    }

    @GetMapping
    @Operation(summary = "Eigene Chat-Sessions auflisten",
            description = "Neueste zuerst, je mit einer kurzen Vorschau (erste Nachricht) für die Session-Auswahl auf /assistant.")
    @ApiResponse(responseCode = "200", description = "Sessions geliefert (ggf. leer)")
    public List<AssistantSessionDTO> getSessions() {
        return assistantSessionService.getSessions();
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Session löschen",
            description = "Standardmäßig ein expliziter, endgültiger Löschvorgang (inkl. aller Nachrichten der " +
                    "Session) - für den \"Löschen\"-Button in der Session-Liste. Mit " +
                    "`onlyIfUnused=true` stattdessen ein stiller No-op, falls die Session bereits Nachrichten " +
                    "hat oder nicht der anfragenden Person gehört - so ruft der Logout-Handler das für die bei " +
                    "diesem Login angelegte Session auf, ohne eine tatsächlich genutzte Unterhaltung zu riskieren.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gelöscht (oder, mit onlyIfUnused=true, No-op)"),
            @ApiResponse(responseCode = "403", description = "Session gehört nicht der anfragenden Person (nur ohne onlyIfUnused)"),
            @ApiResponse(responseCode = "404", description = "Session nicht gefunden (nur ohne onlyIfUnused)")
    })
    public void delete(@Parameter(description = "ID der Session") @PathVariable UUID sessionId,
                        @Parameter(description = "true: nur löschen, wenn die Session noch keine Nachricht hat, sonst stiller No-op")
                        @RequestParam(defaultValue = "false") boolean onlyIfUnused) {
        if (onlyIfUnused) {
            assistantSessionService.deleteIfUnused(sessionId);
        } else {
            assistantSessionService.delete(sessionId);
        }
    }

}
