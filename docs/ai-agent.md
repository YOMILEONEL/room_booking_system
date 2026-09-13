# KI-Assistent (OpenAI)

`/assistant` ist ein Chat, über den eingeloggte **Kunden und Organisationen** Fragen zu ihren
eigenen Buchungen und zu den Räumen stellen können ("Wie viele Buchungen habe ich?", "Was ist
der günstigste Raum?", "Buch mir den günstigsten verfügbaren Raum für morgen bis übermorgen").
Er läuft über die **OpenAI API** mit dem günstigsten verfügbaren Modell, `gpt-5-nano`
($0,05 / 1M Input-Tokens, $0,40 / 1M Output-Tokens, Stand 2026-09) — Google Gemini wurde zuvor
genutzt (kostenloses Tier), aber wegen des sehr knappen Gratis-Kontingents (siehe "Bekannte
Grenzen" unten) auf bezahltes OpenAI-Guthaben umgestellt.

**Nur für Kunden/Organisationen, nicht für Admin** (Produktentscheidung, keine
Sicherheitsgrenze): Admins sehen im Verwaltungsbereich bereits alle Daten, "meine Buchungen"
ergibt für sie keinen Sinn. Der Link erscheint in der `NavBar` nur, wenn
`session.user.role !== "ADMIN"`; `/api/assistant` gibt zusätzlich serverseitig 403 zurück, falls
doch ein Admin-Token durchkommt.

## Beteiligte Dateien

| Datei | Rolle |
|---|---|
| `app/lib/assistant/openai-client.ts` | OpenAI-Client, Tool-Deklarationen, manuelle Konversationsführung (Chat Completions API) |
| `app/lib/assistant/tools.ts` | Ruft die bestehenden Spacio-Backend-Endpunkte (`/booking/getAll`, `/room/Get`) mit dem Access-Token der anfragenden Person auf |
| `app/lib/auth.ts` | `authOptions`, ausgelagert aus `api/auth/[...nextauth]/route.ts` (siehe unten, warum) |
| `app/api/assistant/route.ts` | Route Handler: Session/Rollen-Check, Validierung, ruft `askAssistant` |
| `app/api/assistant.api.ts` | Schlanker Client-seitiger Fetch-Wrapper für `/api/assistant` |
| `app/assistant/page.tsx`, `app/components/AssistantChat.tsx` | Chat-UI (Nachrichtenverlauf, Eingabefeld, Beispiel-Fragen, Bestätigungs-UI für Buchung/Stornierung) |
| `app/components/NavBar.tsx`, `AssistantWidget.tsx` | Verlinkt `/assistant` bzw. öffnet den Chat als Floating-Widget, nur für Nicht-Admin |

## Function Calling statt Freitext-Prompt

Der Assistent muss **echte, aktuelle Daten** der anfragenden Person abrufen — das Modell kennt
Spacios Datenbank nicht. Dafür gibt es zwei bewusst generische Lese-Tools (`get_my_bookings`,
`list_rooms`) statt eines Tools pro Frage-Variante: `list_rooms` liefert z. B. alle Räume mit
Preis und Verfügbarkeit, und das Modell selbst bestimmt daraus "günstigster", "günstigster
verfügbarer", "unter 50 €" etc. Das deckt neue Formulierungen ab, ohne dass für jede ein neues
Tool geschrieben werden muss.

Zwei weitere Tools, `create_booking` und `cancel_booking`, sind **Schreib**-Tools, aber sie
mutieren selbst nichts: Sie lösen Raumname/bookingId auf und geben stattdessen ein
`pending_booking`/`pending_cancellation`-Ergebnis an die Oberfläche zurück, die dort erst nach
expliziter Bestätigung ("Bestätigen"-Button) durch die Person die bereits bestehende
`createBooking`/`deleteBooking`-Logik aus dem normalen (Nicht-KI-)Buchungsablauf aufruft. Das
Modell kann also nie selbständig eine Buchung anlegen oder stornieren.

Die Tools rufen **keine Datenbank direkt auf**, sondern die immer schon vorhandenen,
bereits autorisierten Spacio-Endpunkte (`/booking/getAll`, `/room/Get`, `/booking/add`,
`/booking/delete/{id}`) mit dem Access-Token der anfragenden Person — `/booking/getAll` liefert
für ein MEMBER-Token ohnehin nur dessen eigene Buchungen, es gibt also keinen separaten
Autorisierungscode für den Assistenten zu pflegen.

## Manuelle Konversationsführung statt Helper-Loop

Wie schon in der vorherigen Gemini-Implementierung führt `askAssistant` die Tool-Calling-Schleife
**manuell** über `client.chat.completions.create({ messages, tools })`, statt einen
SDK-Runner-Helfer zu nutzen: der `messages`-Array wird selbst mitgeführt (System-Prompt, Frage,
Assistant-Turn mit `tool_calls`, dann pro Tool-Aufruf eine `role: "tool"`-Nachricht mit
`tool_call_id`), maximal 3 Runden. Ein `create_booking`/`cancel_booking`-Aufruf beendet die
Schleife sofort (die "terminal"-Variante in `executeTool`), statt dem Modell eine Tool-Antwort
zurückzugeben — es gibt für diese beiden Tools kein "danach" innerhalb der Konversation.

## Timeout gegen hängende Anfragen

Die vorherige Gemini-Integration hatte keinen Request-Timeout konfiguriert; ein einzelner Hänger
beim Provider blockierte die Anfrage über 5 Minuten lang, ohne Fehlermeldung für die Person.
Der OpenAI-Client wird deshalb explizit mit `timeout: 20_000` (20 Sekunden) und `maxRetries: 1`
instanziiert — ein hängender Request bricht jetzt zuverlässig mit einem normalen, für die Person
sichtbaren Fehler ab, statt unbegrenzt zu warten.

## Modellwahl: `gpt-5-nano`

Das günstigste verfügbare OpenAI-Modell (Stand 2026-09), ausreichend für diese Aufgabe: der
Assistent gibt größtenteils kurze, strukturierte Daten weiter, statt komplexes Reasoning zu
betreiben.

## Konfiguration

```
# .env.local / docker-compose.yml (OPENAI_API_KEY)
OPENAI_API_KEY=…   # https://platform.openai.com/api-keys, benötigt aufgeladenes Guthaben (ab $5)
```

Der Key wird ausschließlich serverseitig verwendet (`openai-client.ts`/`tools.ts` importieren
`"server-only"`) und landet nie im Browser-Bundle. Fehlt er, antwortet `/api/assistant` mit einer
klaren Fehlermeldung (`missing_api_key`) statt eines Absturzes — die App läuft ansonsten normal
weiter.

## Bekannte Grenzen

- **Bezahltes Guthaben statt Gratis-Tier**: anders als die vorherige Gemini-Integration braucht
  dieser Assistent ein aufgeladenes OpenAI-Guthaben; bei 429 (Guthaben/Kontingent aufgebraucht)
  bekommt die Person eine klare Fehlermeldung statt eines Absturzes. Kein Pro-Nutzer-Tageslimit
  eingebaut; bei echtem Kunden-Traffic sollte hier nachgerüstet werden (z. B. mit
  `security/RateLimiter.java` als Vorbild, serverseitig im Backend, oder analog in Next.js), um
  die Kosten pro Person zu begrenzen.
- Kein Gesprächsverlauf über einen Seiten-Reload hinaus (nur React-State).
- Nicht gestreamt: die Antwort kommt komplett auf einmal, kein Tippeffekt.
- Kein Retry mit Backoff bei transienten Fehlern über die eine `maxRetries: 1`-Wiederholung der
  SDK hinaus — die Person muss bei anhaltenden Fehlern manuell erneut senden.
