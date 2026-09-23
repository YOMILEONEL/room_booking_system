# OpenAPI / Swagger UI

Das Backend liefert seit springdoc-openapi seine REST-API automatisch als OpenAPI-3-Spezifikation
aus - generiert direkt aus den Controllern (`@Tag`/`@Operation`/`@ApiResponse`-Annotationen) und
den DTOs, nicht als separat gepflegte Spec-Datei.

## Aufrufen

| Was | URL |
|---|---|
| Interaktive Doku (Swagger UI) | `http://localhost:8080/swagger-ui.html` |
| Rohe OpenAPI-Spezifikation (JSON) | `http://localhost:8080/v3/api-docs` |

Beide Pfade sind ohne Login erreichbar (`SecurityConfig` erlaubt `/v3/api-docs/**` und
`/swagger-ui/**` explizit) - Browsen der Doku soll keinen Token voraussetzen. Ein echter
API-Aufruf über "Try it out" braucht trotzdem einen gültigen Access-Token.

## Authentifizieren in Swagger UI

1. Über `POST /api/login` (im Tag **Authentifizierung**) einloggen und das `accessToken` aus der
   Antwort kopieren.
2. Oben rechts auf **Authorize** klicken, den Token **ohne** `Bearer `-Präfix einfügen (Swagger UI
   ergänzt das Präfix selbst) und bestätigen.
3. Danach senden alle "Try it out"-Aufrufe automatisch den `Authorization: Bearer <token>`-Header
   mit.

Endpunkte ohne Schloss-Symbol (`/api/register`, `/api/login`, `/api/refresh`, `/api/logout`,
`/api/forgot-password`, `/api/reset-password`) sind bewusst öffentlich und brauchen keinen Token -
das entspricht genau der `permitAll()`-Liste in `SecurityConfig`.

## Tags (Controller-Gruppen)

| Tag | Controller | Zugriff |
|---|---|---|
| Authentifizierung | `RegistrationLoginController` | öffentlich |
| Räume | `RoomController` | Lesen: eingeloggt · Schreiben/Fotos: nur Admin |
| Buchungen | `BookingController` | eigene Buchungen oder Admin |
| Zahlungen | `PaymentController` | Lesen: Besitzer/Admin · Bestätigen: nur Admin |
| Rechnungen | `InvoiceController` | Besitzer/Admin |
| Rabattcodes | `DiscountCodeController` | nur Admin |
| Benutzer | `UserController` | eigenes Konto oder Admin |
| Admin-Dashboard | `AdminDashboardController` | nur Admin |
| KI-Assistent-Verlauf | `AssistantMessageController` | eigener Verlauf (Kunde/Organisation) |

## Was dokumentiert ist

Jeder Endpunkt hat eine kurze Beschreibung (`@Operation`) und die tatsächlich möglichen
Antwortcodes (`@ApiResponses`) - abgeleitet aus `GlobalExceptionHandler` (z. B.
`ResourceNotFoundException` → 404, `BookingConflictException`/`IllegalStateException` → 409,
`AccessDeniedException` → 403, `IllegalArgumentException` → 400), nicht nur aus dem
Happy-Path-Rückgabetyp der Methode. Die Fehlerantworten selbst folgen im ganzen Backend derselben
Form: `{"error": "..."}` (bzw. Feld→Meldung-Paare bei Validierungsfehlern).

## Konfiguration

`OpenApiConfig` (`steve.bookingssystem.config`) definiert Titel/Beschreibung sowie das
`bearerAuth`-Sicherheitsschema; einzelne Controller setzen `@SecurityRequirement(name =
"bearerAuth")`, `RegistrationLoginController` überschreibt das mit einem leeren
`@SecurityRequirements`, da all seine Endpunkte öffentlich sind. Kein manuell gepflegtes
OpenAPI-YAML/JSON im Repo - die Spezifikation entsteht bei jedem Start neu aus dem aktuellen Code
und kann daher nicht veralten.
