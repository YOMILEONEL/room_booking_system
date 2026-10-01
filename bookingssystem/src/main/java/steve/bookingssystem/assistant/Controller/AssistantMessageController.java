package steve.bookingssystem.assistant.Controller;

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
import steve.bookingssystem.assistant.model.AssistantMessageDTO;
import steve.bookingssystem.assistant.model.AssistantMessageRequest;
import steve.bookingssystem.assistant.service.AssistantMessageService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/assistant/sessions/{sessionId}")
@Tag(name = "KI-Assistent-Verlauf", description = "Nachrichten innerhalb einer Chat-Session (eigene Sessions, Kunde/Organisation)")
@SecurityRequirement(name = "bearerAuth")
public class AssistantMessageController {

    @Autowired
    private AssistantMessageService assistantMessageService;

    @GetMapping("/history")
    @Operation(summary = "Verlauf einer Session abrufen",
            description = "Alle bisher gespeicherten Nachrichten (Frage + Antwort/Vorschlag) dieser Session, " +
                    "chronologisch aufsteigend sortiert.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verlauf geliefert (ggf. leer)"),
            @ApiResponse(responseCode = "401", description = "Nicht eingeloggt oder Token ungültig"),
            @ApiResponse(responseCode = "403", description = "Session gehört nicht der anfragenden Person"),
            @ApiResponse(responseCode = "404", description = "Session nicht gefunden")
    })
    public List<AssistantMessageDTO> getHistory(@Parameter(description = "ID der Session") @PathVariable UUID sessionId) {
        return assistantMessageService.getHistory(sessionId);
    }

    @PostMapping("/history")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Nachrichten zu einer Session hinzufügen",
            description = "Speichert eine oder mehrere Nachrichten (z. B. Frage + Antwort in einem Aufruf) in " +
                    "dieser Session. Wird vom Next.js-Frontend nach jeder Anfrage an den KI-Assistenten sowie " +
                    "nach einer bestätigten Buchung/Stornierung aufgerufen.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nachrichten gespeichert"),
            @ApiResponse(responseCode = "400", description = "Ungültige Nachricht (z. B. leerer Inhalt)"),
            @ApiResponse(responseCode = "401", description = "Nicht eingeloggt oder Token ungültig"),
            @ApiResponse(responseCode = "403", description = "Session gehört nicht der anfragenden Person"),
            @ApiResponse(responseCode = "404", description = "Session nicht gefunden")
    })
    public List<AssistantMessageDTO> addMessages(@Parameter(description = "ID der Session") @PathVariable UUID sessionId,
                                                  @Valid @RequestBody List<@Valid AssistantMessageRequest> messages) {
        return assistantMessageService.addMessages(sessionId, messages);
    }

}
