# Projekt-Dokumentation

Technischer Überblick über Architektur, Datenmodell, API und Geschäftsregeln von Spacio.
Für Setup/Quickstart siehe die [Root-README](../README.md). Für formale
Anforderungen siehe [`requirements-engineering.md`](requirements-engineering.md), für den
vollständigen Code-Review siehe [`code-review.md`](code-review.md).

## Architektur

```
 Browser
   │  HTTPS
   ▼
 Next.js-Frontend (React 19, App Router)
   │  NextAuth: Login läuft serverseitig über NextAuth gegen das Backend,
   │  danach ruft der Browser das Backend direkt mit Bearer-Token auf
   ▼
 Spring-Boot-Backend (REST, JWT-geschützt)
   │
   ├──► PostgreSQL (Supabase-gehostet)      – alle Fachdaten
   └──► Supabase Storage (S3-kompatibel)    – Raumfotos (optional)
```

Frontend und Backend sind unabhängig deploybare Anwendungen, die ausschließlich über die REST-API
kommunizieren. Es gibt keinen geteilten Code zwischen beiden — Typen/DTOs werden auf beiden Seiten
separat gepflegt (Fehlerquelle, siehe Code-Review).

## Rollen & Kundentypen

Zwei orthogonale Dimensionen auf `User`:

- **Rolle** (`UserRole`: `ADMIN` / `MEMBER`) steuert **Berechtigungen**. Registrierung erzeugt
  immer `MEMBER`; `ADMIN` kann nur direkt in der Datenbank vergeben werden, es gibt keinen
  API-Weg dorthin.
- **Kundentyp** (`CustomerType`: `KUNDE` / `ORGANISATION`) steuert **Preisgestaltung**, nur bei
  `MEMBER`-Accounts relevant. `ORGANISATION` zahlt 10 % weniger auf jeden Raumpreis, darf dafür
  keine Rabattcodes einlösen.

| | Räume ansehen | Raum buchen | Raum verwalten | Für Kunden buchen | Admin-Bereich |
|---|---|---|---|---|---|
| **MEMBER** | eigene aktive Räume | ja, für sich selbst | nein | nein | nein |
| **ADMIN** | alle Räume (auch deaktivierte) | **nein** (bewusst gesperrt) | ja | ja, per E-Mail-Suche | ja |

Admins können sich also nicht selbst Räume buchen — jede Buchung für einen Admin-Zweck läuft über
den Kunden-Buchungs-Endpunkt mit einer Kunden-E-Mail.

## Datenmodell

| Entität | Wichtigste Felder | Beziehungen |
|---|---|---|
| `User` | username, password (BCrypt), role, customerType, organisationName / firstName+lastName, phoneNumber | 1–n Booking, RefreshToken, PasswordResetToken |
| `Room` | name, capacity, location, pricePerDay, roomStatus, imageUrl, active | 1–n RoomImage, 1–n Booking |
| `RoomImage` | imageUrl, position | n–1 Room |
| `Booking` | startTime, endTime, createdAt | n–1 Room, n–1 User, 1–1 Payment |
| `Payment` | amount, status (PENDING/PAID), appliedDiscountCode, paidAt | 1–1 Booking, 1–1 Invoice |
| `Invoice` | invoiceNumber, customerUsernameSnapshot, roomNameSnapshot, amount, invoiceDate | 1–1 Payment |
| `DiscountCode` | code, type (PERCENT/ABSOLUTE), value, validFrom, validUntil, active | keine (nur als String in `Payment.appliedDiscountCode` referenziert) |
| `RefreshToken`, `PasswordResetToken` | token, expiresAt, revoked/used | n–1 User |

Alle IDs sind UUIDs. Geldbeträge sind `DECIMAL(10,2)`. Enums werden konsequent als String
persistiert (`@Enumerated(EnumType.STRING)`).

## Wichtige Geschäftsregeln

- **Buchungspreis**: `Tage = Tage(Start, Ende) + 1` (beide Enden eingeschlossen, jeder gehaltene
  Kalendertag wird berechnet — ein Raum wird tage-, nicht nächteweise reserviert) ×
  `pricePerDay`, danach ggf. 10 % Organisationsrabatt, danach ggf. Rabattcode. Organisationen
  können nie einen Rabattcode zusätzlich einlösen.
- **Überlappungsprüfung**: Ein Raum kann nicht doppelt für sich überschneidende Zeiträume gebucht
  werden (Backend lehnt mit HTTP 409 ab).
- **Buchung löschen**: nicht möglich, wenn die Buchung bereits bezahlt ist **oder** ihr Zeitraum
  den heutigen Tag einschließt — gilt für Kunden **und** Admins gleichermaßen.
- **Raum-Soft-Delete**: Deaktivierte Räume verschwinden für Kunden (404), bleiben für Admins
  sichtbar und bearbeitbar. Es gibt keinen Hard-Delete.
- **Zahlung → Rechnung**: Erst wenn ein Admin eine Zahlung manuell bestätigt, wird automatisch
  eine Rechnung mit fortlaufender Nummer (`INV-<Jahr>-<6-stellig>`) erzeugt; die Rechnungsdaten
  (Kundenname, Raumname, Betrag) werden zum Bestätigungszeitpunkt eingefroren ("Snapshot").
- **Token-Laufzeiten**: Access-Token 60 Minuten, Refresh-Token 30 Tage, Passwort-Reset-Token 30
  Minuten (alle über Umgebungsvariablen konfigurierbar).

Details und Grenzfälle: [`code-review.md`](code-review.md) Abschnitt 6 ("Business rules as
actually implemented").

## API-Überblick

Alle Endpunkte außer `/api/register`, `/api/login`, `/api/refresh`, `/api/logout`,
`/api/forgot-password`, `/api/reset-password` erfordern einen gültigen Bearer-Token.

| Ressource | Basis-Pfad | Zugriff |
|---|---|---|
| Auth | `/api/*` | öffentlich |
| Benutzer | `/user/*` | eigenes Profil oder Admin |
| Räume | `/room/*` | Lesen: eingeloggt · Schreiben/Fotos: nur Admin |
| Buchungen | `/booking/*` | eigene Buchungen oder Admin |
| Zahlungen | `/payment/*` | Lesen: Besitzer/Admin · Bestätigen: nur Admin |
| Rechnungen | `/invoice/*` | Besitzer/Admin (JSON + PDF-Download) |
| Rabattcodes | `/discount-code` | nur Admin |
| Admin-Dashboard | `/admin/dashboard` | nur Admin |

Vollständige Endpunktliste mit HTTP-Methoden und exakten Berechtigungsregeln:
[`code-review.md`](code-review.md) Abschnitt "REST-Endpunkte".

## Frontend-Struktur

Siehe [`../react_frontend/README.md`](../react_frontend/README.md) für Ordnerstruktur und
Entwicklungsbefehle. Kurzfassung: Next.js App Router, ein API-Client-Modul pro Backend-Ressource
unter `src/app/api/`, gemeinsame UI-Bausteine in `src/app/components/ui.tsx`, Admin-Bereich mit
eigenem Layout/Rollen-Guard unter `src/app/admin/`.

## Deployment

Docker Compose startet zwei Container (`backend`, `frontend`); es gibt **keinen**
Datenbank-Container — beide sprechen mit einer extern gehosteten Supabase-Postgres-Instanz.
Schema-Änderungen laufen über Hibernates `ddl-auto=update` gegen die Produktionsdatenbank (Flyway
ist im Compose-Setup deaktiviert, siehe [`code-review.md`](code-review.md) Abschnitt 3 für den
Hintergrund und die damit verbundenen Risiken). CI (`.github/workflows/ci.yml`) baut und testet
beide Teilprojekte bei jedem Push.
