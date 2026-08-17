# KI-Assistent (Gemini)

`/assistant` ist ein Chat, über den eingeloggte **Kunden und Organisationen** Fragen zu ihren
eigenen Buchungen und zu den Räumen stellen können ("Wie viele Buchungen habe ich?", "Was ist
der günstigste Raum?", "Was ist der günstigste Raum, der gerade verfügbar ist?"). Er läuft über
die **Gemini API (Google AI Studio)**, nicht über Anthropic/Claude — bewusste Entscheidung wegen
des kostenlosen Tiers, siehe die Begründung dazu in `CVio/cvio/docs/ai-agent.md`, dem
Schwesterprojekt, dessen Muster hier wiederverwendet wurde.

**Nur für Kunden/Organisationen, nicht für Admin** (Produktentscheidung, keine
Sicherheitsgrenze): Admins sehen im Verwaltungsbereich bereits alle Daten, "meine Buchungen"
ergibt für sie keinen Sinn. Der Link erscheint in der `NavBar` nur, wenn
`session.user.role !== "ADMIN"`; `/api/assistant` gibt zusätzlich serverseitig 403 zurück, falls
doch ein Admin-Token durchkommt.

## Beteiligte Dateien

| Datei | Rolle |
|---|---|
| `app/lib/assistant/gemini-client.ts` | Gemini-Client, Tool-Deklarationen, manuelle Konversationsführung |
| `app/lib/assistant/tools.ts` | Ruft die bestehenden Spacio-Backend-Endpunkte (`/booking/getAll`, `/room/Get`) mit dem Access-Token der anfragenden Person auf |
| `app/lib/auth.ts` | `authOptions`, ausgelagert aus `api/auth/[...nextauth]/route.ts` (siehe unten, warum) |
| `app/api/assistant/route.ts` | Route Handler: Session/Rollen-Check, Validierung, ruft `askAssistant` |
| `app/api/assistant.api.ts` | Schlanker Client-seitiger Fetch-Wrapper für `/api/assistant` |
| `app/assistant/page.tsx` | Chat-UI (Nachrichtenverlauf, Eingabefeld, Beispiel-Fragen) |
| `app/components/NavBar.tsx` | Verlinkt `/assistant`, nur für Nicht-Admin |

## Function Calling statt Freitext-Prompt

Anders als CVios Stellenabgleich (ein Text rein, ein festes JSON raus) muss der Assistent hier
**echte, aktuelle Daten** der anfragenden Person abrufen — Gemini kennt Spacios Datenbank nicht.
Dafür gibt es zwei bewusst generische Tools (`get_my_bookings`, `list_rooms`) statt eines Tools
pro Frage-Variante: `list_rooms` liefert z. B. alle Räume mit Preis und Verfügbarkeit, und Gemini
selbst bestimmt daraus "günstigster", "günstigster verfügbarer", "unter 50 €" etc. Das deckt neue
Formulierungen ab, ohne dass für jede ein neues Tool geschrieben werden muss.

Die Tools rufen **keine Datenbank direkt auf**, sondern die immer schon vorhandenen,
bereits autorisierten Spacio-Endpunkte (`/booking/getAll`, `/room/Get`) mit dem Access-Token der
anfragenden Person — `/booking/getAll` liefert für ein MEMBER-Token ohnehin nur dessen eigene
Buchungen, es gibt also keinen separaten Autorisierungscode für den Assistenten zu pflegen.

## Eigenständig gelöstes Problem: Rolle "function" wird abgelehnt

Der naheliegende Weg wäre `model.startChat()` + `chat.sendMessage(...)` gewesen (genau das Muster
aus CVio). Live gegen die echte Gemini-API getestet, schlug das aber fehl:

```
[400 Bad Request] Role 'function' is not supported.
Please use a valid role: SYSTEM, SYSTEM_1, USER, ASSISTANT, DEVELOPER, CONTEXT, USER_CONTEXT, MODEL, USER.
```

Grund: `@google/generative-ai@0.24.1`s `ChatSession.sendMessage(...)` verdrahtet für einen
`functionResponse`-Teil intern fest `role: "function"` (siehe
`node_modules/@google/generative-ai/dist/index.js`, `formatNewContent`). Die aktuell live
laufende Gemini-API akzeptiert diese Rolle nicht mehr, sondern erwartet die Funktionsantwort als
`role: "user"`-Turn zurück. Da sich das über die öffentliche `sendMessage`-Schnittstelle nicht
umbiegen lässt, führt `askAssistant` die Konversation stattdessen **manuell** über
`model.generateContent({ contents })`: der `contents`-Array wird selbst mitgeführt, und der
Funktionsantwort-Turn bekommt explizit `role: "user"` statt der von der SDK erzwungenen Rolle.

## Modellwahl: `gemini-flash-latest`

Wie in CVio der von Google gepflegte Alias, kein datiertes Modell wie `gemini-2.5-flash` — dort
wurde genau das ohne Vorwarnung für neue API-Keys gesperrt.

## Konfiguration

```
# .env.local / docker-compose.yml (GEMINI_API_KEY)
GEMINI_API_KEY=…   # https://aistudio.google.com/apikey, kostenloses Tier, kein Zahlungsmittel nötig
```

Der Key wird ausschließlich serverseitig verwendet (`gemini-client.ts`/`tools.ts` importieren
`"server-only"`) und landet nie im Browser-Bundle. Fehlt er, antwortet `/api/assistant` mit einer
klaren Fehlermeldung (`missing_api_key`) statt eines Absturzes — die App läuft ansonsten normal
weiter.

## Live verifiziert (Docker-Container, echte Testdaten)

Gegen die laufenden `booking-backend`/`booking-frontend`-Container, mit einem frisch registrierten
KUNDE-Testkonto (2 echte Buchungen, echte Raumpreise aus der Live-Datenbank):

- „Wie viele Buchungen habe ich?" → korrekt 2 Buchungen mit Raum/Zeitraum/Status genannt.
- „Was ist der günstigste Room?" → korrekt „Büro" (15 €, aktuell nicht verfügbar) genannt, plus
  von sich aus den günstigsten *verfügbaren* Raum ergänzt.
- „Was ist der günstigste Raum, der gerade verfügbar ist?" → korrekt „Konferenz-Raum" (45 €).
- Ohne Login: `POST /api/assistant` → 401.
- Leere Frage: → 400 mit verständlicher Meldung.

**Nicht live getestet:** die Admin-Sperre (403 für `role === "ADMIN"`) — dafür bräuchte es ein
echtes Admin-Konto; es gibt bewusst keinen API-Weg, ein Test-Konto zur Admin zu befördern, und
die `.env`-Zugangsdaten für einen direkten DB-Zugriff wurden hier nicht gelesen. Per Code-Review
verifiziert: derselbe einfache `session.user.role`-Vergleich, der für das MEMBER-Konto oben
nachweislich korrekt durch die Session lief.

## Bekannte Grenzen

- **Sehr knappes Gratis-Kontingent**: Googles Free-Tier für Flash-Modelle liegt aktuell bei RPD 20
  / RPM 5 fürs ganze Google-Cloud-Projekt (nicht pro Nutzer) — siehe CVios Erfahrung dazu. Kein
  Pro-Nutzer-Tageslimit wie CVios `resume_match_usage`-Tabelle ist eingebaut; bei echtem
  Kunden-Traffic sollte hier nachgerüstet werden (z. B. mit `security/RateLimiter.java` als
  Vorbild, serverseitig im Backend, oder analog in Next.js).
- Kein Gesprächsverlauf über einen Seiten-Reload hinaus (nur React-State).
- Nicht gestreamt: die Antwort kommt komplett auf einmal, kein Tippeffekt.
- Kein Retry mit Backoff bei transienten Fehlern (z. B. 503) — die Person muss manuell erneut
  senden.
