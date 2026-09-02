import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import { Card, Alert } from "../components/ui";

export default function AgbPage() {
  return (
    <div>
      <NavBar />

      <div className="max-w-3xl mx-auto px-4 sm:px-6 py-12 grid gap-6">
        <h1 className="text-2xl font-bold">Allgemeine Geschäftsbedingungen</h1>

        <Alert variant="info">
          Vorlage, kein rechtsverbindlicher Text. Alle mit [PLATZHALTER] markierten Stellen
          müssen ergänzt und die AGB vor Veröffentlichung rechtlich geprüft werden.
        </Alert>

        <Card className="grid gap-6 text-sm text-text-secondary">
          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">§ 1 Geltungsbereich</h2>
            <p>
              Diese Allgemeinen Geschäftsbedingungen gelten für alle Buchungen von Räumen über
              diese Plattform, betrieben von Spacio, [PLATZHALTER: Anschrift].
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">
              § 2 Registrierung und Vertragsschluss
            </h2>
            <p>
              Zur Buchung eines Raums ist eine Registrierung als Kunde oder Organisation
              erforderlich. Ein Vertrag über die Raumnutzung kommt mit Absenden der Buchung und
              Bestätigung durch das System zustande. Nach erfolgreicher Buchung wird automatisch
              eine Zahlung mit Status &bdquo;offen&ldquo; angelegt.
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">§ 3 Leistungsbeschreibung</h2>
            <p>
              Gegenstand der Buchung ist die zeitlich begrenzte Nutzung des gewählten Raums für
              den angegebenen Zeitraum (Start- bis Enddatum, jeweils einschließlich). Ein Raum
              kann nicht doppelt für sich überschneidende Zeiträume gebucht werden.
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">§ 4 Preise und Zahlung</h2>
            <p>
              Es gilt der zum Zeitpunkt der Buchung angezeigte Preis pro Tag, multipliziert mit
              der Anzahl der gebuchten Tage. Kunden vom Typ Organisation erhalten automatisch 10 %
              Rabatt auf den Raumpreis. Die Zahlung wird nach Eingang durch [PLATZHALTER:
              Zahlungsart/-abwicklung] bestätigt. Alle Preise verstehen sich in Euro
              [PLATZHALTER: inkl./zzgl. gesetzlicher Umsatzsteuer].
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">§ 5 Rabattcodes</h2>
            <p>
              Rabattcodes können von Kunden (nicht von Organisationen) im Rahmen ihrer jeweiligen
              Gültigkeit eingelöst werden. Ein Anspruch auf Ausstellung oder Verlängerung von
              Rabattcodes besteht nicht.
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">
              § 6 Änderung und Stornierung
            </h2>
            <p>
              Eine Buchung kann storniert werden, solange sie weder bereits bezahlt wurde noch
              der gebuchte Zeitraum den aktuellen Tag einschließt. Bereits bezahlte oder laufende
              Buchungen können nicht mehr storniert werden. [PLATZHALTER: ggf. Regelung zu
              Rückerstattungen ergänzen]
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">§ 7 Pflichten der Nutzer</h2>
            <p>
              Nutzer verpflichten sich, wahrheitsgemäße Angaben zu machen und den gebuchten Raum
              pfleglich zu behandeln. Zugangsdaten sind vertraulich zu behandeln.
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">§ 8 Haftung</h2>
            <p>
              [PLATZHALTER: Haftungsklausel gemäß den tatsächlichen Geschäftsbedingungen
              ergänzen, insbesondere Haftungsbeschränkung für leichte Fahrlässigkeit.]
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">§ 9 Schlussbestimmungen</h2>
            <p>
              Es gilt das Recht der Bundesrepublik Deutschland unter Ausschluss des
              UN-Kaufrechts. Gerichtsstand ist, soweit gesetzlich zulässig,
              [PLATZHALTER: Ort]. Sollten einzelne Bestimmungen dieser AGB unwirksam sein, bleibt
              die Wirksamkeit der übrigen Bestimmungen unberührt.
            </p>
          </section>
        </Card>
      </div>

      <Footer />
    </div>
  );
}
