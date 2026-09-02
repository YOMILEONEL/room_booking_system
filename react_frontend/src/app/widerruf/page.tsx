import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import { Card, Alert } from "../components/ui";

export default function WiderrufPage() {
  return (
    <div>
      <NavBar />

      <div className="max-w-3xl mx-auto px-4 sm:px-6 py-12 grid gap-6">
        <h1 className="text-2xl font-bold">Widerrufsbelehrung</h1>

        <Alert variant="info">
          Vorlage, kein rechtsverbindlicher Text. Diese Seite muss vor Veröffentlichung
          zwingend rechtlich geprüft werden (siehe Hinweis unten zu § 312g BGB).
        </Alert>

        <Card className="grid gap-6 text-sm text-text-secondary">
          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">
              Wichtiger Hinweis zur Anwendbarkeit
            </h2>
            <p>
              Das gesetzliche Widerrufsrecht für Verbraucher gilt nach § 312g Abs. 2 Nr. 9 BGB
              grundsätzlich <strong>nicht</strong> für Verträge zur Beherbergung zu anderen
              Zwecken als zu Wohnzwecken, wenn der Vertrag einen bestimmten Termin oder Zeitraum
              vorsieht, was auf datumsgebundene Raumbuchungen typischerweise zutrifft. Ob diese
              Ausnahme für das konkrete Geschäftsmodell greift, muss [PLATZHALTER: rechtlich
              geprüft werden]. Die folgende Belehrung ist daher nur als generische Vorlage zu
              verstehen und darf nicht ungeprüft veröffentlicht werden.
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">Widerrufsrecht</h2>
            <p>
              Sofern und soweit ein Widerrufsrecht besteht, haben Sie das Recht, binnen
              vierzehn Tagen ohne Angabe von Gründen diesen Vertrag zu widerrufen. Die
              Widerrufsfrist beträgt vierzehn Tage ab dem Tag des Vertragsschlusses.
            </p>
            <p>
              Um Ihr Widerrufsrecht auszuüben, müssen Sie uns (Spacio, [PLATZHALTER: Anschrift,
              Telefonnummer, E-Mail-Adresse]) mittels einer eindeutigen Erklärung
              (z. B. per Post versandter Brief oder E-Mail) über Ihren Entschluss, diesen
              Vertrag zu widerrufen, informieren.
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">Folgen des Widerrufs</h2>
            <p>
              Im Falle eines wirksamen Widerrufs sind die beiderseits empfangenen Leistungen
              zurückzugewähren. Bereits gezahlte Beträge werden unverzüglich, spätestens binnen
              vierzehn Tagen ab Eingang der Widerrufserklärung, zurückerstattet.
              [PLATZHALTER: Rückzahlungsweg ergänzen]
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">Muster-Widerrufsformular</h2>
            <p>
              (Wenn Sie den Vertrag widerrufen wollen, füllen Sie bitte dieses Formular aus und
              senden Sie es zurück.)
            </p>
            <p>
              An Spacio, [PLATZHALTER: Anschrift, E-Mail-Adresse]
              <br />
              Hiermit widerrufe(n) ich/wir den von mir/uns abgeschlossenen Vertrag über die
              Buchung des folgenden Raums: ______________________
              <br />
              Gebucht am: __________ Name des Verbrauchers: __________
              <br />
              Anschrift des Verbrauchers: __________
              <br />
              Datum: __________
            </p>
          </section>
        </Card>
      </div>

      <Footer />
    </div>
  );
}
