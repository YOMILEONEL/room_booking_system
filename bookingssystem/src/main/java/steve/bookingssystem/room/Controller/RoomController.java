package steve.bookingssystem.room.Controller;

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
import org.springframework.web.multipart.MultipartFile;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.model.RoomImageDto;
import steve.bookingssystem.room.model.RoomResponseDTO;
import steve.bookingssystem.room.service.RoomImageService;
import steve.bookingssystem.room.service.RoomService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/room")
@Tag(name = "Räume", description = "Raumliste, -details und -fotos - Lesen für alle eingeloggten Personen, Schreiben/Fotos nur Admin")
@SecurityRequirement(name = "bearerAuth")
public class RoomController {

    @Autowired
    private RoomService roomService;
    @Autowired
    private RoomImageService roomImageService;

    @GetMapping("/Get")
    @Operation(summary = "Alle Räume auflisten",
            description = "Für Kunden/Organisationen nur aktive Räume, für Admins auch deaktivierte. " +
                    "Enthält Verfügbarkeit, Preis (inkl. eventuellem Organisationsrabatt) und ob der Raum " +
                    "aktuell durch eine laufende Buchung belegt ist.")
    @ApiResponse(responseCode = "200", description = "Raumliste geliefert")
    public List<RoomResponseDTO> getRooms() {
        return roomService.findAllRooms();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Einzelnen Raum abrufen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raum geliefert"),
            @ApiResponse(responseCode = "404", description = "Raum nicht gefunden (oder deaktiviert und kein Admin)")
    })
    public RoomResponseDTO getRoom(@Parameter(description = "ID des Raums") @PathVariable UUID id) {
        return roomService.findRoomById(id);
    }

    @PostMapping("/save")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Neuen Raum anlegen (nur Admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Raum angelegt"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto")
    })
    public Room saveRoom(@Valid @RequestBody Room room) {
        return roomService.saveRoom(room);
    }

    @PutMapping("update/{id}")
    @Operation(summary = "Raum bearbeiten (nur Admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raum aktualisiert"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto"),
            @ApiResponse(responseCode = "404", description = "Raum nicht gefunden")
    })
    public Room updateRoom(@Parameter(description = "ID des Raums") @PathVariable UUID id, @Valid @RequestBody Room room) {
        return roomService.updateRoom(id, room);
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Raum aktivieren (nur Admin)", description = "Macht einen zuvor deaktivierten Raum wieder für Kunden sichtbar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raum aktiviert"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto"),
            @ApiResponse(responseCode = "404", description = "Raum nicht gefunden")
    })
    public Room activateRoom(@Parameter(description = "ID des Raums") @PathVariable UUID id) {
        return roomService.activate(id);
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Raum deaktivieren (nur Admin)",
            description = "Soft-Delete: der Raum verschwindet für Kunden, bleibt aber für Admins sichtbar und bearbeitbar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raum deaktiviert"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto"),
            @ApiResponse(responseCode = "404", description = "Raum nicht gefunden")
    })
    public Room deactivateRoom(@Parameter(description = "ID des Raums") @PathVariable UUID id) {
        return roomService.deactivate(id);
    }

    @GetMapping("/{id}/images")
    @Operation(summary = "Fotos eines Raums auflisten", description = "Reihenfolge entspricht der Anzeige-Position (Titelbild zuerst).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bilderliste geliefert (ggf. leer)"),
            @ApiResponse(responseCode = "404", description = "Raum nicht gefunden")
    })
    public List<RoomImageDto> listImages(@Parameter(description = "ID des Raums") @PathVariable UUID id) {
        return roomImageService.list(id);
    }

    @PostMapping("/{id}/image")
    @Operation(summary = "Foto zu einem Raum hochladen (nur Admin)", description = "Multipart-Upload, maximal 5 MB pro Datei.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto hochgeladen"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto"),
            @ApiResponse(responseCode = "404", description = "Raum nicht gefunden")
    })
    public RoomImageDto uploadImage(@Parameter(description = "ID des Raums") @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return roomImageService.upload(id, file);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Foto eines Raums löschen (nur Admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto gelöscht"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto"),
            @ApiResponse(responseCode = "404", description = "Raum oder Foto nicht gefunden")
    })
    public void deleteImage(@Parameter(description = "ID des Raums") @PathVariable UUID id,
                             @Parameter(description = "ID des Fotos") @PathVariable UUID imageId) {
        roomImageService.delete(id, imageId);
    }

    @PutMapping("/{id}/images/reorder")
    @Operation(summary = "Fotoreihenfolge eines Raums ändern (nur Admin)",
            description = "Nimmt die gewünschte Reihenfolge als Liste von Foto-IDs entgegen; das erste Foto wird das Titelbild.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Neue Reihenfolge übernommen"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto"),
            @ApiResponse(responseCode = "404", description = "Raum nicht gefunden")
    })
    public List<RoomImageDto> reorderImages(@Parameter(description = "ID des Raums") @PathVariable UUID id,
                                             @RequestBody List<UUID> orderedImageIds) {
        return roomImageService.reorder(id, orderedImageIds);
    }

}
