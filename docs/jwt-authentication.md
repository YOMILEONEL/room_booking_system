# JWT-Authentifizierung

Das Backend ist zustandslos (kein Server-Session-Speicher): jede Anfrage außer den in
`SecurityConfig` explizit freigegebenen Pfaden (siehe unten) muss einen gültigen
`Authorization: Bearer <accessToken>`-Header mitbringen. Es gibt **drei** unterschiedliche
Token-Arten im System, die leicht verwechselt werden:

| Token | Format | Gespeichert | Prüfbar/widerrufbar? | TTL (Default) |
|---|---|---|---|---|
| **Access-Token** | signiertes JWT (HS256, `io.jsonwebtoken`/JJWT) | nirgends serverseitig | nein - gültig bis zum Ablauf, keine Blacklist | 60 Minuten |
| **Refresh-Token** | opaker Zufallsstring (zwei aneinandergehängte UUIDs) | `refresh_tokens`-Tabelle | ja - `revoked`-Flag in der DB | 30 Tage |
| **Passwort-Reset-Token** | opaker Zufallsstring | `password_reset_tokens`-Tabelle | ja - `used`-Flag, einmal verwendbar | 30 Minuten |

## Beteiligte Dateien (Backend)

| Datei | Rolle |
|---|---|
| `security/JwtService.java` | Access-Token erzeugen (`generateAccessToken`) und lesen (`extractSubject`); signiert/verifiziert mit `security.jwt.secret` |
| `security/JwtAuthFilter.java` | Liest den `Authorization`-Header bei jeder Anfrage, setzt bei gültigem Token die Spring-Security-Authentication |
| `security/RefreshToken.java` / `RefreshTokenRepository.java` / `RefreshTokenServiceImpl.java` | Ausstellen, Validieren, Widerrufen der Refresh-Tokens |
| `security/PasswordResetToken.java` / `PasswordResetTokenServiceImpl.java` | Passwort-Reset-Tokens (separat vom Login-Fluss) |
| `security/RateLimitFilter.java` / `RateLimiter.java` | Drosselt `/api/login`, `/api/register`, `/api/forgot-password` pro Client-IP |
| `security/ExpiredTokenCleanupScheduler.java` | Täglicher Cron-Job (03:00), löscht abgelaufene Refresh-/Reset-Tokens |
| `security/AuthorizationService.java` | `requireAuthenticatedUser()`/`requireAdmin()`/`requireOwnerOrAdmin(...)` - liest die Rolle **aus der aktuellen Spring-Security-Authentication**, nie aus dem JWT direkt |
| `user/configuration/SecurityConfig.java` | Filterkette: welche Pfade `permitAll()` sind, JWT-/Rate-Limit-Filter-Reihenfolge |
| `user/Controller/RegistrationLoginController.java` | `/api/register`, `/api/login`, `/api/refresh`, `/api/logout`, `/api/forgot-password`, `/api/reset-password` |

Frontend-seitig: `react_frontend/src/app/lib/auth.ts` (NextAuth `authOptions` - Login-Aufruf,
Token-Refresh-Logik, JWT/Session-Callbacks) und `react_frontend/src/app/api/apiClient.ts`
(hängt `session.accessToken` als Bearer-Header an jeden direkten Backend-Aufruf des Browsers).

## Access-Token: Inhalt und Prüfung

`JwtService.generateAccessToken` erzeugt ein JWT mit:

```
iss = security.jwt.issuer     (Default: "bookingssystem")
sub = E-Mail-Adresse der Person
iat = Ausstellungszeitpunkt
exp = iat + security.jwt.accessTokenTtlMinutes (Default: 60 Minuten)
roles = [ "ROLE_MEMBER" ] oder [ "ROLE_ADMIN" ]   (Claim wird geschrieben, aber nie gelesen - siehe unten)
```

Signiert mit HMAC-SHA256 (`Keys.hmacShaKeyFor(secret)`), `security.jwt.secret` muss daher
ausreichend lang/zufällig sein (JJWT verlangt für HS256 mindestens 256 Bit ≈ 32 Byte-Secret,
sonst wirft `generateAccessToken` beim Start/ersten Aufruf einen Fehler).

**`JwtAuthFilter` prüft bei jeder Anfrage** (läuft vor `UsernamePasswordAuthenticationFilter`,
siehe `SecurityConfig.securityFilterChain`):

1. Kein `Authorization`-Header oder kein `Bearer `-Präfix → Anfrage läuft unauthentifiziert
   weiter (kein Fehler an dieser Stelle - erst `.anyRequest().authenticated()` weiter unten in
   der Filterkette entscheidet, ob der Pfad überhaupt einen Token braucht).
2. Token vorhanden → `jwtService.extractSubject(token)` versucht, das JWT zu verifizieren und zu
   parsen. **Jeder Fehler hier** (falsche Signatur, abgelaufen, falscher Issuer, kaputtes Format)
   wird stillschweigend aufgefangen - die Anfrage läuft ebenfalls unauthentifiziert weiter, ohne
   eigene Fehlermeldung an dieser Stelle.
3. Subject (E-Mail) gefunden → Person wird per `UserDetailsService.loadUserByUsername(email)`
   aus der DB geladen (frischer DB-Zugriff bei **jeder** Anfrage - kein Caching). Existiert die
   Person nicht mehr (z. B. gelöscht), läuft die Anfrage unauthentifiziert weiter.
4. Erst wenn alles davon passt, wird `SecurityContextHolder` gesetzt und die Anfrage gilt als
   authentifiziert mit den **aktuell aus der DB geladenen** Rollen/Berechtigungen.

Praktische Folge: Ein fehlendes, kaputtes oder abgelaufenes Token führt nie zu einer
spezifischen Fehlermeldung aus `JwtAuthFilter` selbst - die Anfrage kommt einfach
unauthentifiziert bei `.anyRequest().authenticated()` an, was Spring Security mit einem
generischen `401` beantwortet (kein `{"error": "..."}` im selben Format wie die übrigen
Endpunkte, da `GlobalExceptionHandler` hier nicht greift - es wird ja keine Exception geworfen).

### Warum der `roles`-Claim im JWT nie gelesen wird

Das ist **kein Bug, sondern bewusst**: `AuthorizationService`/`JwtAuthFilter` holen die
Berechtigungen bei jeder Anfrage frisch aus der DB (`UserDetailsService`), statt dem
`roles`-Claim im Token zu vertrauen. Vorteil: eine Rollenänderung (z. B. Admin degradiert einen
Account) wirkt **sofort** beim nächsten Request, statt erst nach Ablauf des alten Access-Tokens
bis zu 60 Minuten später. Nachteil: der Claim ist totes Gewicht, vergrößert jedes Token
unnötig (siehe `docs/code-review.md`, Abschnitt "JWT trägt einen `roles`-Claim...").

## Login-/Refresh-Ablauf

```
POST /api/login {email, password}
  → AuthenticationManager prüft Passwort (BCrypt) gegen die DB
  → bei Erfolg: JwtService erzeugt Access-Token, RefreshTokenService legt einen neuen,
    opaken Refresh-Token in der DB an
  → Antwort: { id, email, displayName, role, customerType, accessToken, refreshToken }

... 60 Minuten später ist der Access-Token abgelaufen ...

POST /api/refresh {refreshToken}
  → RefreshTokenService.validate(): existiert der Token? nicht widerrufen? nicht abgelaufen?
    (jeweils InvalidTokenException → 401, falls nein)
  → bei Erfolg: JwtService erzeugt ein NEUES Access-Token für denselben Nutzer
  → Antwort: { accessToken }   (kein neues Refresh-Token - siehe "Keine Rotation" unten)

POST /api/logout {refreshToken}
  → RefreshTokenService.revoke(): setzt refreshToken.revoked = true
  → Access-Token selbst bleibt bis zu seinem natürlichen Ablauf gültig (siehe "Kein
    Access-Token-Widerruf" unten) - Logout beendet nur die Möglichkeit, weitere Access-Token
    über diesen Refresh-Token nachzuziehen.
```

### Frontend-Seite (NextAuth, `lib/auth.ts`)

NextAuth hält Access-/Refresh-Token serverseitig im verschlüsselten NextAuth-JWT-Cookie
(separat vom Spring-Access-Token!) und reicht das Spring-Access-Token zusätzlich über
`session.accessToken` an den Browser durch (siehe "Bekannte Grenzen" unten). Der `jwt()`-Callback
prüft bei jedem Seitenaufruf `accessTokenExpires` (aus dem JWT-Payload dekodiert, **ohne**
Signaturprüfung - die übernimmt ausschließlich das Backend) mit 30 Sekunden Puffer und ruft bei
Bedarf proaktiv `/api/refresh` auf, **bevor** das Access-Token abläuft - ein Nutzer merkt vom
Refresh im Normalfall nichts. Schlägt der Refresh fehl (z. B. Refresh-Token wurde clientseitig
verloren oder das Backend meldet 401), wird `token.error = "RefreshAccessTokenError"` gesetzt und
**nicht** bei jeder weiteren Anfrage erneut versucht (würde das Backend sonst mit
Dauerversuchen fluten) - die Person muss sich neu einloggen.

Ein erzwungener Refresh passiert außerdem bei `useSession().update(...)` (z. B. nach einer
Profiländerung: neue E-Mail/Anzeigename), damit die Session nicht bis zu 60 Minuten den alten
Stand zeigt, obwohl die Änderung im Backend schon gespeichert ist.

## Rate-Limiting

`RateLimitFilter` drosselt pro Client-IP (nicht pro Account - verhindert auch Enumeration über
viele verschiedene E-Mail-Adressen von derselben IP):

| Endpunkt | Limit |
|---|---|
| `POST /api/login` | 10 Anfragen / 5 Minuten |
| `POST /api/register` | 5 Anfragen / Stunde |
| `POST /api/forgot-password` | 5 Anfragen / Stunde |

`/api/refresh` ist bewusst **nicht** gedrosselt - ein gültiger Refresh-Token ist bereits ein
Besitznachweis, Drosseln würde dort nur legitimen, IP-geteilten Traffic (NAT, Firmennetz)
treffen. Deaktivierbar über `APP_RATE_LIMIT_ENABLED` (Default `true`; im `test`-Profil
standardmäßig `false`, damit sich unabhängige Integrationstests nicht gegenseitig aussperren).

## Aufräumen abgelaufener Tokens

`ExpiredTokenCleanupScheduler` läuft täglich um 03:00 Uhr (`@Scheduled(cron = "0 0 3 * * *")`)
und löscht abgelaufene Refresh- und Passwort-Reset-Tokens aus der DB. Ein Fehler dabei (z. B. DB
kurzzeitig nicht erreichbar) wird geloggt, aber nicht weitergeworfen - der nächste Lauf am
folgenden Tag versucht es erneut.

## Konfiguration

```
# bookingssystem/.env / docker-compose.yml
SECURITY_JWT_SECRET=…                        # Pflicht, ≥ 32 zufällige Byte/Zeichen (HS256)
SECURITY_JWT_ISSUER=bookingssystem            # optional
SECURITY_JWT_ACCESSTOKENTTLMINUTES=60         # optional
SECURITY_JWT_REFRESHTOKENTTLDAYS=30           # optional
SECURITY_JWT_PASSWORDRESETTOKENTTLMINUTES=30  # optional
APP_RATE_LIMIT_ENABLED=true                   # optional
```

## Öffentliche Endpunkte (kein Token nötig)

`SecurityConfig` erlaubt exakt: `/api/register`, `/api/login`, `/api/refresh`, `/api/logout`,
`/api/forgot-password`, `/api/reset-password` (plus, unabhängig von JWT, die Doku-/Monitoring-
Pfade `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health`, `/actuator/info`,
`/actuator/prometheus` - siehe `docs/openapi.md`/`docs/monitoring.md`). Jeder andere Pfad
verlangt `.anyRequest().authenticated()`.

## Bekannte Grenzen (siehe auch `docs/code-review.md`)

- **Kein Access-Token-Widerruf**: Access-Token sind reine JWTs ohne serverseitige
  Gültigkeitsliste - einmal ausgestellt, bleiben sie bis zum natürlichen Ablauf (60 Minuten)
  gültig, selbst wenn die Person sich zwischenzeitlich ausloggt oder das Passwort ändert (Logout
  und Passwort-Reset widerrufen nur die **Refresh**-Tokens, siehe `revokeAllForUser` in
  `resetPassword`). Kompromiss zwischen Einfachheit (kein Redis/Blacklist-Speicher nötig) und
  sofortiger Widerrufbarkeit - das kurze TTL begrenzt das Zeitfenster.
- **Keine Refresh-Token-Rotation**: `/api/refresh` gibt bei jedem Aufruf ein neues Access-Token
  zurück, aber denselben Refresh-Token - ein einmal gestohlener Refresh-Token bleibt bis zu 30
  Tage lang nutzbar, statt bei Gebrauch automatisch gegen einen neuen ausgetauscht (und der alte
  ungültig) zu werden. Größerer Umbau (Frontend müsste ein neues Refresh-Token pro Refresh-Aufruf
  entgegennehmen und speichern können), bisher nicht umgesetzt.
- **Access-Token für Browser-JavaScript sichtbar**: `lib/auth.ts`s `session()`-Callback reicht
  das Spring-Access-Token über `session.accessToken` an den Browser durch (der Refresh-Token
  bleibt serverseitig im NextAuth-Cookie). Jede zukünftige XSS-Lücke im Frontend würde damit zur
  vollen Kontoübernahme für die Access-Token-Laufzeit führen. Architektonisch bewusst in Kauf
  genommen, aber undokumentiert war es vor diesem Abschnitt.
- **Passwort-Reset-Token-Versand**: aktuell kein echter E-Mail-Versand - siehe
  `docs/code-review.md` Abschnitt 2.1 (`APP_LOG_PASSWORD_RESET_TOKENS`).
