# Projekt

Spring Boot (bookingssystem/), Next.js App Router + TypeScript (react_frontend/).
Maven, Java 23. Claude Code aus der Repo-Wurzel starten.

## Befehle

- Backend testen: `./bookingssystem/mvnw -f bookingssystem/pom.xml -q test -Dspring.profiles.active=test`
- Backend bauen: `./bookingssystem/mvnw -f bookingssystem/pom.xml -q package -DskipTests`
- Frontend Lint: `npm --prefix react_frontend run lint`
- Frontend Typecheck: `npm --prefix react_frontend run typecheck`
- Frontend Build: `npm --prefix react_frontend run build`
- Frontend Tests: nicht vorhanden (kein Test-Framework eingerichtet)
- API-Client generieren: nicht vorhanden. Die OpenAPI-Spec erzeugt springdoc zur Laufzeit (`/v3/api-docs`), der Client ist handgeschrieben in `react_frontend/src/app/api/*.api.ts` (Basis: `http.ts`, `apiClient.ts`).

### Bekannte Altlasten (Stand 2026-10-01)

`npm --prefix react_frontend run lint` meldet 6 vorbestehende Errors `react-hooks/set-state-in-effect` in:
`admin/page.tsx`, `admin/users/page.tsx`, `components/BookingTable.tsx`, `components/DiscountCodeAdmin.tsx`,
`components/RoomTable.tsx`, `profile/page.tsx` (alle unter `react_frontend/src/app/`).
Diese zählen nicht als FAIL. Nur neue Lint-Errors oder Errors in geänderten Dateien zählen.
Wenn sie behoben sind, diesen Abschnitt löschen.

## Regeln

- Contract-first: erst Backend (Controller/DTO inkl. `@Operation`/`@ApiResponses`), dann die passenden Client-Module in `react_frontend/src/app/api/`, dann UI.
- Backend-Code nur in bookingssystem/, Frontend-Code nur in react_frontend/.
- Tests müssen echt ausgeführt werden. Nie "angenommen", dass sie laufen.
- Keine Secrets lesen oder ausgeben (`.env*`, `application-prod.*`).
- Niemals committen, pushen oder deployen ohne meine ausdrückliche Freigabe.
- Halte dich an bestehende Paketstruktur, Namenskonventionen und Code-Stil.
