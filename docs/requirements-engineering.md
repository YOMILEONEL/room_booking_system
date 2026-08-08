# Requirements Engineering — Raumbuchungssystem

## 1. Einleitung

### 1.1 Zweck des Dokuments

Dieses Dokument beschreibt die Anforderungen an das Raumbuchungssystem — ein Buchungssystem für
Besprechungs-, Veranstaltungs- und Schulungsräume. Es wurde begleitend zur Implementierung
erstellt und bildet den tatsächlich umgesetzten Funktionsumfang als formale Anforderungen ab
(Ist-Zustand), ergänzt um bewusst getroffene Abgrenzungen. Es dient als Referenz für weitere
Entwicklung, Tests und Bewertung des Systems.

### 1.2 Geltungsbereich

Das System besteht aus einem Spring-Boot-Backend (REST-API) und einem Next.js-Frontend
(Weboberfläche), die gemeinsam eine Plattform bilden, über die

- Kunden (private Personen oder Organisationen) Räume einsehen und buchen können,
- Administratoren Räume, Buchungen, Zahlungen, Rabattcodes und Benutzer verwalten.

Nicht Teil des Systems: mobile Apps, Kalender-Integrationen Dritter, automatisierter
E-Mail-Versand, Anbindung eines echten Zahlungsdienstleisters (siehe Abschnitt 6, Abgrenzung).

### 1.3 Zielgruppe

Entwickler:innen, die das System warten oder erweitern; Prüfer:innen, die den Funktionsumfang
bewerten; zukünftige Bearbeiter:innen, die den Kontext einer Entscheidung nachvollziehen wollen.

## 2. Produktübersicht

Das Raumbuchungssystem ersetzt eine manuelle/telefonische Raumreservierung durch eine
Selbstbedienungs-Plattform: Kunden sehen Verfügbarkeit und Preise in Echtzeit und buchen selbst;
Administratoren behalten die volle Kontrolle über Raumbestand, Preise, Zahlungsstatus und können
im Namen von Kunden buchen (z. B. bei telefonischer Anfrage). Ein zentrales Geschäftsmerkmal ist
die Unterscheidung zwischen privaten Kunden und Organisationen mit automatischem Mengenrabatt für
Organisationen.

## 3. Akteure

| Akteur | Beschreibung |
|---|---|
| **Kunde (KUNDE)** | Registrierte Privatperson. Sieht aktive Räume, bucht für sich selbst, sieht eigene Buchungen/Rechnungen, kann Rabattcodes einlösen. |
| **Organisation (ORGANISATION)** | Registrierter Firmenkunde. Wie Kunde, erhält aber automatisch 10 % Rabatt auf jeden Raumpreis und kann im Gegenzug keine Rabattcodes einlösen. |
| **Administrator (ADMIN)** | Verwaltet Räume (inkl. Fotos), bestätigt Zahlungen, verwaltet Rabattcodes und Benutzer, sieht alle Buchungen und Kennzahlen, kann im Namen eines Kunden buchen. Kann nicht selbst als Nutzer Räume buchen. |
| **System** | Berechnet Preise, prüft Zeitraum-Überlappungen, erzeugt Rechnungen, verwaltet Authentifizierungs-Token. |

Ein Benutzerkonto ist entweder Administrator oder Kunde/Organisation — beide Eigenschaften
(Rolle und Kundentyp) sind unabhängig voneinander im Datenmodell abgebildet, aber ein
Administrator-Konto hat praktisch keinen Kundentyp, da Admins nicht selbst buchen.

## 4. Funktionale Anforderungen

Kennzeichnung: **FA-&lt;Bereich&gt;.&lt;Nummer&gt;**. "Das System muss…" markiert eine bereits
umgesetzte, verpflichtende Anforderung.

### 4.1 Registrierung & Authentifizierung

- **FA-AUTH.1** Das System muss die Registrierung mit Benutzername, Passwort (≥ 6 Zeichen) und
  Telefonnummer ermöglichen.
- **FA-AUTH.2** Bei der Registrierung muss ein Kundentyp gewählt werden: *Kunde* (erfordert
  Vor- und Nachname) oder *Organisation* (erfordert Organisationsname).
- **FA-AUTH.3** Jedes neu registrierte Konto muss automatisch die Rolle *Kunde* (MEMBER)
  erhalten; es darf keinen Weg über die API geben, sich selbst als Administrator zu registrieren.
- **FA-AUTH.4** Das System muss Login per Benutzername/Passwort anbieten und bei Erfolg ein
  Access-Token (kurzlebig) und ein Refresh-Token (langlebig) ausstellen.
- **FA-AUTH.5** Abgelaufene Access-Token müssen ohne erneute Passworteingabe über das
  Refresh-Token erneuert werden können.
- **FA-AUTH.6** Das System muss eine "Passwort vergessen"-Funktion anbieten, die unabhängig davon,
  ob der angegebene Benutzername existiert, dieselbe Rückmeldung liefert (keine Preisgabe, ob ein
  Konto existiert).
- **FA-AUTH.7** Ein Passwort-Reset-Token muss zeitlich begrenzt und nach einmaliger Nutzung
  ungültig sein.
- **FA-AUTH.8** Login und Registrierung müssen auf einer gemeinsamen Seite mit umschaltbaren
  Ansichten (Tabs) angeboten werden.
- **FA-AUTH.9** Nach erfolgreichem Login muss ein Administrator zum Admin-Bereich, ein Kunde zur
  Startseite weitergeleitet werden.

### 4.2 Raumverwaltung

- **FA-ROOM.1** Administratoren müssen Räume mit Name, Kapazität, Standort, Preis pro Nacht und
  Status anlegen und bearbeiten können.
- **FA-ROOM.2** Ein Raum muss mehrere Fotos haben können; eines davon ist das Titelbild.
  Administratoren müssen Fotos hochladen, löschen und die Reihenfolge ändern können.
- **FA-ROOM.3** Administratoren müssen Räume deaktivieren und wieder aktivieren können
  ("Soft Delete"); ein endgültiges Löschen eines Raums ist nicht vorgesehen.
- **FA-ROOM.4** Deaktivierte Räume dürfen für Kunden nicht sichtbar sein, müssen für
  Administratoren aber weiterhin sichtbar und bearbeitbar bleiben.
- **FA-ROOM.5** Kunden müssen aktive Räume mit Name, Standort, Kapazität, Foto(s) und Preis
  einsehen können.
- **FA-ROOM.6** Kunden müssen direkt in der Raumübersicht erkennen können, ob ein Raum aktuell
  gebucht ist und bis wann.
- **FA-ROOM.7** Der angezeigte Preis muss für Organisationen automatisch den Rabatt (siehe
  FA-PRICE.1) berücksichtigen; der reguläre Preis muss zusätzlich sichtbar bleiben (z. B.
  durchgestrichen), wenn er vom rabattierten Preis abweicht.

### 4.3 Buchung

- **FA-BOOK.1** Ein eingeloggter Kunde muss einen aktiven Raum für einen zusammenhängenden
  Zeitraum (Start-/Enddatum) für sich selbst buchen können.
- **FA-BOOK.2** Das System muss verhindern, dass ein Raum für sich überschneidende Zeiträume
  doppelt gebucht wird.
- **FA-BOOK.3** Ein Administrator muss im Namen eines bestehenden Kunden buchen können, indem er
  dessen E-Mail-Adresse/Benutzernamen angibt; das Zielkonto muss ein Kundenkonto (keine
  Administrator-Rolle) sein.
- **FA-BOOK.4** Ein Administrator darf keinen Raum für sich selbst über den regulären
  Buchungsweg buchen können.
- **FA-BOOK.5** Eine Buchung darf nicht gelöscht werden, wenn sie bereits bezahlt ist.
- **FA-BOOK.6** Eine Buchung darf nicht gelöscht werden, solange ihr Zeitraum den aktuellen Tag
  einschließt (laufende Buchung) — unabhängig davon, ob die Löschung von der buchenden Person
  oder einem Administrator ausgelöst wird.
- **FA-BOOK.7** Kunden müssen ihre eigenen Buchungen einsehen können; Administratoren müssen alle
  Buchungen einsehen können.

### 4.4 Preisgestaltung & Rabattcodes

- **FA-PRICE.1** Kunden vom Typ *Organisation* müssen automatisch 10 % Rabatt auf den
  Raumpreis erhalten — sowohl in der Anzeige als auch bei der tatsächlichen Buchung.
- **FA-PRICE.2** Kunden vom Typ *Organisation* dürfen keinen Rabattcode zusätzlich einlösen
  können.
- **FA-PRICE.3** Administratoren müssen Rabattcodes mit Typ (prozentual oder fester Betrag),
  Wert und Gültigkeitszeitraum anlegen, einsehen und löschen können.
- **FA-PRICE.4** Ein Kunde (nicht Organisation) muss bei der Buchung optional einen gültigen
  Rabattcode angeben können, der den Buchungspreis reduziert.
- **FA-PRICE.5** Ein ungültiger, abgelaufener oder deaktivierter Rabattcode muss mit einer
  verständlichen Fehlermeldung abgelehnt werden.

### 4.5 Zahlung & Rechnung

- **FA-PAY.1** Jede Buchung muss automatisch einen Zahlungsdatensatz mit Status "offen" (PENDING)
  erzeugen.
- **FA-PAY.2** Nur Administratoren dürfen eine Zahlung als "bezahlt" bestätigen.
- **FA-PAY.3** Beim Bestätigen einer Zahlung muss automatisch eine Rechnung mit fortlaufender,
  eindeutiger Rechnungsnummer erzeugt werden.
- **FA-PAY.4** Ein Kunde muss die Rechnung zu einer eigenen, bezahlten Buchung als PDF
  herunterladen können.
- **FA-PAY.5** Die Rechnung muss Rechnungsnummer, Datum, Kundenname, Raumname, Buchungszeitraum
  und Gesamtbetrag enthalten.

### 4.6 Administration

- **FA-ADMIN.1** Administratoren müssen eine Übersicht mit Kennzahlen sehen: Anzahl
  verfügbarer/belegter Räume, Anzahl Benutzer, bevorstehende Buchungen, Umsatz des laufenden
  Monats.
- **FA-ADMIN.2** Administratoren müssen offene Zahlungen einsehen und direkt aus der Übersicht
  bestätigen können.
- **FA-ADMIN.3** Administratoren müssen die meistgebuchten Räume und aktivsten Kunden einsehen
  können.
- **FA-ADMIN.4** Administratoren müssen alle Benutzerkonten einsehen und löschen können.
- **FA-ADMIN.5** Der Admin-Bereich muss über eine eigene Navigation erreichbar sein, die auf
  mobilen Geräten als ein-/ausklappbares Menü funktioniert.

## 5. Nicht-funktionale Anforderungen

- **NFA-SEC.1 (Authentifizierung)**: Zugriff auf alle nicht-öffentlichen Endpunkte muss einen
  gültigen Bearer-Token erfordern; Passwörter dürfen niemals im Klartext gespeichert werden
  (Hashing erforderlich).
- **NFA-SEC.2 (Autorisierung)**: Jeder Zugriff auf personenbezogene Daten (eigenes Profil, eigene
  Buchungen, eigene Zahlungen/Rechnungen) muss serverseitig auf den Besitzer oder einen
  Administrator beschränkt sein, unabhängig davon, was die Weboberfläche anzeigt oder verbirgt.
- **NFA-USA.1 (Sprache)**: Die Benutzeroberfläche muss durchgängig auf Deutsch sein.
- **NFA-USA.2 (Responsivität)**: Die gesamte Plattform, einschließlich des Admin-Bereichs, muss
  auf Bildschirmbreiten ab Smartphone-Größe nutzbar sein.
- **NFA-MAINT.1 (Wartbarkeit)**: Backend und Frontend müssen unabhängig voneinander baubar,
  testbar und deploybar sein.
- **NFA-MAINT.2 (Testbarkeit)**: Geschäftskritische Logik (Preisberechnung, Überlappungsprüfung,
  Berechtigungen) soll durch automatisierte Tests abgesichert sein, die ohne externe
  Abhängigkeiten (z. B. echte Datenbank) laufen.
- **NFA-PERF.1 (Antwortzeiten)**: Listen- und Detailabfragen (Räume, eigene Buchungen) sollen für
  die im Rahmen des Projekts realistische Datenmenge (niedrige drei- bis vierstellige Zeilenzahl
  pro Tabelle) ohne spürbare Verzögerung antworten.
- **NFA-COMPAT.1 (Browser)**: Die Weboberfläche muss in aktuellen Versionen gängiger Browser
  (Chrome, Firefox, Edge, Safari) funktionieren.
- **NFA-DEPLOY.1 (Betrieb)**: Das System muss per Docker Compose mit zwei Containern
  (Backend, Frontend) startbar sein, ohne dass eine lokale Datenbank installiert werden muss.

## 6. Rahmenbedingungen & Annahmen

- Die Datenbank ist eine extern gehostete PostgreSQL-Instanz (Supabase); es wird keine lokale
  Datenbank betrieben.
- Raumfoto-Uploads setzen eine konfigurierte Supabase-Storage-Anbindung voraus; ohne diese
  Konfiguration funktioniert die restliche Anwendung weiterhin, nur Foto-Uploads sind dann nicht
  möglich.
- Es existiert genau eine Währung (Euro) und eine Sprache (Deutsch) — keine
  Mehrwährungs- oder Mehrsprachigkeits-Unterstützung.
- Ein Administrator-Konto wird nicht über die Anwendung selbst angelegt, sondern durch direkten
  Datenbankzugriff.

## 7. Abgrenzung (Out of Scope)

Bewusst **nicht** Teil des Systems:

- Echter Zahlungsdienstleister (z. B. Stripe/PayPal) — der Zahlungsstatus wird von
  Administrator:innen manuell gepflegt.
- Automatisierter E-Mail-/SMS-Versand (Buchungsbestätigungen, Zahlungserinnerungen,
  Passwort-Reset-Links) — Passwort-Reset-Token werden derzeit serverseitig geloggt statt
  versendet (siehe [`code-review.md`](code-review.md), Abschnitt 2.1, als bekannte
  Übergangslösung).
- Kalenderintegration (z. B. Export als .ics, Sync mit Google/Outlook Kalender).
- Mehrsprachigkeit / Internationalisierung.
- Mehrere Währungen oder länderspezifische Steuersätze auf der Rechnung.
- Stornierung/Teilrückerstattung einer bereits bezahlten Buchung.
- Bewertungen/Rezensionen zu Räumen.
- Mobile Apps (native iOS/Android).

## 8. Glossar

| Begriff | Bedeutung |
|---|---|
| **Buchung** | Reservierung eines Raums durch einen Kunden für einen Zeitraum. |
| **Kunde** | Privater Nutzer-Account ohne Organisationsrabatt. |
| **Organisation** | Firmen-Account mit automatischem 10 %-Rabatt, ohne Rabattcode-Berechtigung. |
| **Rabattcode** | Von Administratoren angelegter Code, der den Buchungspreis um einen
  Prozentsatz oder Festbetrag reduziert. |
| **Soft Delete** | Deaktivieren statt endgültiges Löschen; Datensatz bleibt für Administratoren
  erhalten. |
| **Zahlung** | Datensatz zu einer Buchung mit Status "offen" oder "bezahlt". |
| **Rechnung** | Automatisch erzeugtes Dokument nach Zahlungsbestätigung, als PDF herunterladbar. |
