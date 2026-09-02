import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import { Card, Alert } from "../components/ui";

export default function ImpressumPage() {
  return (
    <div>
      <NavBar />

      <div className="max-w-3xl mx-auto px-4 sm:px-6 py-12 grid gap-6">
        <h1 className="text-2xl font-bold">Impressum</h1>

        <Alert variant="info">
          Vorlage gemäß § 5 TMG / § 18 MStV, kein rechtsverbindlicher Text. Alle mit
          [PLATZHALTER] markierten Angaben müssen vor Veröffentlichung durch die echten
          Unternehmensdaten ersetzt und die Seite von einer rechtskundigen Person geprüft werden.
        </Alert>

        <Card className="grid gap-6 text-sm text-text-secondary">
          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">Angaben gemäß § 5 TMG</h2>
            <p>Spacio [PLATZHALTER: ggf. vollständige Rechtsform, z. B. Spacio GmbH]</p>
            <p>[PLATZHALTER: Straße und Hausnummer]</p>
            <p>[PLATZHALTER: PLZ und Ort]</p>
            <p>[PLATZHALTER: Land]</p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">Vertreten durch</h2>
            <p>[PLATZHALTER: Geschäftsführer:in / Inhaber:in]</p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">Kontakt</h2>
            <p>Telefon: [PLATZHALTER]</p>
            <p>E-Mail: [PLATZHALTER]</p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">Registereintrag</h2>
            <p>
              Eintragung im [PLATZHALTER: Handelsregister/Vereinsregister/…] beim
              [PLATZHALTER: Registergericht]
            </p>
            <p>Registernummer: [PLATZHALTER]</p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">Umsatzsteuer-ID</h2>
            <p>
              Umsatzsteuer-Identifikationsnummer gemäß § 27 a Umsatzsteuergesetz:
              [PLATZHALTER]
            </p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">
              Verantwortlich für den Inhalt nach § 18 Abs. 2 MStV
            </h2>
            <p>[PLATZHALTER: Name, Anschrift wie oben]</p>
          </section>

          <section className="grid gap-1.5">
            <h2 className="text-base font-bold text-text-primary">EU-Streitschlichtung</h2>
            <p>
              Die Europäische Kommission stellt eine Plattform zur Online-Streitbeilegung (OS)
              bereit: [PLATZHALTER: Link zur aktuellen ec.europa.eu-OS-Plattform-URL]. Unsere
              E-Mail-Adresse finden Sie oben. Wir sind nicht bereit oder verpflichtet, an
              Streitbeilegungsverfahren vor einer Verbraucherschlichtungsstelle teilzunehmen.
              [PLATZHALTER: ggf. anpassen, falls doch eine Teilnahme erfolgt]
            </p>
          </section>
        </Card>
      </div>

      <Footer />
    </div>
  );
}
