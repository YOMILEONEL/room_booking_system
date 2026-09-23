package steve.bookingssystem.assistant.Controller;

import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/assistant")
@Tag(name = "KI-Assistent-Verlauf", description = "Gespeicherter Gesprächsverlauf mit dem KI-Assistenten (eigener Verlauf, Kunde/Organisation)")
@SecurityRequirement(name = "bearerAuth")
public class AssistantMessageController {

    @Autowired
    private AssistantMessageService assistantMessageService;

    @GetMapping("/history")
    @Operation(summary = "Eigenen Gesprächsverlauf abrufen",
            description = "Alle bisher gespeicherten Nachrichten (Frage + Antwort/Vorschlag) der anfragenden " +
                    "Person, chronologisch aufsteigend sortiert.")
    @ApiResponse(responseCode = "200", description = "Verlauf geliefert (ggf. leer)")
    public List<AssistantMessageDTO> getHistory() {
        return assistantMessageService.getHistory();
    }

    @PostMapping("/history")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Nachrichten zum Verlauf hinzufügen",
            description = "Speichert eine oder mehrere Nachrichten (z. B. Frage + Antwort in einem Aufruf) " +
                    "für die anfragende Person. Wird vom Next.js-Frontend nach jeder Anfrage an den " +
                    "KI-Assistenten sowie nach einer bestätigten Buchung/Stornierung aufgerufen.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nachrichten gespeichert"),
            @ApiResponse(responseCode = "400", description = "Ungültige Nachricht (z. B. leerer Inhalt)")
    })
    public List<AssistantMessageDTO> addMessages(@Valid @RequestBody List<@Valid AssistantMessageRequest> messages) {
        return assistantMessageService.addMessages(messages);
    }

}
