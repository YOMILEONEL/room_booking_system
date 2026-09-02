import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import { Card, Alert } from "../components/ui";

export default function DatenschutzPage() {
  return (
    <div>
      <NavBar />

      <div className="max-w-3xl mx-auto px-4 sm:px-6 py-12 grid gap-6">
        <h1 className="text-2xl font-bold">Datenschutzerklärung</h1>

        <Alert variant="info">
          Vorlage auf Basis der DSGVO, kein rechtsverbindlicher Text. Die tatsächliche
          Datenverarbeitung dieser Anwendung ist bereits eingetragen; alle mit [PLATZHALTER]
          markierten Stellen (Verantwortlicher, Kontakt, ggf. Auftragsverarbeiter-Details) müssen
          vor Veröffentlichung ergänzt und die Seite rechtlich geprüft werden.
        </Alert>

        <Card className="grid gap-6 text-sm text-text-secondary">
          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">1. Verantwortlicher</h2>
            <p>Spacio, [PLATZHALTER: Anschrift, E-Mail (siehe Impressum)]</p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">2. Ihre Rechte</h2>
            <p>
              Sie haben jederzeit das Recht auf Auskunft über Ihre gespeicherten Daten (Art. 15
              DSGVO), Berichtigung (Art. 16), Löschung (Art. 17), Einschränkung der Verarbeitung
              (Art. 18), Datenübertragbarkeit (Art. 20) sowie Widerspruch gegen die Verarbeitung
              (Art. 21). Wenden Sie sich dazu an [PLATZHALTER: Kontakt-E-Mail]. Außerdem besteht
              ein Beschwerderecht bei einer Datenschutzaufsichtsbehörde.
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">
              3. Registrierung und Nutzerkonto
            </h2>
            <p>
              Bei der Registrierung erheben wir Benutzername, Passwort (gehasht gespeichert,
              nicht im Klartext), Telefonnummer sowie je nach gewähltem Kontotyp entweder Vor-
              und Nachname (Kunde) oder Organisationsname (Organisation). Rechtsgrundlage ist die
              Vertragserfüllung (Art. 6 Abs. 1 lit. b DSGVO) für die Bereitstellung des
              Buchungskontos.
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">
              4. Buchungs- und Zahlungsdaten
            </h2>
            <p>
              Zu jeder Raumbuchung speichern wir den gebuchten Raum, den Zeitraum, den
              berechneten Betrag sowie den Zahlungsstatus. Nach Bestätigung einer Zahlung wird
              zusätzlich eine Rechnung mit den zu diesem Zeitpunkt gültigen Angaben (Name, Raum,
              Betrag) erzeugt. Rechtsgrundlage ist ebenfalls die Vertragserfüllung sowie
              gesetzliche Aufbewahrungspflichten für Rechnungen (Art. 6 Abs. 1 lit. c DSGVO).
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">
              5. Anmeldung und Sitzungsdaten
            </h2>
            <p>
              Für die Anmeldung verwenden wir technisch notwendige Cookies/Session-Token, um Sie
              während Ihres Besuchs eingeloggt zu halten. Diese sind für den Betrieb der
              Anwendung erforderlich (Art. 6 Abs. 1 lit. f DSGVO) und werden nicht zu
              Marketingzwecken eingesetzt.
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">
              6. Weitergabe an Auftragsverarbeiter
            </h2>
            <p>
              Für Datenbank-Hosting und die Speicherung von Raumfotos nutzen wir Supabase
              (Supabase Inc.) als Auftragsverarbeiter. Mit diesem Anbieter besteht
              [PLATZHALTER: ggf. Auftragsverarbeitungsvertrag gemäß Art. 28 DSGVO ergänzen bzw.
              bestätigen]. Eine Übermittlung an weitere Dritte findet nicht statt, soweit nicht
              gesetzlich vorgeschrieben.
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">7. Speicherdauer</h2>
            <p>
              Kontodaten werden gespeichert, solange das Nutzerkonto besteht. Buchungs- und
              Rechnungsdaten werden entsprechend der gesetzlichen Aufbewahrungsfristen (in der
              Regel 10 Jahre für steuerrelevante Unterlagen) vorgehalten. [PLATZHALTER: konkrete
              Löschroutine ergänzen, falls vorhanden]
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">
              8. Änderung dieser Erklärung
            </h2>
            <p>
              Wir passen diese Datenschutzerklärung an, sobald sich die Datenverarbeitung ändert.
              Es gilt die jeweils aktuelle, auf dieser Seite veröffentlichte Fassung.
            </p>
          </section>
        </Card>
      </div>

      <Footer />
    </div>
  );
}
