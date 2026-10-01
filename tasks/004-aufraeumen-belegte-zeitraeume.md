# Aufräumen nach "Belegte Zeiträume" (Review-Befunde)

## Problem
Das Review zu `tasks/001-belegte-zeitraeume.md` hat kleinere Punkte ohne Blocker gefunden:
1. `react_frontend/src/app/components/BookingAdd.tsx`: fehlendes Leerzeichen in
   `disabled={submitting || hasConflict}className=...`
2. `RoomServiceImpl.java`: die Konstanten `DEFAULT_WINDOW_DAYS`/`MAX_WINDOW_DAYS` stehen mitten in der Klasse
   (vor `getBookedPeriods`) statt oben bei den Feldern
3. `getBookedPeriods` nutzt `LocalDate.now()`. Die Tests berechnen "heute" selbst, ein Lauf genau um
   Mitternacht kann flackern.
4. Fehlende Integrationstests in `BookedPeriodsIntegrationTest`: deaktivierter Raum für Nicht-Admins → 404,
   Buchung genau am Fensterrand (Start = `to` bzw. Ende = `from`) ist enthalten

## Ziel
Code-Stil bereinigt, Tests deterministisch und die Grenzfälle über die echte Query abgedeckt.
Keine Verhaltensänderung.

## Backend
- Konstanten an den Klassenanfang verschieben
- `java.time.Clock` als Bean bereitstellen (z. B. in `config/`, `Clock.systemDefaultZone()`) und in
  `RoomServiceImpl` injizieren. `LocalDate.now(clock)` nur in `getBookedPeriods` verwenden, andere Stellen
  nicht umbauen.
- `RoomServiceImplTest`: mit `Clock.fixed(...)` testen statt "heute" selbst zu berechnen
- `BookedPeriodsIntegrationTest` ergänzen:
  - deaktivierter Raum, Kunde eingeloggt → 404; Admin → 200
  - Buchung mit `startTime == to` und Buchung mit `endTime == from` sind in der Antwort enthalten

## Frontend
- Leerzeichen in `BookingAdd.tsx` einfügen (reine Formatierung)

## Akzeptanzkriterien
- [ ] Keine Verhaltensänderung des Endpunkts
- [ ] Service-Tests nutzen eine feste Clock
- [ ] Neue Integrationstests für deaktivierten Raum und Fenstergrenzen grün
- [ ] Backend-Tests, Typecheck und Build grün; keine neuen Lint-Errors

## Nicht anfassen
- `BookingRepository.findOverlapping`
- Andere Verwendungen von `LocalDate.now()` außerhalb von `getBookedPeriods`
- Buchungs-, Zahlungs- und Stripe-Logik, `SecurityConfig`, Migrationen, Admin-Bereich
