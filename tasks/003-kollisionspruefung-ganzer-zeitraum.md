# Kollisionsprüfung im Buchungsformular über 90 Tage hinaus

## Problem
Seit `tasks/001-belegte-zeitraeume.md` warnt `BookingAdd` vor kollidierenden Zeiträumen. Die Detailseite
`rooms/[id]/page.tsx` lädt aber nur `getBookedPeriods(id)` mit dem Standardfenster heute bis heute + 90 Tage.
Daraus folgen zwei Lücken:
1. Wählt der Kunde einen Zeitraum, der ganz oder teilweise nach Tag 90 liegt, erkennt das Formular eine
   Kollision dort nicht. Der Kunde bekommt wieder erst nach dem Absenden den 409.
2. Schlägt das Laden fehl, übergibt die Seite `bookedPeriods={[]}`. Das Formular prüft dann stillschweigend
   gar nicht, ohne Hinweis im Formular.

## Ziel
Die Kollisionsprüfung im Formular deckt den tatsächlich gewählten Zeitraum ab. Ist sie nicht möglich,
wird das im Formular kenntlich gemacht.

## Frontend
- Anzeige-Liste auf der Detailseite bleibt bei "nächste 90 Tage" (unverändert)
- Für die Prüfung in `BookingAdd`: sobald Start und Ende gewählt sind und der Zeitraum über das geladene
  Fenster hinausgeht, `getBookedPeriods(roomId, start, ende)` für genau den gewählten Zeitraum laden
  (Backend erlaubt max. 366 Tage; bei längerem Zeitraum keine Prüfung, aber Hinweis)
  - Debounce bzw. nur bei Änderung der Daten laden, veraltete Antworten verwerfen (Race beim schnellen Ändern)
  - Während des Ladens: Absende-Button nicht blockieren, aber kurzer Hinweis "Verfügbarkeit wird geprüft…"
- Wenn das Laden fehlschlägt: neutraler Hinweis unter den Datumsfeldern ("Verfügbarkeit konnte nicht
  geprüft werden"), Absenden bleibt erlaubt (Backend prüft weiterhin mit 409)
- Überlappungsregel unverändert: `start <= belegtEnde && ende >= belegtStart`, ISO-Stringvergleich ohne `new Date`
- Keine neuen Lint-Errors, insbesondere kein synchrones `setState` im Effect-Body (`react-hooks/set-state-in-effect`)

## Backend
- Keine Änderung. Endpunkt `GET /room/{id}/booked-periods?from&to` existiert bereits.

## Akzeptanzkriterien
- [ ] Kollision mit einer Buchung nach Tag 90 wird vor dem Absenden erkannt, Button deaktiviert
- [ ] Ladefehler zeigt einen Hinweis im Formular und blockiert nicht
- [ ] Schnelles Ändern der Daten führt nicht zu veralteten Ergebnissen
- [ ] Verhalten ohne `fixedRoomId` unverändert
- [ ] Typecheck und Build grün; keine neuen Lint-Errors

## Nicht anfassen
- Zahlungs- und Stripe-Flow in `BookingAdd`
- Backend (`bookingssystem/`)
- Admin-Bereich
