# Auth-Testlücken schließen und 401 vollständig dokumentieren

## Problem
Nach `tasks/002-401-ohne-login.md` sind aus den Reviews Lücken offen:
1. Kein Test für einen **abgelaufenen** Access-Token. Nur ein ungültiger Token ist getestet; beide laufen
   im `JwtAuthFilter` über denselben Pfad, das ist aber nicht abgesichert.
2. Keine Regressionstests, dass die 401-Umstellung Sonderpfade nicht verändert hat:
   - CORS-Preflight (`OPTIONS` mit `Origin` und `Access-Control-Request-Method`) auf einen geschützten Pfad darf
     nicht mit 401 abgewiesen werden
   - `POST /payment/stripe/webhook` ohne `Stripe-Signature` liefert weiterhin 400 (nicht 401)
3. In der OpenAPI-Doku fehlt 401 bei geschützten Endpunkten, die keinen 403-Eintrag haben
   (z. B. `GET /room/Get`, `GET /room/{id}/images`). Bisher wurde 401 nur per Annotation an den 26 Endpunkten
   mit 403-Eintrag ergänzt.

## Ziel
Die 401-Logik ist durch Tests abgesichert, und die OpenAPI-Doku nennt 401 bei **allen** geschützten Endpunkten.
Keine Verhaltensänderung der App.

## Backend
- Tests (Integrationstests im Stil von `integration/UnauthenticatedAccessIntegrationTest.java`):
  - abgelaufener Access-Token → 401 mit `{"error":"Nicht authentifiziert"}`. Den Token im Test erzeugen
    (z. B. eigene `JwtService`-Instanz mit gleicher Test-Konfiguration und abgelaufener Gültigkeit, oder per
    JJWT mit dem Test-Secret aus `src/test/resources/application-test.properties`). `JwtService` selbst nicht ändern.
  - CORS-Preflight `OPTIONS /booking/getAll` mit erlaubtem Origin (`http://localhost:3000` bzw. Testwert von
    `app.cors.allowed-origins`) → 200 und `Access-Control-Allow-Origin` gesetzt, kein 401
  - `POST /payment/stripe/webhook` ohne `Stripe-Signature` → 400
- OpenAPI: global per `OpenApiCustomizer` (springdoc) in `config/OpenApiConfig.java` eine 401-Antwort
  ("Nicht eingeloggt oder Token ungültig") an jede Operation hängen, die **nicht** öffentlich ist und noch keine
  401 hat. Öffentlich: `/api/register`, `/api/login`, `/api/refresh`, `/api/logout`, `/api/forgot-password`,
  `/api/reset-password`, `/payment/stripe/webhook`. Bestehende 401-Annotationen bleiben (kein Doppeleintrag).
- Test für die Doku: `GET /v3/api-docs` im Test abrufen und prüfen, dass z. B. `/room/Get` (GET) eine 401 hat
  und `/api/login` (POST) keine.

## Frontend
- Keine Änderung.

## Akzeptanzkriterien
- [ ] Abgelaufener Token → 401 mit JSON-Body (Test)
- [ ] CORS-Preflight auf geschützten Pfad → kein 401, CORS-Header vorhanden (Test)
- [ ] Stripe-Webhook ohne Signatur → 400 (Test)
- [ ] `/v3/api-docs`: jede geschützte Operation hat 401, öffentliche nicht (Test)
- [ ] Backend-Tests grün; Typecheck und Build unverändert grün

## Bewusst nicht in diesem Task
- Umstellung von Feldinjektion (`@Autowired public`) auf Konstruktorinjektion: Das Muster steht in ~20 Klassen.
  Nur `RoomServiceImpl` umzustellen würde den Projektstil brechen. Bei Bedarf eigener, projektweiter Task.

## Nicht anfassen
- `SecurityConfig`, `JsonAuthenticationEntryPoint`, `JwtService`, `JwtAuthFilter`, `RateLimitFilter`
- Stripe-/Zahlungslogik (`StripeServiceImpl`, `StripeWebhookController`), Buchungslogik
- Controller-Logik und bestehende `@ApiResponse`-Annotationen
- Flyway-Migrationen, Frontend
- Keine Secrets aus `.env*` oder `application-prod.*` lesen
