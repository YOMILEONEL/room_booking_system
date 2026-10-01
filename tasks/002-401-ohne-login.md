# 401 statt 403 bei fehlendem Login

## Problem
Ohne Login (kein oder ungültiger Bearer-Token) antworten alle geschützten Endpunkte mit 403 statt 401,
weil in `SecurityConfig` kein `AuthenticationEntryPoint` konfiguriert ist. Spring fällt dann auf den
`Http403ForbiddenEntryPoint` zurück. Folgen:
- 403 bedeutet laut HTTP "eingeloggt, aber keine Berechtigung". Clients können "nicht eingeloggt" und
  "keine Rechte" nicht unterscheiden.
- Die OpenAPI-Doku widerspricht dem Verhalten, z. B. `RoomController` für `GET /room/{id}/booked-periods`:
  `@ApiResponse` nennt 401, tatsächlich kommt 403.
- Die Akzeptanzkriterien aus `tasks/001-belegte-zeitraeume.md` verlangen 401.

## Ziel
Fehlende oder ungültige Authentifizierung ergibt app-weit 401 mit JSON-Body im bestehenden Fehlerformat
(`{"error": "..."}`). Fehlende Berechtigung bei gültigem Login bleibt 403.

## Backend
- In `SecurityConfig` einen `AuthenticationEntryPoint` registrieren (`exceptionHandling(...)`), der 401 mit
  `{"error": "Nicht authentifiziert"}` (Content-Type `application/json`) schreibt
- **Begründung für den Eingriff in `SecurityConfig`:** Der Status lässt sich nur dort app-weit korrekt setzen.
  Andere Security-Regeln (Pfade, Rollen, JWT-Filter, Rate-Limit) bleiben unverändert.
- `AccessDeniedHandler`/`GlobalExceptionHandler` für 403 bei fehlender Rolle unverändert lassen
- Bestehende Tests anpassen, die 403 ohne Login erwarten:
  - `integration/AuthFlowIntegrationTest.java` (`/booking/getAll` ohne Token)
  - `integration/BookedPeriodsIntegrationTest.java` (inkl. des Kommentars zu 403)
- Neuer Test: eingeloggter Kunde auf einem Admin-Endpunkt → weiterhin 403
- `@ApiResponses` prüfen: wo 403 für "nicht eingeloggt" dokumentiert ist, auf 401 korrigieren

## Frontend
- Keine Änderung erwartet. `apiClient.ts`/`http.ts` verzweigen nicht auf 401/403, sie zeigen nur die
  Meldung an. Kurz prüfen und im Bericht bestätigen.

## Akzeptanzkriterien
- [ ] Geschützter Endpunkt ohne Token → 401 mit JSON-Body `{"error": ...}`
- [ ] Geschützter Endpunkt mit ungültigem/abgelaufenem Token → 401
- [ ] Eingeloggter Nutzer ohne nötige Rolle → 403 (unverändert)
- [ ] Öffentliche Endpunkte (Login, Registrierung, Refresh, Stripe-Webhook) unverändert erreichbar
- [ ] Tests angepasst/ergänzt; Backend-Tests, Typecheck und Build grün

## Nicht anfassen
- JWT-Erzeugung/-Validierung, Refresh-Logik, Rate-Limit-Filter
- Pfad- und Rollenregeln in `SecurityConfig`
- Buchungs-, Zahlungs- und Stripe-Logik
- Flyway-Migrationen
