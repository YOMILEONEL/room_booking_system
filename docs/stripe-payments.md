# Stripe-Zahlungen (Test-Modus)

Zahlungen laufen über **Stripe Checkout** im Test-Modus - kostenlos, keine echten Kartendaten
nötig (Testkarte `4242 4242 4242 4242`). Eine Buchung wird ausschließlich durch eine erfolgreiche
Stripe-Zahlung von `PENDING` auf `PAID` gesetzt; es gibt keinen "Admin bestätigt manuell"-Weg mehr
in der Oberfläche (siehe "Admin-Override" unten für den technischen Rest davon).

## Ablauf

```
Buchung anlegen (BookingServiceImpl.createBookingFor)
  → Payment mit Status PENDING

Person klickt "Jetzt bezahlen" (BookingTable.tsx, nur Kunde/Organisation, nicht Admin)
  → POST /payment/{id}/checkout-session
  → Backend legt eine Stripe Checkout Session an (client_reference_id = Payment-ID)
  → Antwort: { url }  →  Frontend leitet den Browser dorthin weiter

Stripe-gehostete Zahlungsseite
  → Person zahlt mit einer Testkarte

  ├─ Redirect zurück zu /profile?payment=success bzw. ?payment=cancelled (sofort, informativ)
  └─ Asynchron, unabhängig vom Redirect: Stripe schickt einen Webhook
       POST /payment/stripe/webhook (checkout.session.completed)
       → Signatur geprüft, Payment über client_reference_id gefunden
       → Payment auf PAID gesetzt, Rechnung erzeugt (PaymentServiceImpl.markPaid,
         wiederverwendet von InvoiceServiceImpl.generateForPayment)
```

**Wichtig**: Der Redirect zurück (`success_url`) bedeutet nicht zwingend, dass die Buchung schon
als bezahlt in der DB steht - das passiert erst, wenn der Webhook ankommt (in der Praxis meist
innerhalb von Sekunden, aber nicht synchron mit dem Redirect). `/profile` zeigt deshalb bei
`?payment=success` einen Hinweis, kein "fertig"-Häkchen - siehe `PaymentStatusBanner` in
`react_frontend/src/app/profile/page.tsx`.

## Warum Stripe Checkout (gehostete Seite), nicht Stripe Elements

Checkout Sessions statt eines eingebetteten Kartenformulars (Stripe Elements/Payment Intents):
kein Stripe.js im Frontend nötig, keine Kartendaten laufen jemals durch eigenen Code - der
komplette PCI-Compliance-Scope bleibt bei Stripe. Nur ein Redirect zur von Stripe gehosteten Seite
und zurück, passt zum bereits redirect-basierten Stil dieser App (z. B. NextAuth-Login) und ist
der von Stripe selbst empfohlene einfachste Einstieg.

## Beteiligte Dateien

| Datei | Rolle |
|---|---|
| `payment/model/Payment.java` | Neues Feld `stripeCheckoutSessionId` (nur Nachvollziehbarkeit, nicht für die Zuordnung im Webhook) |
| `payment/service/StripeService.java`/`Impl` | Checkout Session anlegen, Webhook verifizieren+verarbeiten |
| `payment/service/PaymentService.java`/`Impl` | `markPaid(Payment)` - die eigentliche "auf bezahlt setzen + Rechnung erzeugen"-Logik, von Stripe-Webhook **und** Admin-Override genutzt |
| `payment/Controller/PaymentController.java` | `POST /payment/{id}/checkout-session` |
| `payment/Controller/StripeWebhookController.java` | `POST /payment/stripe/webhook` (öffentlich) |
| `user/configuration/SecurityConfig.java` | Erlaubt `/payment/stripe/webhook` ohne Bearer-Token |
| `react_frontend/src/app/api/payment.api.ts` | `createCheckoutSession(paymentId)` |
| `react_frontend/src/app/components/BookingTable.tsx` | "Jetzt bezahlen"-Button (nur Kunde/Organisation, `payment.status === "PENDING"`) |
| `react_frontend/src/app/profile/page.tsx` | Zeigt Erfolg/Abbruch-Hinweis nach dem Stripe-Redirect |

## Lokal testen: Webhook-Weiterleitung einrichten

**Das ist der wichtigste Schritt** - ohne ihn sieht die Integration verbunden aus (der Checkout
läuft durch), aber die Buchung bleibt für immer `PENDING`, weil Stripes Server `localhost:8080`
nicht direkt erreichen können.

1. Stripe CLI installieren - am einfachsten über npm (bereits vorhanden, kein neues
   Paketmanagement nötig):
   ```bash
   npm install -g @stripe/cli
   stripe --version   # zur Kontrolle
   ```
   Alternativen (Scoop, direkter ZIP-Download) stehen in der
   [offiziellen Anleitung](https://docs.stripe.com/stripe-cli).
2. `stripe login` (öffnet den Browser, einmalig; danach `Done!` in der Konsole).
3. Weiterleitung starten - **dieses Terminal muss während des gesamten Tests offen und aktiv
   bleiben**, sonst kommt das Event nie an:
   ```bash
   stripe listen --events checkout.session.completed --forward-to localhost:8080/payment/stripe/webhook
   ```
   Neuere CLI-Versionen (getestet: 1.52.1) verlangen `--events` (oder `--all-snapshot`)
   explizit - ohne die Angabe bricht der Befehl sofort mit "must specify events to forward"
   ab. Gibt danach eine Zeile wie `Ready! Your webhook signing secret is whsec_...` aus - dieser
   Wert kommt als `STRIPE_WEBHOOK_SECRET` in `bookingssystem/.env`. **Bleibt über Neustarts von
   `stripe listen` hinweg gleich** (offiziell von Stripe dokumentiert) - nur einmal eintragen,
   nicht bei jedem `stripe listen`-Start neu.
4. `STRIPE_SECRET_KEY` (Test-Key, beginnt mit `sk_test_`) von
   [dashboard.stripe.com/test/apikeys](https://dashboard.stripe.com/test/apikeys) ebenfalls in
   `bookingssystem/.env` eintragen, Backend-Container neu bauen (`docker compose up -d --build
   backend`).
5. **Reihenfolge beachten**: `stripe listen` muss schon laufen, **bevor** "Jetzt bezahlen"
   geklickt wird - eine bereits abgeschlossene Zahlung sendet ihr Event nicht nachträglich, wenn
   `stripe listen` erst danach gestartet wird (live selbst so erlebt: die erste Testzahlung blieb
   PENDING, weil die Weiterleitung zu dem Zeitpunkt noch nicht lief; ein zweiter Versuch mit
   bereits laufendem `stripe listen` hat funktioniert). Im Zweifel: `stripe listen` starten, "Ready!"
   abwarten, erst dann bezahlen.
6. Buchung anlegen, "Jetzt bezahlen" klicken, mit einer Testkarte bezahlen (siehe Tabelle unten) -
   in der `stripe listen`-Konsole erscheint das eingehende Event mit dem HTTP-Status der
   Weiterleitung (`[200]` = angekommen und akzeptiert), im Backend-Log (`docker logs
   booking-backend`) eine Zeile `payment ... marked PAID via webhook`. **Keine Zeile im
   Backend-Log ist dagegen kein Fehlersignal für sich genommen** - vor dieser Zeile loggt der
   Handler nur, wenn etwas auffällig war (unbekannte Zahlung, fehlende Signatur, ...); am
   zuverlässigsten ist, einfach `/profile` neu zu laden und den Status dort zu prüfen.

Für einen deployten Server (öffentlich erreichbare URL) stattdessen ein Webhook-Endpoint im
[Stripe-Dashboard](https://dashboard.stripe.com/webhooks) anlegen (Event `checkout.session.completed`
abonnieren) und dessen Signing Secret als `STRIPE_WEBHOOK_SECRET` verwenden - kein `stripe listen`
nötig, da der Webhook dann direkt zustellbar ist.

## Testkarten

| Szenario | Kartennummer |
|---|---|
| Zahlung erfolgreich | `4242 4242 4242 4242` |
| Erfordert 3-D-Secure-Authentifizierung | `4000 0025 0000 3155` |
| Zahlung abgelehnt | `4000 0000 0000 9995` |

Beliebiges zukünftiges Ablaufdatum, beliebiger 3-stelliger CVC, beliebige Postleitzahl - im
Test-Modus wird nichts davon echt geprüft.

## Sicherheitsmodell

`/payment/stripe/webhook` ist in `SecurityConfig` bewusst `permitAll()` - Stripe schickt keinen
Bearer-Token. Die Sicherheit kommt stattdessen aus `Webhook.constructEvent(payload, sigHeader,
webhookSecret)` (Stripe Java SDK): jede Anfrage ohne gültige `Stripe-Signature` (signiert mit dem
Webhook-Secret, das nur Stripe und dieses Backend kennen) wird mit 400 abgelehnt, **bevor**
irgendetwas mit dem Inhalt passiert. Ohne diese Prüfung könnte jeder mit einem einfachen POST an
den Endpunkt beliebige Buchungen als bezahlt markieren.

`POST /payment/{id}/checkout-session` bleibt dagegen normal authentifiziert
(`requireOwnerOrAdmin`) - nur die eigene Buchung (oder als Admin jede) kann bezahlt werden.

## Admin-Override: `PUT /payment/{id}/confirm`

Bleibt im Backend bestehen (nutzt intern dieselbe `markPaid`-Logik wie der Stripe-Webhook),
ist aber **bewusst nicht mehr in der Admin-Oberfläche verdrahtet** - im Normalbetrieb soll wirklich
nur Stripe eine Zahlung bestätigen. Gedacht als technischer Fallback für Fälle, die Stripe nicht
abdeckt (z. B. eine vor Ort bar bezahlte Buchung), erreichbar nur direkt über die API/Swagger UI
von einem Admin-Konto aus.

## Konfiguration

```
# bookingssystem/.env
STRIPE_SECRET_KEY=sk_test_…      # https://dashboard.stripe.com/test/apikeys
STRIPE_WEBHOOK_SECRET=whsec_…    # von "stripe listen" oder einem Dashboard-Webhook

# .env (Projekt-Root, docker-compose.yml)
APP_FRONTEND_URL=http://localhost:3000   # optional, Default passt für lokale Entwicklung
```

Beide Stripe-Werte leer lassen deaktiviert den Bezahlvorgang kontrolliert: `createCheckoutSession`
wirft eine klare Fehlermeldung ("Stripe ist auf diesem Server nicht konfiguriert") statt eines
Absturzes, der Rest der App läuft normal weiter - gleiches Muster wie `OPENAI_API_KEY`.

## Bekannte Grenzen

- **Nur `checkout.session.completed` behandelt.** Kartenzahlungen schließen synchron ab, das
  deckt den Regelfall vollständig ab. Asynchrone Zahlungsmethoden (z. B. SEPA-Lastschrift, die
  Tage braucht) bräuchten zusätzlich `checkout.session.async_payment_succeeded`/
  `..._payment_failed` - nicht implementiert, da Checkout hier nur Kartenzahlung anbietet.
- **Keine Rückerstattungen/Refunds.** Weder eine Oberfläche dafür noch ein Webhook-Handler für
  `charge.refunded` - eine Rückerstattung im Stripe-Dashboard ändert den `PAID`-Status hier nicht
  automatisch zurück.
- **Kein Stripe-Connect/Marketplace-Modell.** Ein einzelnes Stripe-Konto für den gesamten
  Betreiber, keine Auszahlung an einzelne Raumanbieter.
- **Kein Retry/Backoff bei einem vorübergehend nicht erreichbaren Backend.** Stripe wiederholt
  fehlgeschlagene Webhook-Zustellungen zwar automatisch (mehrere Stunden lang, exponentiell), aber
  das Backend selbst tut nichts Aktives, um eine verpasste Zustellung nachzuholen - siehe Stripes
  eigenes [Dashboard → Webhooks](https://dashboard.stripe.com/webhooks) für den Zustellstatus im
  Zweifelsfall.
