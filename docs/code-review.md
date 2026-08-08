# Code Review — Raumbuchungssystem

Vollständiger Review von Backend (`bookingssystem/`) und Frontend (`react_frontend/`), Stand 2026-08-08.
Jede Datei in `src/main` bzw. `src/app` wurde gelesen (nicht nur überflogen). Ziel: Korrektheit,
Sicherheit, Konsistenz, toter Code, Testabdeckung, Performance.

Schweregrade: **HOCH** (kann Daten/Geld/Zugriff falsch machen oder die App lahmlegen),
**MITTEL** (spürbarer Bug/Schwäche, aber begrenzter Radius), **NIEDRIG** (Politur/Konsistenz).

---

## 1. Backend — Korrektheit

### 1.1 `updateBooking` setzt den Raum auf `null`, wenn keiner mitgeschickt wird — HOCH
`booking/service/BookingServiceImpl.java:49-62`

Der Fallback-Raum wird nur für die Überlappungsprüfung berechnet, aber `booking.setRoom(newBooking.getRoom())`
wird immer ausgeführt. Ein `PUT /booking/update/{id}/{userId}` ohne `room`-Feld im Body setzt
`room = null` → Verstoß gegen `nullable=false` → unbehandelte 500. Betroffen: nur der (im Frontend
ungenutzte) Update-Endpunkt, aber die Methode ist erreichbar.

### 1.2 `updateBooking` berechnet den Preis nie neu — HOCH
Gleiche Methode. Ändert man Datum/Raum einer Buchung, bleibt `payment.amount` unverändert. Eine
15€-Buchung lässt sich so auf den 200€-Raum für 30 Nächte "umbuchen", ohne dass sich der Preis
ändert.

### 1.3 `updateBooking` hat keine Datumsprüfung und keine Bezahlt/Laufend-Sperre — HOCH
Im Gegensatz zu `createBookingFor`/`deleteBooking` validiert diese Methode weder `start &lt;= end`
noch verhindert sie das Verschieben einer bereits bezahlten oder laufenden Buchung.

### 1.4 Buchung + Zahlung werden ohne Transaktion geschrieben — HOCH
`booking/service/BookingServiceImpl.java:147-174`. Im ganzen Projekt gibt es **kein einziges**
`@Transactional`. Die Buchung wird gespeichert, *bevor* der Rabattcode geprüft wird — schlägt die
Prüfung fehl (ungültiger Code), bleibt eine Buchung ganz ohne Zahlung in der DB stehen und blockiert
den Raum dauerhaft ("Geister-Buchung").

### 1.5 Race Condition bei der Überlappungsprüfung — HOCH
`booking/service/BookingServiceImpl.java:180-185`. Kein DB-Constraint, keine Transaktion. Zwei
gleichzeitige Buchungsanfragen für denselben Raum/Zeitraum können beide durchkommen → Doppelbuchung.

### 1.6 Rechnungsnummern-Vergabe ist COUNT-basiert und kollisionsanfällig — HOCH
`invoice/service/InvoiceServiceImpl.java:33-35`: `countByInvoiceNumberStartingWith(prefix) + 1`.
Zwei gleichzeitige Zahlungsbestätigungen können dieselbe Nummer berechnen → Unique-Constraint-
Verletzung → 500, obwohl die Zahlung schon als `PAID` gespeichert wurde. Diese Buchung bekommt nie
eine Rechnung (erneutes Bestätigen wirft `"Payment is already marked as paid"`).

### 1.7 `Collectors.toMap` wirft bei zwei überlappenden Buchungen für denselben Raum — MITTEL/HOCH
`room/service/RoomServiceImpl.java:64-67`. Sobald (z. B. durch 1.5) zwei Buchungen für denselben
Raum heute aktiv sind, liefert `GET /room/Get` — die zentrale Raumliste — **HTTP 409** mit einer
internen Java-Fehlermeldung statt der Raumliste. Die ganze Raumübersicht ist dann für alle down.

### 1.8 `@Data` auf beiden Seiten von `Booking` ↔ `Payment` — MITTEL/HOCH
`booking/model/Booking.java` und `payment/model/Payment.java` referenzieren sich gegenseitig und
haben beide Lombok `@Data`. `toString()`/`equals()`/`hashCode()` können sich gegenseitig endlos
aufrufen → `StackOverflowError` bei jedem Debug-Log oder jeder `Set`-Nutzung.

### 1.9 Doppelte Benutzernamen möglich → Admin-Account kann lahmgelegt werden — HOCH
`user/model/User.java:24`: `username` hat kein `unique = true`, keine DB-Unique-Constraint.
`UserServiceImpl.updateUser` prüft beim Ändern des eigenen Benutzernamens nicht auf Kollision.
Jedes Mitglied kann sich per `PUT /user/update/{eigeneId}` den Benutzernamen eines Admins
zulegen → `findByUsername` liefert dann zwei Treffer → `IncorrectResultSizeDataAccessException`
→ **jede Anfrage des echten Admins schlägt fehl** (Login, Auth-Check, alles). Faktisch ein
Denial-of-Service gegen ein fremdes Konto, auslösbar von jedem Mitglied.

### 1.10 "Nächte"-Berechnung ist eigentlich eine inklusive Tageszählung — MITTEL
`booking/service/BookingServiceImpl.java:155`: `DAYS.between(start, end) + 1`. Eine Buchung
1.–3. Januar wird als 3 Nächte berechnet, nicht 2. In sich konsistent (Ganztags-Modell), aber der
Feldname `pricePerNight` und die Variable `nights` verschleiern das.

### 1.11 Rabattcode-Rundung fehlt — NIEDRIG/MITTEL
`discount/service/DiscountCodeServiceImpl.java:64-65` rundet nicht auf 2 Nachkommastellen (im
Gegensatz zu `CustomerType.applyPricing`, das `setScale(2, HALF_UP)` macht). Führt zu
Rundungsabweichungen zwischen angezeigtem und gespeichertem Betrag.

### 1.12 Weitere kleinere Korrektheitspunkte
- Monatsumsatz auf dem Dashboard zählt die Monatsgrenze doppelt (`AdminDashboardServiceImpl.java:45-47`, inklusive JPQL-`between`).
- `updateRoom` kann `roomStatus` versehentlich auf `null` setzen, wenn das Feld im Request fehlt.
- "Laufende Buchung"-Grenze: der letzte Tag zählt noch als laufend (wahrscheinlich beabsichtigt, aber nicht dokumentiert).

---

## 2. Backend — Sicherheit

### 2.1 Passwort-Reset-Token landet im Log — HOCH
`user/Controller/RegistrationLoginController.java:122`: `log.info(... token.getToken())`. Jeder
mit Zugriff auf die Logs (Docker-Logs, Hosting-Dashboard, Screenshot) kann damit **jeden Account**
innerhalb der 30-Minuten-Gültigkeit übernehmen. `/api/forgot-password` ist unauthentifiziert und
ungedrosselt — ein Angreifer kann sich für jeden Benutzernamen jederzeit einen frischen Token
erzeugen lassen.

### 2.2 Passwort-Reset widerruft keine bestehenden Sessions — HOCH
Nach einem Reset bleibt ein bereits ausgestelltes Refresh-Token bis zu **30 Tage** gültig. Ein
Angreifer mit gestohlenem Refresh-Token bleibt eingeloggt, obwohl das Passwort geändert wurde.

### 2.3 Keine Refresh-Token-Rotation, Klartext-Speicherung, kein Aufräumen — MITTEL
`security/RefreshTokenServiceImpl.java`. Jeder Login legt eine neue Zeile an, es gibt keinen
Cleanup-Job für abgelaufene Refresh-/Reset-Tokens.

### 2.4 CORS-Origin hart codiert auf `localhost:3000` — MITTEL
`user/configuration/SecurityConfig.java:55`. Kein Property-Hook — jedes andere Deployment-Ziel
für das Frontend bricht die Anmeldung, bis der Code geändert wird.

### 2.5 Login/Register/Update binden die JPA-Entity direkt als Request-Body — MITTEL
`RegistrationLoginController.loginUser`, `UserController.updateUser`, `BookingController.updateBooking`
nehmen `@RequestBody User`/`Booking` statt eines DTOs entgegen. Zusätzlich fängt der Login-Handler
`catch(Exception e)` — jeder interne Fehler (DB down, NPE) wird als "falsches Passwort" (401,
Klartext) maskiert.

### 2.6 Bild-Upload vertraut clientseitigen Metadaten — MITTEL
`storage/StorageServiceImpl.java:43-51`. Der `Content-Type`-Header wird geprüft, nicht der
tatsächliche Dateiinhalt — eine als `image/png` deklarierte `.html`-Datei landet im **öffentlichen**
Supabase-Bucket. Der Dateiname wird zudem nicht bereinigt (Pfad-Traversal in der S3-Key-Bildung
theoretisch möglich). Nur Admins erreichen den Endpunkt, das begrenzt den Schaden.

### 2.7 Keine Rate-Limits/Lockouts — MITTEL
Login, Passwort-Reset, Refresh und Registrierung sind alle offen und ungedrosselt. Mindestpasswort
6 Zeichen. Online-Brute-Force ist realistisch.

### 2.8 Deaktivierte Räume bleiben buchbar — MITTEL
`BookingServiceImpl.createBookingFor` prüft `room.isActive()` nicht, obwohl `RoomServiceImpl`
inaktive Räume für Kunden mit 404 versteckt. Wer die UUID kennt, kann einen deaktivierten Raum
trotzdem buchen.

### 2.9 Kleinere Punkte
- Supabase-Projekt-Referenz und DB-Benutzername sind in `docker-compose.yml` committet (Passwörter korrekt in `.env`, nicht committet).
- `spring.jpa.show-sql=true` standardmäßig aktiv — loggt jedes SQL-Statement in Produktion.
- `roomImageService.list` hat als einziger Bild-Endpunkt keine explizite Berechtigungsprüfung (nur "authenticated", nicht schlimm, aber inkonsistent).
- **IDOR-Check bestand:** Jeder ID-basierte Endpunkt wurde nachverfolgt — Berechtigung wird korrekt aus dem geladenen Objekt abgeleitet (nicht aus der URL). Kein IDOR gefunden — das ist der stärkste Teil des Backends.

---

## 3. Backend — Konsistenz & totes Gewicht

- **Flyway-Migrationen sind MySQL-DDL gegen eine Postgres-Produktionsdatenbank, und es gibt kein `V1`.**
  `V2`–`V6` nutzen `AUTO_INCREMENT`/`ENGINE=InnoDB`, obwohl jede Entity `UUID`-IDs hat und Produktion
  Supabase Postgres ist. Sie sind zudem unvollständig (kein `room_images`, keine der neuen
  `users.*`-Spalten, kein `room.active`). Produktiv sind sie ohnehin deaktiviert
  (`SPRING_FLYWAY_ENABLED=false`, `ddl-auto=update` in `docker-compose.yml`) — die Checked-in-Defaults
  in `application.properties` widersprechen dem tatsächlichen Deployment. `pom.xml` schleppt zudem
  weiterhin `mysql-connector-j`/`flyway-mysql` mit.
- **Drei verschiedene Fehler-Response-Formen**: `{"error": "..."}`, `{"feldname": "meldung"}` (Validierung),
  reiner `text/plain` beim Login-Fehler, plus Springs Standard-500-JSON für unbehandelte Exceptions.
- **`IllegalStateException` → 409 ist zu weit gefasst** — fängt auch interne Java-Fehler (siehe 1.7)
  und zeigt sie dem Nutzer als "Konflikt".
- **`{userId}`-Pfadparameter, die nichts bewirken**: `updateBooking`, `deleteBooking`, `getBooking`,
  `getBookings` nehmen alle einen `userId`-Pfadparameter entgegen, der von der Implementierung
  ignoriert wird (Berechtigung kommt aus der geladenen Buchung). Täuscht eine URL-Scoping-Garantie vor,
  die es nicht gibt.
- **Toter Code**: `booking/model/Datum.java` (Wochentags-Enum, nirgends benutzt),
  `JwtService.extractRoles`/`isExpired` (nie aufgerufen bzw. nie erreichbar), `UserService.addUser`
  (nirgends aufgerufen — und würde das Passwort **ungehasht** speichern, falls doch verdrahtet),
  mehrere sich überschneidende "ist der Raum heute gebucht"-Repository-Methoden.
- Gemischte Sprache in Bezeichnern/Meldungen (Deutsch/Englisch gemischt), französische Kommentare
  in `SecurityConfig.java` (mit Tippfehlern), `Controller`-Pakete groß geschrieben (Java-Konvention
  verletzt), gemischte `@Autowired`-Feld- vs. Konstruktor-Injection, `pom.xml` mit leeren
  Initializr-Stubs.

---

## 4. Backend — Testabdeckung

Vorhanden: Tests für Booking-Service (inkl. der 3 neuen Lösch-Sperre-Tests), Discount-Code-Service,
Payment-Service, AuthorizationService, Refresh-/Reset-Token-Services, zwei Integrationstests.

**Ungetestet, obwohl geschäftskritisch:**
- Die 10%-Organisationsrabatt-Logik (`CustomerType.applyPricing`) — keine einzige Assertion.
- `updateBooking` komplett — exakt die Methode mit den drei HOCH-Bugs oben.
- Das komplette `invoice`-Paket (Nummernvergabe, Berechtigung, PDF).
- `RoomServiceImpl` komplett (Soft-Delete-Sichtbarkeit, `bookedUntil`-Berechnung).
- `addBookingForCustomer` (Admin-Buchungspfad).
- Die Rabattcode-Sperre für Organisationen.
- Cross-User-Zugriff auf HTTP-Ebene (kein Integrationstest beweist, dass Nutzer B von Nutzer A's
  Buchung/Profil mit 403 abgewiesen wird).

---

## 5. Backend — Performance

- `GET /booking/getAll` als Admin lädt **alle** Buchungen ohne Paginierung; `@ManyToOne`/`@OneToOne`
  sind EAGER → ca. 3 zusätzliche Selects pro Buchung (N+1).
- Admin-Dashboard lädt **alle** Buchungen und **alle** Zahlungen komplett in den Speicher, nur um
  Top-5-Listen zu bilden — sollte `GROUP BY`-Queries mit `Pageable` sein.
- `getAllUsers` ohne Paginierung.
- `spring.jpa.open-in-view` nicht explizit gesetzt (Default `true`) — hält DB-Verbindungen länger offen als nötig.

---

## 6. Frontend — Korrektheit

### 6.1 Benutzername ändern zerschießt die eigene Session — HOCH
`profile/page.tsx:61`. Nach dem Ändern des Benutzernamens wird die NextAuth-JWT nicht aktualisiert.
Das Access-Token trägt weiter den alten Benutzernamen als `sub` — jede folgende Backend-Anfrage
scheitert mit 403, bis das Token nach ~60 Minuten automatisch erneuert wird. Für den Nutzer wirkt
die App bis dahin zufällig kaputt.

### 6.2 Buchungsstatus ist in West-Zeitzonen um einen Tag versetzt — HOCH
`components/BookingTable.tsx:25-40`. Ein reines Datum (`"2026-08-08"`) wird als UTC-Mitternacht
geparst und dann auf lokale Mitternacht "geschnappt" — westlich von UTC verschiebt das Start/Ende
um einen Tag zurück. In Deutschland unsichtbar, für Nutzer in anderen Zeitzonen (z. B. USA) zeigt
die UI eine Buchung fälschlich als "abgelaufen" und den Löschen-Button an, obwohl das Backend die
Buchung korrekt als laufend erkennt und mit 409 ablehnt.

### 6.3 "Ist diese Buchung löschbar"-Logik existiert doppelt und weicht dadurch ab — HOCH
`components/BookingTable.tsx:123` vs. Backend `BookingServiceImpl.java:70-77`. Zwei unabhängige
Implementierungen derselben Regel — durch 6.2 in der Praxis nicht immer synchron.

### 6.4 Zugriffstoken ist für clientseitiges JavaScript sichtbar — HOCH (bewusster Trade-off, aber undokumentiert)
`api/auth/[...nextauth]/route.ts:133`. Das Spring-JWT wird über `session.accessToken` an den
Browser durchgereicht (Refresh-Token bleibt korrekt serverseitig). Jede zukünftige XSS-Lücke würde
damit zur vollen Kontoübernahme für die Token-Laufzeit. Architektonisch vertretbar für ein
Browser→Backend-Direktmodell, sollte aber als bewusste Entscheidung dokumentiert sein.

### 6.5 Registrierungs-Fehlermeldung wird sofort wieder gelöscht — MITTEL
`login/page.tsx:92-95`. Schlägt der Auto-Login nach erfolgreicher Registrierung fehl, wird die
entsprechende Meldung gesetzt und im selben Atemzug durch `switchMode("login")` wieder gelöscht —
der Nutzer sieht gar nichts und weiß nicht, ob das Konto angelegt wurde.

### 6.6 Weitere Punkte
- Nur der Admin-Buchungspfad nutzt `extractErrorMessage` — Mitglieder sehen bei jedem
  Buchungsfehler (z. B. Raum bereits belegt) nur eine generische Meldung statt des konkreten Grunds.
- Kein Formular außer Login/Profil/Reset-Passwort deaktiviert den Submit-Button während des
  Requests → Doppel-Submits möglich (doppelte Räume, doppelte Buchungsversuche).
- Rechnungs-Download: der Blob-Link wird nie ins DOM gehängt und die Object-URL sofort widerrufen
  — funktioniert in Chrome, kann in Firefox den Download abbrechen.
- `Room`-TypeScript-Typ deklariert `effectivePricePerNight` als Pflichtfeld, obwohl der rohe
  `Booking.room` (vom Backend) dieses Feld gar nicht mitliefert — bei zukünftiger Nutzung würde das
  zu `"NaN €"` führen.
- Kein Datum wird mit `Intl.DateTimeFormat` formatiert — überall rohe ISO-Strings
  (`Zeitraum: 2026-08-08 – 2026-08-10`) in einer sonst komplett deutschen Oberfläche.
- Benutzer löschen im Admin-Bereich hat keine Bestätigung (`ConfirmDialog` existiert bereits, wird
  aber nur für Logout verwendet) und keine Fehlerrückmeldung bei Misserfolg.
- Keine serverseitige Routen-Absicherung (`middleware.ts` fehlt) — rein clientseitige Redirects.
  **Wichtig:** das Backend setzt alle relevanten Berechtigungen unabhängig durch, es handelt sich
  also nicht um eine Sicherheitslücke, nur um unnötige Anfragen/Flackern für nicht berechtigte
  Nutzer.

---

## 7. Frontend — Barrierefreiheit & Politur

- `ConfirmDialog` ist kein zugängliches Dialog-Element (kein `role="dialog"`, kein Fokus-Trap, kein
  Escape-Handler).
- Einige Bild-Buttons (Galerie-Miniaturen) haben kein `aria-label` und leeres `alt=""`.
- Vier verschiedene Fehler-/Bestätigungs-Idiome parallel im Einsatz: `Alert`-Komponente,
  `alert()`, `window.confirm()`, `ConfirmDialog` — sollte auf `Alert` + `ConfirmDialog` vereinheitlicht werden.
- Statisches Zimmer-Bild-Mapping (`lib/roomImages.ts`) mit 13 fest verdrahteten Namen ist Altlast,
  seit das Backend echte `imageUrl`s liefert.

---

## 8. Priorisierte Empfehlung (falls als Nächstes angegangen werden soll)

1. **`updateBooking` reparieren oder entfernen** (1.1–1.3) — aktuell drei zusammenhängende Bugs in einer ungenutzten, aber erreichbaren Methode.
2. **Transaktionen einführen** (`@Transactional` auf den schreibenden Service-Methoden) — behebt 1.4 und mindert 1.5/1.6.
3. **Eindeutigkeit von `username`** auf DB-Ebene erzwingen (1.9) — einfache, hochwirksame Änderung.
4. **Passwort-Reset-Token nicht loggen** (2.1) — trivial zu fixen, hohes Risiko.
5. **Session nach Benutzernamen-Änderung aktualisieren** (6.1) — betrifft jeden Nutzer, der sein Profil bearbeitet.
6. **Zeitzonen-sichere Datumsvergleiche im Frontend** (6.2) statt `new Date(isoString)`.
7. Rest nach Zeit/Interesse — die Tabellen oben sind vollständig genug, um einzeln priorisiert zu werden.

Kein Punkt in diesem Dokument wurde automatisch behoben — dies ist ausschließlich der Befund.
