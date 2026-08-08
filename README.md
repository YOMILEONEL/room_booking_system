# Spacio

Fullstack-Anwendung zur Verwaltung und Buchung von Besprechungs-, Veranstaltungs- und
Schulungsräumen. Backend (Spring Boot) und Frontend (Next.js) sind getrennte Anwendungen, die über
eine REST-API kommunizieren und gemeinsam per Docker Compose betrieben werden.

> Ausführlichere Dokumentation liegt im Ordner [`docs/`](docs/README.md):
> Architektur- und Feature-Überblick in [`docs/README.md`](docs/README.md),
> Anforderungen in [`docs/requirements-engineering.md`](docs/requirements-engineering.md),
> vollständiger Code-Review in [`docs/code-review.md`](docs/code-review.md).

## Tech-Stack

| Bereich | Technologien |
|---|---|
| Backend | Java 23, Spring Boot 3.4, Spring Security (JWT), Spring Data JPA/Hibernate, Lombok |
| Datenbank | PostgreSQL (Supabase-gehostet) |
| Dateispeicher | Supabase Storage (S3-kompatibel, AWS SDK v2) für Raumfotos |
| PDF-Erzeugung | OpenPDF (Rechnungen) |
| Frontend | Next.js 15 (App Router), React 19, TypeScript, Tailwind CSS v3, NextAuth |
| Infrastruktur | Docker Compose (Backend- + Frontend-Container), GitHub Actions CI |

## Funktionsumfang

- **Registrierung & Login** auf einer gemeinsamen Seite mit Tabs; JWT-Authentifizierung mit
  Access-/Refresh-Token, Passwort-Reset per E-Mail-Adresse (Token wird aktuell geloggt statt
  gemailt — siehe Einschränkungen).
- **Zwei Kundentypen**: private Kunden (Vor-/Nachname) und Organisationen (Organisationsname).
  Organisationen erhalten automatisch 10 % Rabatt auf jeden Raumpreis, können dafür aber keine
  Rabattcodes einlösen.
- **Raumverwaltung**: Anlegen/Bearbeiten durch Admins, mehrere Fotos pro Raum (Galerie,
  Titelbild), Soft-Delete (Deaktivieren statt Löschen) — deaktivierte Räume bleiben für Admins
  sichtbar, verschwinden aber für Kunden.
- **Buchungen**: Zeitraum-Auswahl mit Überlappungsprüfung, Live-Anzeige "Gebucht bis …" auf der
  Raumliste, Admins können im Namen eines Kunden buchen (per E-Mail-Suche). Laufende oder bereits
  bezahlte Buchungen können nicht mehr gelöscht werden.
- **Zahlungen & Rechnungen**: Jede Buchung erzeugt eine Zahlung (offen/bezahlt); Admins
  bestätigen Zahlungen manuell, was automatisch eine Rechnung erzeugt. Kunden können die Rechnung
  als PDF herunterladen.
- **Rabattcodes**: prozentual oder absolut, mit Gültigkeitszeitraum, nur für Nicht-Organisationen.
- **Admin-Dashboard**: Kennzahlen (verfügbare/belegte Räume, Nutzerzahl, Umsatz), offene
  Zahlungen, meistgebuchte Räume, aktivste Kunden — vollständig responsive inkl.
  Off-Canvas-Sidebar auf Mobilgeräten.

## Projektstruktur

```
room_booking_system/
├── bookingssystem/     Spring-Boot-Backend (REST-API)
├── react_frontend/     Next.js-Frontend
├── docs/                Projekt-Dokumentation (Architektur, Requirements, Code-Review)
└── docker-compose.yml   Startet Backend- und Frontend-Container
```

## Lokal starten

Voraussetzung: Docker Desktop, ein Supabase-Projekt (Postgres-Datenbank; Storage-Bucket
`room-images` optional für Fotos).

1. **Backend-Konfiguration** — `bookingssystem/.env` anlegen (siehe
   `bookingssystem/.env.example` als Ausgangspunkt) mit mindestens:
   - `SPRING_DATASOURCE_PASSWORD` — Passwort der Supabase-Datenbank
   - `SECURITY_JWT_SECRET` — langer, zufälliger String (≥ 32 Zeichen)
   - optional `SUPABASE_S3_ACCESS_KEY` / `SUPABASE_S3_SECRET_KEY` für Raumfoto-Uploads

   Die Datenbank-URL, das Storage-Projekt und der Datenbank-Benutzername stehen bereits fest in
   `docker-compose.yml` (Supabase Session-Pooler) — dort ggf. auf das eigene Supabase-Projekt
   anpassen.

2. **Root-Konfiguration** — `.env` im Projekt-Root anlegen (siehe `.env.example`) mit:
   - `NEXTAUTH_SECRET` — langer, zufälliger String
   - `NEXTAUTH_URL=http://localhost:3000`
   - `NEXT_PUBLIC_BACKEND_URL=http://localhost:8080`

3. **Starten**:
   ```bash
   docker compose up -d --build
   ```
   Frontend: [http://localhost:3000](http://localhost:3000) — Backend: [http://localhost:8080](http://localhost:8080)

4. Ein Admin-Account muss einmalig direkt in der Datenbank angelegt werden (Registrierung
   erstellt immer nur normale Kunden-Accounts) — `role` der `users`-Tabelle auf `ADMIN` setzen.

## Entwicklung ohne Docker

- Backend: `cd bookingssystem && ./mvnw spring-boot:run` (benötigt dieselben Umgebungsvariablen
  wie oben, z. B. über die IDE-Run-Configuration).
- Frontend: `cd react_frontend && npm install && npm run dev`.

Details zu einzelnen Teilprojekten: [`bookingssystem/`](bookingssystem) folgt der Standard-Maven-
Struktur; [`react_frontend/README.md`](react_frontend/README.md) beschreibt das Frontend im Detail.

## Tests

```bash
cd bookingssystem && ./mvnw test      # Unit- und Integrationstests (H2, kein Docker nötig)
cd react_frontend && npm run build    # Type-Check + Produktions-Build
```

CI (`.github/workflows/ci.yml`) führt beides bei jedem Push/PR automatisch aus.

## Bekannte Einschränkungen

Siehe [`docs/code-review.md`](docs/code-review.md) für die vollständige, priorisierte Liste. Die
wichtigsten Punkte:

- Kein echter E-Mail-Versand — Passwort-Reset-Links werden aktuell nur geloggt.
- Keine echte Zahlungsanbieter-Anbindung — Zahlungen werden von Admins manuell bestätigt.
- Einige Schreiboperationen laufen ohne Datenbank-Transaktion; siehe Code-Review für Details.

