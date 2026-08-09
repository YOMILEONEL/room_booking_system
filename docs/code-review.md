# Code Review — Spacio

Vollständiger Review von Backend (`bookingssystem/`) und Frontend (`react_frontend/`), Stand 2026-08-08.
Jede Datei in `src/main` bzw. `src/app` wurde gelesen (nicht nur überflogen). Ziel: Korrektheit,
Sicherheit, Konsistenz, toter Code, Testabdeckung, Performance.

Schweregrade: **HOCH** (kann Daten/Geld/Zugriff falsch machen oder die App lahmlegen),
**MITTEL** (spürbarer Bug/Schwäche, aber begrenzter Radius), **NIEDRIG** (Politur/Konsistenz).

---

## 1. Backend — Korrektheit

### 1.1–1.3 `updateBooking` — behoben (entfernt)
`PUT /booking/update/{id}/{userId}` war im Frontend komplett ungenutzt (kein Client-Aufruf, keine
UI zum Bearbeiten einer bestehenden Buchung), aber erreichbar und trug drei gekoppelte Bugs:
Raum konnte auf `null` gesetzt werden, der Preis wurde nie neu berechnet, und es fehlten
Datums-/Bezahlt-Laufend-Sperren. Da es keinen Produktbedarf für "Buchung nachträglich verschieben"
gibt, wurde die Methode entfernt statt repariert (`BookingController`, `BookingService`,
`BookingServiceImpl`) — damit entfällt die Angriffsfläche vollständig, ohne eine ungenutzte
Funktion mit drei Korrektheitsbugs am Leben zu halten. Sollte "Buchung verschieben" später als
echtes Feature gebraucht werden, muss die Preisneuberechnung und die Sperr-Logik aus
`createBookingFor`/`deleteBooking` konsequent mitgebaut werden.

### 1.4 Buchung + Zahlung werden ohne Transaktion geschrieben — behoben
`booking/service/BookingServiceImpl.java`. `addBooking` und `addBookingForCustomer` (und damit die
gemeinsam genutzte `createBookingFor`) sowie `deleteBooking` sind jetzt `@Transactional`. Schlägt
die Rabattcode-Prüfung fehl (ungültiger Code), rollt die gesamte Buchung inklusive Zahlung zurück
statt als "Geister-Buchung" ohne Zahlung stehen zu bleiben. Regressionstest:
`integration/BookingConflictIntegrationTest.bookingWithInvalidDiscountCode_leavesNoGhostBooking`
(gegen die Version ohne `@Transactional` verifiziert — schlägt dort fehl, ist jetzt grün).

### 1.5 Race Condition bei der Überlappungsprüfung — MITTEL (gemindert, nicht vollständig behoben)
`booking/service/BookingServiceImpl.java`. Überlappungsprüfung und Buchungs-Insert laufen jetzt in
derselben Transaktion, aber ohne Locking (`SELECT ... FOR UPDATE`) oder einen DB-seitigen
Exclusion-Constraint schützt Postgres' Standard-Isolationsstufe (`READ COMMITTED`) nicht davor,
dass zwei parallele Transaktionen beide die Überlappungsprüfung bestehen, bevor eine von beiden
committet. Für eine vollständige Lösung wäre ein `EXCLUDE`-Constraint auf `(room_id, daterange)`
oder pessimistisches Locking auf dem Raum nötig — bewusst nicht Teil dieser Änderung.

### 1.6 Rechnungsnummern-Vergabe ist COUNT-basiert und kollisionsanfällig — MITTEL (gemindert, nicht vollständig behoben)
`invoice/service/InvoiceServiceImpl.java:33-35`: `countByInvoiceNumberStartingWith(prefix) + 1`
bleibt kollisionsanfällig. Aber `PaymentServiceImpl.confirmPayment` ist jetzt `@Transactional`:
schlägt die Rechnungserzeugung an einer Nummernkollision fehl, rollt der `PAID`-Status mit zurück —
die Zahlung bleibt `PENDING` und lässt sich erneut bestätigen, statt dauerhaft "bezahlt, aber ohne
Rechnung" hängen zu bleiben. Die eigentliche Kollisionsquelle (COUNT statt DB-Sequence) besteht
weiter.

### 1.7 `Collectors.toMap` wirft bei zwei überlappenden Buchungen für denselben Raum — behoben
`RoomServiceImpl.findAllRooms` nutzt jetzt `Collectors.toMap(keyMapper, valueMapper, mergeFunction)`
mit einer Merge-Funktion (behält das spätere Enddatum) statt der Zweier-Variante, die bei einem
doppelten Key `IllegalStateException` wirft. Dieselbe Fehlerklasse steckte auch in
`findRoomById`: `BookingRepository.findByRoom_IdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual`
gab `Optional<Booking>` zurück — bei zwei Treffern hätte Spring Data
`IncorrectResultSizeDataAccessException` geworfen. Repository-Methode auf `List<Booking>`
umgestellt, Service nimmt das späteste Enddatum. Regressionstests in `RoomServiceImplTest`
(`findAllRooms_survivesTwoActiveBookingsForTheSameRoom`,
`findRoomById_survivesTwoActiveBookingsForTheSameRoom`) — gegen die Version ohne Fix verifiziert.

### 1.8 `@Data` auf beiden Seiten von `Booking` ↔ `Payment` — behoben
`Payment.booking` hat jetzt `@ToString.Exclude` und `@EqualsAndHashCode.Exclude` (nur auf einer
Seite der Beziehung nötig, um den Zyklus zu brechen — `Booking`s generierte Methoden rufen weiter
`Payment` auf, aber `Payment`s Methoden rufen nicht mehr zurück in `Booking`). Regressionstest
`BookingPaymentCircularReferenceTest` — ruft `toString()`/`equals()`/`hashCode()` auf einer
Buchung mit Zahlung auf und prüft in einem `HashSet`; ohne den Fix wirft das `StackOverflowError`
(gegen die Version ohne Fix verifiziert).

### 1.9 Doppelte E-Mail-Adressen möglich → Admin-Account kann lahmgelegt werden — behoben
`User.email` (umbenannt von `username`, siehe Session-Historie) hat jetzt `@Column(unique = true)`
als harten DB-Constraint. `UserServiceImpl.updateUser` prüft zusätzlich auf Anwendungsebene auf
Kollision (schöne 400-Fehlermeldung statt roher Constraint-Verletzung), und
`GlobalExceptionHandler` fängt `DataIntegrityViolationException` als Backstop für den
verbleibenden Race (zwei parallele Requests) mit einem sauberen 409 ab. Regressionstests:
`UserServiceImplTest` (Anwendungsebene, inkl. false-positive-Check bei unveränderter/eigener
E-Mail) und `integration/EmailCollisionIntegrationTest` (End-to-End über die API) — beide gegen
die Version ohne Fix verifiziert.

**Achtung vor dem Deploy:** `ddl-auto=update` erzeugt daraus `ALTER TABLE users ADD CONSTRAINT
... UNIQUE (email)`. Falls in der Produktions-DB bereits doppelte E-Mail-Adressen existieren
(z. B. genau durch diesen Bug entstanden), schlägt dieses ALTER TABLE fehl und rollt komplett
zurück (gleiche Fehlerklasse wie beim `city`/`description`-Rollout weiter oben in der
Session-Historie) — vorher in Supabase prüfen:
```sql
SELECT email, COUNT(*) FROM users GROUP BY email HAVING COUNT(*) > 1;
```
Gibt es Treffer, müssen die doppelten Zeilen erst manuell bereinigt werden, bevor der Backend-
Container mit dieser Änderung neu gestartet wird.

### 1.10 "Nächte"-Berechnung ist eigentlich eine inklusive Tageszählung — behoben
Bereits vor diesem Durchgang in der Session-Historie gelöst: das komplette Feld/Variable/UI-Wording
wurde von `pricePerNight`/`nights`/"Nacht" auf `pricePerDay`/`days`/"Tag" umbenannt (Backend, Frontend,
Docs, DB-Spalte). Dieser Eintrag war beim letzten Doku-Durchgang übersehen worden, ist aber seit der
entsprechenden Umbenennung erledigt — keine weitere Änderung in diesem Durchgang nötig.

### 1.11 Rabattcode-Rundung fehlt — behoben
`DiscountCodeServiceImpl.applyDiscount` rundet jetzt beide Zweige (PERCENT/ABSOLUTE) mit
`.setScale(2, RoundingMode.HALF_UP)`, genau wie `CustomerType.applyPricing`. Regressionstest
`applyDiscount_percentRoundsToTwoDecimals` (99.99 € − 10 % = 89.991 € ungerundet, muss auf 89.99 €
runden).

### 1.12 Weitere kleinere Korrektheitspunkte
- **Behoben:** Monatsumsatz zählte die Monatsgrenze doppelt — `PaymentRepository.sumRevenueBetween`
  nutzte JPQL `between` (inklusiv auf beiden Seiten), obwohl der Aufrufer (`AdminDashboardServiceImpl`)
  den *Start des Folgemonats* als oberes Ende übergibt. Auf halboffenes Intervall (`>= start and <
  end`) umgestellt. Regressionstest `PaymentRepositoryTest` (`@DataJpaTest` gegen H2, echte Query
  gegen die JPQL) — eine Zahlung exakt auf der Monatsgrenze wurde vorher in beiden Monaten gezählt,
  jetzt nur noch im richtigen; gegen die Version mit `between` verifiziert.
- **Behoben:** `updateRoom` setzt `roomStatus` nur noch, wenn das Feld im Request tatsächlich
  mitgeschickt wurde (analog zu den anderen optionalen Feldern). Regressionstests
  `updateRoom_omittingRoomStatusKeepsTheExistingStatus` / `..._providingRoomStatusUpdatesIt`.
- **Nicht verändert:** "Laufende Buchung"-Grenze (letzter Tag zählt noch als laufend) — das ist
  reines Verhalten, kein Bug, und wird an mehreren Stellen (Löschsperre, Statusanzeige) konsistent
  so gehandhabt. Nur eine Dokumentationslücke, keine Code-Änderung nötig.

---

## 2. Backend — Sicherheit

### 2.1 Passwort-Reset-Token landet im Log — teilweise behoben
Das Loggen des rohen Tokens ist jetzt standardmäßig aus (`app.password-reset.log-token`,
Default `false`) — ohne die Property loggt `RegistrationLoginController.forgotPassword` nur noch,
*dass* ein Token erzeugt wurde, nicht mehr seinen Wert. Regressionstest:
`RegistrationLoginControllerResetTokenLoggingTest` (prüft beide Zustände der Property direkt
gegen den Log-Output).

**Nicht behoben, weil es kein triviales Ein/Aus ist:** Dieses Projekt hat keinen Mailversand
(kein SMTP/Resend), das Logging war der einzige Weg, den Token überhaupt aus dem System zu
bekommen. Da `docker-compose.yml` aktuell die einzige Deployment-Variante ist (kein separates
"nur lokal"-Setup), ist die Property dort bewusst auf `"true"` gesetzt, mit einem Kommentar,
der erklärt, dass das ein Provisorium ist und bei echtem Mailversand entfernt werden muss.
Solange das so bleibt, gilt die zweite Hälfte des ursprünglichen Befunds unverändert: Jeder mit
Zugriff auf `docker logs booking-backend` kann weiterhin **jeden Account** innerhalb der
30-Minuten-Gültigkeit übernehmen, und `/api/forgot-password` bleibt unauthentifiziert und
ungedrosselt (separat als 2.7 "Keine Rate-Limits/Lockouts" erfasst). Vollständig gelöst ist das
erst mit echtem E-Mail-Versand.

### 2.2 Passwort-Reset widerruft keine bestehenden Sessions — behoben
`RegistrationLoginController.resetPassword` ruft nach dem Speichern des neuen Passworts jetzt
`refreshTokenService.revokeAllForUser(user)` auf (neue Methode: `RefreshTokenRepository
.findByUser_IdAndRevokedFalse` + alle auf `revoked = true` setzen). Bewusst **nicht** auch in
`UserServiceImpl.updateUser`s Passwort-Änderungspfad eingebaut: dort ist der Aufrufer selbst
eingeloggt, ein pauschales Widerrufen würde die eigene laufende Session sofort abmelden — das
wäre eine spürbare UX-Regression ohne im Review explizit gefordert zu sein, und bräuchte ein
Konzept für "aktuelle Session ausnehmen", das hier nicht gebaut wurde. Live end-to-end verifiziert:
Refresh-Token vor Reset funktioniert (200), nach Reset mit demselben Token → 401. Regressionstest
`integration/PasswordResetRevokesSessionsIntegrationTest`.

### 2.3 Keine Refresh-Token-Rotation, Klartext-Speicherung, kein Aufräumen — teilweise behoben
**Aufräumen:** neuer `ExpiredTokenCleanupScheduler` (`@Scheduled(cron = "0 0 3 * * *")`, täglich
03:00) löscht abgelaufene Refresh- und Passwort-Reset-Tokens über neue
`deleteExpired()`-Methoden auf beiden Services (`findByExpiresAtBefore` + `deleteAll`). Ein
Fehlschlag eines Laufs wird geloggt statt den Scheduler-Thread zu crashen (sonst würde ein
einziger DB-Ausfall alle künftigen Läufe stillschweigend beenden). `@EnableScheduling` auf
`BookingssystemApplication` ergänzt. Regressionstests: `RefreshTokenServiceImplTest`,
`PasswordResetTokenServiceImplTest`, `ExpiredTokenCleanupSchedulerTest` (inkl. Exception-Fall).

**Nicht behoben:** Token-Rotation (bei jedem Refresh ein neues Refresh-Token ausstellen und das
alte invalidieren) und die Klartext-Speicherung in der DB — beides würde Backend- **und**
Frontend-Änderungen brauchen (`/api/refresh` liefert aktuell nur `{accessToken}`, der Client
müsste ein neues Refresh-Token entgegennehmen und speichern können) und ist damit ein größerer,
eigenständiger Umbau, bewusst nicht Teil dieses Durchgangs.

### 2.4 CORS-Origin hart codiert auf `localhost:3000` — behoben
Neue Property `app.cors.allowed-origins` (Default `http://localhost:3000`, damit sich am
aktuellen Deployment nichts ändert), kommagetrennt für mehrere Origins (z. B. Staging + Prod).
`SecurityConfig.corsConfigurationSource()` parst und trimmt die Liste. Regressionstest
`SecurityConfigTest` (Default-Origin weiterhin erlaubt, mehrere Origins korrekt geparst).

### 2.5 Login/Register/Update binden die JPA-Entity direkt als Request-Body — teilweise behoben
**Login:** neues `LoginRequest`-DTO (`email`, `password`, nur `@NotBlank` — bewusst kein `@Email`,
damit Bestandskonten mit nicht-email-förmigem `username`-Altwert sich weiter einloggen können)
ersetzt `@RequestBody User`. Der zu weite `catch(Exception e)` ist komplett entfernt:
`authenticationManager.authenticate(...)` darf jetzt frei werfen, `GlobalExceptionHandler` fängt
`AuthenticationException` zentral mit fester generischer Meldung ab (409 wird nicht mehr für
z. B. einen DB-Ausfall als "falsches Passwort" ausgegeben) — behebt damit gleichzeitig einen Teil
von Punkt 3 ("drei verschiedene Fehler-Response-Formen": Login lieferte bisher reinen Text statt
`{"error": ...}`). Regressionstest `login_withWrongPassword_returnsJsonErrorNotPlainText`.
**Nicht behoben:** `UserController.updateUser` bindet weiterhin kein spezielleres DTO als
`UpdateUserRequest` schon ist (das ist bereits ein DTO, kein Problem mehr); `BookingController
.updateBooking` ist mit 1.1–1.3 komplett entfernt worden, der Punkt ist dort gegenstandslos.

### 2.6 Bild-Upload vertraut clientseitigen Metadaten — behoben
`StorageServiceImpl.uploadRoomImage` liest jetzt den tatsächlichen Dateiinhalt und prüft Magic
Bytes für PNG/JPEG/GIF/WebP (kein `javax.imageio.ImageIO`, weil dessen eingebaute Plugins kein
WebP dekodieren — das hätte gültige WebP-Uploads fälschlich abgelehnt). Der Dateiname des Clients
fließt nicht mehr in den S3-Key ein — die Dateiendung kommt jetzt ausschließlich aus dem
(inhaltlich verifizierten) Content-Type, das schließt die von der Review genannte
Pfad-Traversal-Möglichkeit im S3-Key. Regressionstests `StorageServiceImplTest`: als `image/png`
deklarierter HTML-Inhalt wird abgelehnt, eine echte PNG-Signatur kommt durch die Validierung.

### 2.7 Keine Rate-Limits/Lockouts — behoben
Neuer `RateLimitFilter` (In-Memory, Fixed-Window über `RateLimiter`) begrenzt `/api/login`
(10/5 Min), `/api/register` und `/api/forgot-password` (je 5/Stunde) pro Client-IP, mit 429 +
`{"error": ...}`. `/api/refresh` bewusst ausgenommen: erfordert bereits ein gültiges, nicht
erratbares Refresh-Token, Drosseln würde dort vor allem legitime, gemeinsame IPs (NAT/Firmennetz)
treffen. Über `app.rate-limit.enabled` abschaltbar (Default `true`; in `application-test.properties`
`false`, weil der `RateLimiter`-Bean als Singleton im geteilten Test-Kontext sonst unabhängige
Integrationstests gegenseitig blockieren würde). Live gegen die laufenden Container verifiziert:
11 schnelle Login-Versuche → der 11. liefert 429. Ausdrücklich **kein** verteilter/production-grade
Rate-Limiter (kein Redis o. ä.) — reicht für eine Single-Instance-Deployment, müsste bei mehreren
Backend-Instanzen ersetzt werden. Regressionstests `RateLimiterTest`, `RateLimitFilterTest`.

### 2.8 Deaktivierte Räume bleiben buchbar — behoben
`BookingServiceImpl.createBookingFor` prüft jetzt `room.isActive()` und wirft `ResourceNotFoundException`
(gleiches Verhalten wie `RoomServiceImpl` für nicht-Admins) — gilt für beide Buchungspfade
(`addBooking`, `addBookingForCustomer`), da ein deaktivierter Raum grundsätzlich nicht buchbar sein
sollte, unabhängig davon, wer die Buchung anstößt. Regressionstest
`addBooking_rejectsDeactivatedRoom`; live verifiziert (Testraum angelegt, deaktiviert, Buchung als
Mitglied → 404).

### 2.9 Kleinere Punkte
- **Behoben:** `spring.jpa.show-sql` defaultet jetzt auf `false` (per `SPRING_JPA_SHOW_SQL`
  weiterhin aktivierbar) — Produktions-Deployment (`docker-compose.yml`) überschreibt es nicht,
  loggt also ab sofort kein SQL mehr standardmäßig.
- **Nicht behoben:** Supabase-Projekt-Referenz/DB-Benutzername in `docker-compose.yml` committet —
  das sind keine Secrets (die Passwörter liegen korrekt in `.env`), nur Konfigurationswerte;
  Ändern würde eine Rotation der zugehörigen Supabase-Zugangsdaten voraussetzen, außerhalb des
  Scopes eines Code-Fixes.
- **Nicht behoben:** `roomImageService.list`s inkonsistente (nur "authenticated" statt
  rollenspezifisch) Berechtigungsprüfung — kleine Inkonsistenz ohne echten Schaden, bewusst nicht
  angefasst, um nicht ohne klaren Anlass an der Autorisierungslogik zu schrauben.
- **IDOR-Check bestand:** Jeder ID-basierte Endpunkt wurde nachverfolgt — Berechtigung wird korrekt aus dem geladenen Objekt abgeleitet (nicht aus der URL). Kein IDOR gefunden — das ist der stärkste Teil des Backends.

---

## 3. Backend — Konsistenz & totes Gewicht

- **Nicht behoben:** Flyway-Migrationen (MySQL-DDL gegen Postgres, kein `V1`, `mysql-connector-j`/
  `flyway-mysql` noch in `pom.xml`). Flyway ist produktiv komplett deaktiviert
  (`SPRING_FLYWAY_ENABLED=false`), diese Dateien haben also keinen Laufzeit-Effekt — sie zu bereinigen
  wäre reine Historienpflege ohne funktionalen Nutzen und war nicht Teil dieses Durchgangs.
- **Teilweise behoben — drei Fehler-Response-Formen:** Der Login-Endpunkt lieferte reinen Text statt
  JSON; das ist mit 2.5 behoben (`AuthenticationException` → `GlobalExceptionHandler` →
  `{"error": ...}`). Die grundsätzliche Mischung aus `{"error": ...}` und `{"feldname": "meldung"}`
  (Bean-Validation) bleibt bestehen — das sind zwei unterschiedliche, für ihren jeweiligen Zweck
  passende Formen (Feldfehler brauchen die Feld-Zuordnung fürs Frontend), keine reine Inkonsistenz.
- **Größtenteils entschärft — `IllegalStateException` → 409 zu weit gefasst:** Der konkrete Auslöser
  (1.7, `Collectors.toMap`-Crash) ist behoben, dieser Pfad wirft die Exception also nicht mehr. Das
  generelle Mapping (jede `IllegalStateException` wird als 409 "Konflikt" an den Client durchgereicht)
  bleibt bestehen — eine vollständige Lösung bräuchte eigene, spezifischere Exception-Typen für jeden
  Fall, das ist ein größerer Umbau der Fehlerarchitektur und war nicht Teil dieses Durchgangs.
- **Behoben:** `{userId}`-Pfadparameter ohne Wirkung entfernt aus `BookingController` (`/booking/getAll`,
  `/booking/delete/{id}`, `/booking/get/{id}` statt `.../​{userId}`) und `BookingService`/
  `BookingServiceImpl` (Berechtigung kam ohnehin schon aus der geladenen Buchung/Session, nie aus der
  URL). Frontend (`booking.api.ts`, `BookingTable.tsx`) entsprechend angepasst
  (`fetchBookingsForUser(userId)` → `fetchBookings()`, `deleteBooking(id, userId)` → `deleteBooking(id)`).
- **Toter Code entfernt**: `booking/model/Datum.java` (Wochentags-Enum, war nirgends referenziert),
  `JwtService.extractRoles` (kein Aufrufer) und `isExpired` (durch JJWTs eingebaute
  `exp`-Prüfung beim Parsen faktisch unerreichbar — ein abgelaufenes Token wirft schon beim
  vorangehenden `extractSubject(token)`-Aufruf, der in `JwtAuthFilter` sicher abgefangen wird),
  `UserService.addUser`/`UserServiceImpl.addUser` (kein Aufrufer, hätte das Passwort ungehasht
  gespeichert). **Nicht verändert:** die "sich überschneidenden" Repository-Methoden
  (`findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual` für alle Räume vs.
  `findByRoom_Id...` für einen Raum vs. `findOverlapping` mit Exclude-ID) — die drei haben
  unterschiedliche Aufrufer mit unterschiedlichem Cardinality-Bedarf; sie zusammenzuführen würde
  entweder Java-seitiges Nachfiltern über die volle Buchungsliste erzwingen (Performance-Regression,
  siehe Abschnitt 5) oder die Exclude-ID-Query unnötig verkomplizieren. Bewusst nicht angefasst.
- **Nicht behoben:** gemischte Sprache in Bezeichnern/Meldungen, `Controller`-Pakete groß geschrieben,
  gemischte `@Autowired`-Feld- vs. Konstruktor-Injection, leere `pom.xml`-Initializr-Stubs — reine
  Stilfragen ohne funktionalen Effekt, ein projektweiter Sweep dafür stand nicht im Verhältnis zum
  Nutzen und barg das Risiko, an Stellen, die gerade nicht im Fokus standen, unbeabsichtigt etwas zu
  verändern. Die französischen `SecurityConfig`-Kommentare mit den irreführenden/falschen Erklärungen
  wurden im Rahmen von 2.4 (dieselbe Datei ohnehin geändert) durch klare deutsche/englische Kommentare
  ersetzt.

---

## 4. Backend — Testabdeckung

Vorhanden: Tests für Booking-Service (inkl. der 3 neuen Lösch-Sperre-Tests), Discount-Code-Service,
Payment-Service, AuthorizationService, Refresh-/Reset-Token-Services, mehrere Integrationstests, plus
in diesem Durchgang neu: `RoomServiceImplTest` (5 Tests, Soft-Delete-Sichtbarkeit,
`bookedUntil`-Berechnung, `toMap`-Merge), `BookingPaymentCircularReferenceTest`,
`PaymentRepositoryTest` (`@DataJpaTest` für die JPQL-Intervallgrenze), `CustomerTypeTest` (3 Tests
für die 10%-Organisationsrabatt-Logik — **behoben**, siehe unten), `ExpiredTokenCleanupSchedulerTest`,
`SecurityConfigTest`, `StorageServiceImplTest`, `RateLimiterTest`, `RateLimitFilterTest`.

**Behoben:** `CustomerType.applyPricing` (Organisationsrabatt) hatte keine einzige Assertion —
jetzt mit drei Tests abgedeckt (10%-Rabatt, Vollpreis für `KUNDE`, Rundung auf zwei Nachkommastellen).
`updateBooking` ist als ungetesteter Risikobereich hinfällig, da die Methode komplett entfernt wurde
(siehe 1.1–1.3, bereits vorher erledigt).

**Weiterhin ungetestet, unverändert von vorher (nicht Teil dieses Durchgangs):**
- Das komplette `invoice`-Paket (Nummernvergabe, Berechtigung, PDF).
- `addBookingForCustomer` (Admin-Buchungspfad).
- Die Rabattcode-Sperre für Organisationen.
- Cross-User-Zugriff auf HTTP-Ebene (kein Integrationstest beweist, dass Nutzer B von Nutzer A's
  Buchung/Profil mit 403 abgewiesen wird).

Diese vier Lücken wurden bewusst nicht angegangen — sie sind reine Testabdeckungs-Lücken ohne
bekannten aktuellen Bug dahinter, und die Zeit dieses Durchgangs floss vorrangig in die
Korrektheits-/Sicherheits-Punkte der Abschnitte 1–3 und 6–7.

---

## 5. Backend — Performance

**Nicht behoben, unverändert.** Alle vier Punkte bestehen weiterhin:
- `GET /booking/getAll` als Admin lädt **alle** Buchungen ohne Paginierung; `@ManyToOne`/`@OneToOne`
  sind EAGER → ca. 3 zusätzliche Selects pro Buchung (N+1).
- Admin-Dashboard lädt **alle** Buchungen und **alle** Zahlungen komplett in den Speicher, nur um
  Top-5-Listen zu bilden — sollte `GROUP BY`-Queries mit `Pageable` sein.
- `getAllUsers` ohne Paginierung.
- `spring.jpa.open-in-view` nicht explizit gesetzt (Default `true`) — hält DB-Verbindungen länger offen als nötig.

Bewusst zurückgestellt: das sind strukturelle Änderungen (Pagination auf mehreren Endpunkten inkl.
Frontend-Anpassung, DTO-Projektionen statt Entity-Laden) mit Risiko für Frontend-Breaking-Changes,
bei aktueller Datenmenge aber ohne akuten Leidensdruck. Passt eher in einen eigenen Durchgang.

---

## 6. Frontend — Korrektheit

### 6.1 E-Mail-Adresse ändern zerschießt die eigene Session — behoben
`profile/page.tsx` ruft nach einer erfolgreichen `updateUser`-Änderung jetzt
`useSession().update({ email })` auf. `api/auth/[...nextauth]/route.ts`s `jwt`-Callback
erkennt `trigger === "update"` und erzwingt einen `refreshAccessToken(...)`-Aufruf, statt die
noch nicht abgelaufene alte Access-Token-TTL zu respektieren — das neue Token trägt die aktuelle
E-Mail als `sub`, weil `/api/refresh` sie über die `RefreshToken.user`-Relation (Foreign Key,
keine Namens-Lookup) aus der DB liest. Zusätzlich musste der `session`-Callback
`session.user.email = token.email` explizit setzen: NextAuth seedet `session.user.email`
intern aus dem *alten*, noch nicht durch `jwt()` aktualisierten Token, sonst hätte die Anzeige
weiter die alte Adresse gezeigt, obwohl Access-Token/JWT-Subject schon korrekt waren.

Live end-to-end verifiziert (curl gegen die laufenden Container, kompletter
NextAuth-CSRF/Login/Update-Zyklus): E-Mail geändert → alter Access-Token liefert 403 (Bug
reproduziert) → `update()`-Aufruf → Antwort enthält sowohl die neue `session.user.email` als
auch einen neuen Access-Token mit der neuen E-Mail als `sub` → neuer Token funktioniert (200).

### 6.2 Buchungsstatus ist in West-Zeitzonen um einen Tag versetzt — behoben
`getBookingStatus` in `components/BookingTable.tsx` vergleicht jetzt reine `"YYYY-MM-DD"`-Strings
(heutiges Datum in lokaler Zeit vs. `start`/`end`) statt über `Date`-Objekte. Der alte Code parste
ein reines Datum wie `"2026-08-08"` als UTC-Mitternacht und "schnappte" es dann per `setHours`
auf lokale Mitternacht — westlich von UTC verschob das Start/Ende um einen Tag zurück. `LocalDate`
kennt keine Zeitzone, ein reiner Stringvergleich bildet das korrekt ab und ist zeitzonenunabhängig.
Mit vier Fällen (laufend, zukünftig, abgelaufen, ein Tag) verifiziert; live gegen die laufenden
Container deployt, `/profile` (nutzt `BookingTable`) lädt fehlerfrei.

### 6.3 "Ist diese Buchung löschbar"-Logik existiert doppelt — MITTEL (Ursache für die Abweichung behoben, Duplikation bleibt)
`components/BookingTable.tsx` vs. Backend `BookingServiceImpl.deleteBooking`. Zwei unabhängige
Implementierungen derselben Regel bestehen weiterhin — das ist nicht behoben und bräuchte eine
gemeinsame Quelle (z. B. den `canDelete`-Status vom Backend mitliefern lassen). Durch die
6.2-Korrektur landen beide Implementierungen aber wieder auf demselben Kalendertag-Modell und
weichen dadurch praktisch nicht mehr ab wie zuvor beschrieben.

### 6.4 Zugriffstoken ist für clientseitiges JavaScript sichtbar — HOCH (bewusster Trade-off, aber undokumentiert)
`api/auth/[...nextauth]/route.ts:133`. Das Spring-JWT wird über `session.accessToken` an den
Browser durchgereicht (Refresh-Token bleibt korrekt serverseitig). Jede zukünftige XSS-Lücke würde
damit zur vollen Kontoübernahme für die Token-Laufzeit. Architektonisch vertretbar für ein
Browser→Backend-Direktmodell, sollte aber als bewusste Entscheidung dokumentiert sein.

### 6.5 Registrierungs-Fehlermeldung wird sofort wieder gelöscht — behoben
`login/page.tsx`: Schlägt der Auto-Login nach erfolgreicher Registrierung fehl, rief der Code
`switchMode("login")` auf, was den gerade gesetzten Fehlertext beim Moduswechsel wieder leerte.
Jetzt wird direkt `setMode("login")` (ohne den Fehler-Reset von `switchMode`) gefolgt von
`setError(...)` aufgerufen — der Nutzer sieht die Meldung ("Konto erstellt, automatische Anmeldung
fehlgeschlagen, bitte manuell anmelden") jetzt tatsächlich, statt dass sie im selben Render-Zyklus
wieder verschwindet.

### 6.6 Weitere Punkte
- **Behoben — Doppel-Submits:** `RaumAdd.tsx` und `BookingAdd.tsx` (Admin- und Mitglieder-Pfad)
  haben jetzt einen `submitting`-State, der den Submit-Button während des Requests deaktiviert,
  nach dem Muster von Login/Profil/Reset-Passwort. `DiscountCodeAdmin.tsx` ebenso.
- **Behoben — Rechnungs-Download:** `BookingTable.tsx` hängt den Blob-Link jetzt vor dem `.click()`
  ins DOM, entfernt ihn danach wieder, und verzögert `URL.revokeObjectURL` (statt es synchron direkt
  nach `.click()` aufzurufen) — vermeidet die Race-Condition, die in Firefox den Download abbrechen
  konnte.
- **Behoben — `effectivePricePerDay` als Pflichtfeld:** In `room.api.ts` jetzt als optionales Feld
  deklariert (`effectivePricePerDay?: number`), passend dazu, dass der rohe `Booking.room` vom
  Backend es nicht mitliefert. Alle vier Lesestellen (`RoomTable.tsx`, `rooms/[id]/page.tsx`) nutzen
  jetzt `room.effectivePricePerDay ?? room.pricePerDay` als Fallback statt eines potenziellen `NaN`.
- **Behoben — Datumsformatierung:** neue Hilfsfunktion `lib/formatDate.ts`
  (`formatLocalDate(isoDate)`, zeitzonensicher, siehe 6.2) wird jetzt überall verwendet, wo zuvor
  rohe ISO-Strings angezeigt wurden: `BookingTable.tsx`, `RoomTable.tsx`, `rooms/[id]/page.tsx`,
  `DiscountCodeAdmin.tsx`, `admin/page.tsx` (Zeitraum-Spalte). Statt `2026-08-08 – 2026-08-10`
  erscheint jetzt ein lokalisiertes deutsches Datumsformat.
- **Behoben — Benutzer löschen ohne Bestätigung/Feedback:** `admin/users/page.tsx` nutzt jetzt
  `ConfirmDialog` (statt der Löschung ganz ohne Rückfrage) und einen `error`-State mit `Alert`-Anzeige
  bei Fehlschlag, nach demselben Muster wie `BookingTable.tsx`/`DiscountCodeAdmin.tsx`.
- **Teilweise behoben — generische Fehlermeldungen bei Mitglieder-Buchungen:** `alert()`/
  `window.confirm()` wurden projektweit durch die `Alert`-Komponente und `ConfirmDialog` ersetzt
  (siehe Abschnitt 7), wodurch Fehler jetzt konsistent sichtbar sind statt über Browser-Popups. Die
  ursprüngliche Beobachtung — dass nur der Admin-Buchungspfad `extractErrorMessage` nutzt und
  Mitglieder bei z. B. "Raum bereits belegt" nur eine generische Meldung statt des Backend-Grundes
  sehen — ist **nicht** behoben; das bräuchte eine gezielte Änderung an `BookingAdd.tsx`s
  Mitglieder-Zweig und war nicht Teil dieses Durchgangs.
- **Nicht behoben:** fehlende serverseitige Routen-Absicherung (`middleware.ts`) — weiterhin rein
  clientseitige Redirects. Wie schon zuvor festgehalten keine Sicherheitslücke (das Backend setzt
  alle Berechtigungen unabhängig durch), nur unnötige Anfragen/Flackern für nicht berechtigte Nutzer;
  bewusst zurückgestellt, da reine UX-Politur ohne Sicherheitsrelevanz.

---

## 7. Frontend — Barrierefreiheit & Politur

- **Behoben — `ConfirmDialog` Barrierefreiheit:** jetzt mit `role="dialog"`, `aria-modal="true"`,
  `aria-labelledby`/`aria-describedby` (verknüpft via `React.useId()`), Escape-Handler zum Schließen,
  einem minimalen Zwei-Elemente-Fokus-Trap (Tab zirkuliert zwischen Abbrechen-/Bestätigen-Button) und
  Autofokus beim Öffnen. Dafür musste `Button` (`components/ui.tsx`) erst `ref` als expliziten
  `React.Ref<HTMLButtonElement>`-Prop entgegennehmen und weiterreichen — React 19 reicht `ref` bei
  einer normalen Funktionskomponente nicht automatisch durch, auch nicht über `{...props}`.
- **Behoben — Galerie-Miniaturen ohne Label:** `RoomGallery.tsx`s Bild-Buttons haben jetzt
  `aria-label` (Raumname/Bildnummer) und `aria-current` für das aktuell angezeigte Bild.
- **Weitgehend vereinheitlicht:** `alert()`/`window.confirm()` wurden durchgängig durch die
  `Alert`-Komponente bzw. `ConfirmDialog` ersetzt, in `BookingTable.tsx` (Löschbestätigung +
  Aktions-Fehler), `RoomTable.tsx` (Status-Wechsel-Fehler), `DiscountCodeAdmin.tsx`
  (Löschbestätigung + Fehler), `admin/page.tsx` (Zahlungsbestätigung-Fehler), `admin/users/page.tsx`
  (Löschbestätigung + Fehler, siehe 6.6). Damit sind faktisch nur noch die zwei einheitlichen Idiome
  (`Alert`, `ConfirmDialog`) im Einsatz statt vier.
- **Nicht verändert:** statisches Zimmer-Bild-Mapping (`lib/roomImages.ts`) — dient weiterhin als
  Fallback für Räume ohne `imageUrl` (u. a. die vorhandenen Seed-Daten) und wird in `RoomTable.tsx`/
  `rooms/[id]/page.tsx` bewusst als `room.imageUrl ?? roomImages[room.name] ?? defaultRoomImage`
  verwendet — solange nicht jeder Raum ein echtes Bild hat, ist das kein Altlast-Entfernungskandidat.

---

## 8. Priorisierte Empfehlung (falls als Nächstes angegangen werden soll)

1. ~~**`updateBooking` reparieren oder entfernen** (1.1–1.3)~~ — erledigt, Methode entfernt.
2. ~~**Transaktionen einführen** (`@Transactional` auf den schreibenden Service-Methoden)~~ — erledigt, behebt 1.4, mindert 1.5/1.6.
3. ~~**Eindeutigkeit von `email`** auf DB-Ebene erzwingen (1.9)~~ — erledigt, siehe Deploy-Hinweis oben (Duplikate vorher prüfen).
4. ~~**Passwort-Reset-Token nicht loggen** (2.1)~~ — teilweise erledigt (Default jetzt sicher), volle Lösung braucht echten Mailversand, siehe Hinweis oben.
5. ~~**Session nach E-Mail-Änderung aktualisieren** (6.1)~~ — erledigt, live end-to-end verifiziert.
6. ~~**Zeitzonen-sichere Datumsvergleiche im Frontend** (6.2)~~ — erledigt, reiner Kalendertag-Stringvergleich statt `new Date(isoString)`.

**Weiterer Durchgang (dieser Bearbeitungsschritt) — alle verbliebenen Punkte einzeln abgearbeitet:**
im Detail siehe die "behoben"/"teilweise behoben"/"nicht behoben"-Markierungen direkt in den
Abschnitten 1–7 oben. Kurzfassung:
- **Behoben:** 1.7, 1.8, 1.10–1.12 (teilw.), 2.2, 2.4, 2.5 (teilw.), 2.6, 2.7, 2.8, 2.9 (teilw.),
  userId-Pfadparameter (Abschnitt 3), toter Code (Abschnitt 3), 4 (CustomerType-Tests), 6.5, 6.6
  (teilw.), 7 (ConfirmDialog-a11y, Galerie-Label, Alert/ConfirmDialog-Vereinheitlichung).
- **Bewusst nicht behoben** (mit Begründung im jeweiligen Abschnitt): 1.5/1.6 (Race
  Condition/Rechnungsnummerierung — bräuchte DB-Constraint/Sequence), 2.1 zweite Hälfte (echter
  Mailversand fehlt weiterhin), 2.3 Rotation/Klartext-Speicherung, Flyway-MySQL-Bereinigung,
  gemischte Sprache/Casing/Injection-Stil, überlappende Repository-Methoden, Abschnitt 5
  (Performance/Pagination), `roomImages.ts`, `middleware.ts`, restliche Testabdeckungslücken
  (Abschnitt 4), generische Fehlermeldungen bei Mitglieder-Buchungen (6.6).

Alle Fixes wurden getestet (`mvn test`: 74/74 grün, `npm run build`/`tsc --noEmit`: fehlerfrei) und,
wo sinnvoll, live gegen die laufenden Docker-Container verifiziert. Dieser Durchgang wurde
absichtlich **nicht committet/gepusht** — nur gelöst und getestet, wie angefordert.

---

## 9. Neuer vollständiger Repository-Durchgang (2026-08-09) — noch offene Funde

Zusätzlich zum obigen Durchgang wurde das **gesamte Repository noch einmal Datei für Datei**
gelesen (jede Klasse in `bookingssystem/src/main`, alle Tests, `application.properties`, `pom.xml`,
`docker-compose.yml`, beide Dockerfiles, alle Flyway-Migrationen, `scripts/seed_rooms.sql`, sowie
jede Datei unter `react_frontend/src`), gezielt auf der Suche nach Problemen, die noch **nicht** in
diesem Dokument stehen. Die folgenden Punkte sind neu und noch **nicht behoben** — reine
Bestandsaufnahme für einen künftigen Durchgang, in absteigender Priorität. Mehrere Punkte wurden
live gegen die laufenden Container verifiziert (curl/MockMvc), das ist jeweils vermerkt.

### 9.1 Passwort-Mindestlänge wird beim Profil-Self-Service umgangen — HOCH, live verifiziert
`user/model/UpdateUserRequest.java`: `record UpdateUserRequest(@Email String email, String password,
String currentPassword)` — anders als `RegisterRequest.password` und
`ResetPasswordRequest.newPassword` (beide `@NotBlank @Size(min = 6)`) hat das `password`-Feld hier
**keine einzige Validierungs-Annotation**. `UserServiceImpl.updateUser` verschlüsselt und speichert
es ungeprüft. Live verifiziert: ein frisch registrierter Nutzer konnte sein Passwort per
`PUT /user/update/{id}` (derselbe Endpunkt, den `profile/page.tsx`s "Passwort ändern"-Formular
aufruft) auf das einzelne Zeichen `"a"` setzen (`200 OK`), und der anschließende Login mit Passwort
`"a"` war erfolgreich. Damit ist die im Rest der Anwendung (Registrierung, Passwort-Reset, `User`-
Entity selbst) durchgesetzte 6-Zeichen-Mindestlänge über den Profil-Pfad vollständig umgehbar.
**Fix-Ansatz:** `@Size(min = 6)` auf `UpdateUserRequest.password` ergänzen (bleibt mit dem
Partial-Update-Verhalten kompatibel, da `@Size` einen `null`-Wert weiterhin als gültig durchlässt)
und `@Valid` (bereits auf dem Controller vorhanden) greift dann automatisch.

### 9.2 Nutzer mit bestehenden Buchungen löschen scheitert mit irreführender Fehlermeldung — HOCH, live verifiziert
`UserServiceImpl.deleteUser` ruft `userRepository.delete(user)` ohne vorherige Prüfung auf
verknüpfte Buchungen auf. `Booking.user` ist eine `@ManyToOne(nullable = false)`-Beziehung ohne
Cascade — die Datenbank lehnt das `DELETE` mit einer Fremdschlüsselverletzung ab. Diese landet im
generischen `DataIntegrityViolationException`-Handler (`GlobalExceptionHandler`, ursprünglich für
E-Mail-Kollisionen gedacht) und liefert **409 `{"error": "Diese Angabe wird bereits verwendet."}`**
— eine für diesen Fall komplett falsche/verwirrende Meldung. Live mit einem eigens angelegten
Test verifiziert (MockMvc: Nutzer registriert, Buchung angelegt, Löschversuch über
`DELETE /user/delete/{id}` → exakt dieses Ergebnis, SQL-Fehler `Referential integrity constraint
violation` im Log). Über die reale Admin-Oberfläche (`admin/users/page.tsx`, nutzt
`extractErrorMessage`) bekäme ein Admin diese irreführende Meldung 1:1 angezeigt. **Fix-Ansatz:** in
`deleteUser` vorher prüfen, ob `bookingRepository.findByUser_Id(id)` etwas liefert, und falls ja
eine eigene, klare Fehlermeldung werfen (z. B. "Nutzer hat noch Buchungen und kann nicht gelöscht
werden") — oder bewusst entscheiden, ob ein Löschen mit Buchungshistorie überhaupt erlaubt sein
soll (ggf. stattdessen deaktivieren statt löschen, analog zu Räumen).

### 9.3 Rabattcode-Einlösung ist case-sensitiv, aber nur die Erstellung normalisiert Großschreibung — MITTEL
`DiscountCodeServiceImpl.applyDiscount`/`DiscountCodeRepository.findByCode` vergleichen den Code
exakt (case-sensitiv). `DiscountCodeAdmin.tsx`s Erstellungsformular zwingt die Eingabe automatisch
auf Großbuchstaben (`setCode(e.target.value.toUpperCase())`), aber `BookingAdd.tsx`s
"Rabattcode (optional)"-Feld beim Buchen tut das nicht. Ein Kunde, der einen ihm mündlich/schriftlich
mitgeteilten Code wie "summer10" statt "SUMMER10" eingibt, bekommt fälschlich "Unknown discount
code", obwohl der Code existiert. **Fix-Ansatz:** entweder Eingabe in `BookingAdd.tsx` ebenfalls
normalisieren oder den Vergleich im Backend case-insensitiv machen (z. B. Code beim Speichern immer
in Großbuchstaben normalisieren).

### 9.4 Keine Prüfung gegen Buchungen in der Vergangenheit — MITTEL
Weder `BookingServiceImpl.addBooking`/`addBookingForCustomer`/`createBookingFor` noch die
`<input type="date">`-Felder in `BookingAdd.tsx` (kein `min`-Attribut) verhindern ein Startdatum in
der Vergangenheit. Ein Nutzer (oder jeder direkte API-Aufruf) kann aktuell eine Buchung für ein
bereits vergangenes Datum anlegen. Das crasht nichts, verzerrt aber Statistiken/Admin-Dashboard und
widerspricht dem impliziten Geschäftsmodell ("Raum für einen zukünftigen Zeitraum buchen").
**Fix-Ansatz:** in `createBookingFor` prüfen `startTime.isBefore(LocalDate.now())` → 400, plus
`min={heutigesDatum}` auf den Datums-Inputs im Frontend.

### 9.5 `RoomImageServiceImpl.list()` hat keine Berechtigungs- oder Aktiv-Prüfung — NIEDRIG/MITTEL
Anders als `RoomServiceImpl.findRoomById` (das einen deaktivierten Raum für Nicht-Admins mit 404
versteckt) prüft `list(UUID roomId)` weder `authorizationService`-Berechtigungen noch
`room.isActive()`. Jeder eingeloggte Nutzer kann über `GET /room/{id}/images` die Bilder eines
beliebigen — auch eines deaktivierten — Raums abrufen, dessen Detailseite ihm sonst 404 liefern
würde. Die Bilder selbst sind nicht sensibel, aber die Inkonsistenz zur sonstigen
Sichtbarkeitsregel ist ein loses Ende. **Fix-Ansatz:** dieselbe Aktiv-Prüfung wie in
`findRoomById` ergänzen.

### 9.6 Bild-Upload: deklarierter Content-Type wird nicht gegen den erkannten Bildtyp abgeglichen — NIEDRIG
`StorageServiceImpl.looksLikeImage` (siehe 2.6) verifiziert nur, dass die Bytes *irgendein*
bekanntes Bildformat sind — `extensionFor(contentType)` und der an S3 gesendete `Content-Type`
kommen aber weiterhin unverändert vom Client, ohne Abgleich mit dem tatsächlich per Magic Bytes
erkannten Format. Ein Client, der `Content-Type: image/gif` deklariert, aber echte PNG-Bytes
schickt, besteht die Prüfung (es *ist* ein Bild), landet aber mit falscher Dateiendung/falschem
Content-Type-Header im Bucket. Kein Sicherheitsproblem mehr (das war 2.6), aber ein
Metadaten-Inkonsistenz-Rest. **Fix-Ansatz:** `looksLikeImage` das erkannte Format zurückgeben
lassen und `extensionFor`/den S3-`Content-Type` daraus statt aus dem Client-Header ableiten.

### 9.7 Weitere kleinere Funde (Performance, Deployment, Konsistenz)
- **`StorageServiceImpl.uploadRoomImage` ist komplett `synchronized`**, nicht nur die
  Lazy-Initialisierung des S3-Clients — serialisiert dadurch *alle* Bild-Uploads app-weit
  (inklusive der S3-Netzwerk-Roundtrip-Zeit selbst), nicht nur den einmaligen Client-Aufbau.
  Unnötig langsam bei gleichzeitiger Nutzung durch mehrere Admins.
- **`AuthorizationService.requireAuthenticatedUser()`/`currentCustomerType()` laden den `User`
  bei jedem Aufruf frisch per `findByEmail` aus der DB** — zusätzliche Query-Last pro Request,
  die sich mit einem Request-scoped Cache vermeiden ließe (ergänzt die bereits in Abschnitt 5
  dokumentierten Performance-Punkte).
- **JWT trägt einen `roles`-Claim, der nirgends gelesen wird**: `JwtService.generateAccessToken`
  packt die Rollen in jedes Token, aber `JwtAuthFilter` lädt die Berechtigungen bei jedem Request
  frisch über `UserDetailsService` aus der DB, nie aus dem Token selbst. Der Claim ist totes
  Gewicht (vergrößert jedes Token unnötig) — praktisch aber sogar von Vorteil: Rollenänderungen
  wirken sofort, ohne dass ein altes Token die alte Rolle weiterträgt.
- **`scripts/seed_rooms.sql` ist nicht mehr mit dem aktuellen `Room`-Schema synchron**: die
  INSERT-Liste setzt weder `active` noch `city`/`description`/`size_square_meters`/`image_url`.
  Da `active` ein primitives `boolean` ohne `@Column`-Override ist, könnte ein frischer Lauf
  dieses (nur manuell auszuführenden, einmaligen) Skripts je nach Hibernate-Spaltendefinition
  entweder an einer NOT-NULL-Verletzung scheitern oder Zeilen mit `active = NULL` erzeugen, die
  das Backend beim Lesen zum Absturz bringen könnten (nicht live verifiziert — reines
  Setup-Tooling, nicht Teil des automatisierten Deployments).
- **Backend-`Dockerfile` läuft als root** (kein `USER`-Directive), im Unterschied zum
  Frontend-`Dockerfile`, das korrekt einen eigenen `nextjs`-User anlegt und verwendet — übliche
  Container-Hardening-Lücke.
- **`RoomController`/`DiscountCodeController` nehmen die rohe JPA-Entity (`Room`, `DiscountCode`)
  direkt als `@RequestBody` entgegen statt eines DTOs** — dadurch könnte ein Admin-Request an
  `/room/save` oder `/discount-code` Felder wie `active`/`roomStatus` direkt mitschicken, auch
  über die dafür vorgesehenen dedizierten Endpunkte (`/activate`, `/deactivate`) hinaus. Nur
  Admin-erreichbar, daher geringes Risiko, aber ein Architektur-Bruch gegenüber den sonst
  DTO-basierten Endpunkten (z. B. `BookingDTO`, `AdminBookingDTO`, `UpdateUserRequest`).
- Kleinere Inhalts-Inkonsistenz: `datenschutz/page.tsx`, Abschnitt 3, spricht noch von
  "Benutzername", obwohl das Feld im gesamten Code zu "E-Mail-Adresse" umbenannt wurde.

### 9.8 Ausdrücklich geprüft und für unauffällig befunden
Damit diese Liste nicht wie eine unvollständige Suche wirkt, zwei Dinge, die während des
Durchgangs gezielt untersucht und **verworfen** wurden, weil sie sich als unbegründet erwiesen:
- *Verdacht:* Die öffentliche Raumsuche/-browsing könnte durch `SecurityConfig`s
  `anyRequest().authenticated()` für nicht eingeloggte Besucher kaputt sein (`GET /room/Get`
  liefert live tatsächlich 403 ohne Token). Live-Check ergab aber: `/rooms` und die Raumdetailseite
  sind im Frontend ohnehin bewusst hinter einem Login-Redirect versteckt
  (`useSession().status === "unauthenticated"` → Redirect zu `/login`), die Startseite zeigt nur
  statische Bilder. Öffentliches Raum-Browsing ohne Konto ist schlicht nicht Teil des
  Produktdesigns — kein Bug.
- *Verdacht:* `confirmPayment`s `IllegalStateException` bei bereits bezahlten Zahlungen könnte ein
  Beispiel für die in Abschnitt 3 kritisierte "zu weite" `IllegalStateException`→409-Zuordnung
  sein. Bei genauerer Betrachtung ist das hier tatsächlich ein sauberer, spezifischer Anwendungsfall
  (doppelte Zahlungsbestätigung verhindern) und kein Beispiel für das allgemeine Problem.
