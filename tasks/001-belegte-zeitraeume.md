# Belegte Zeiträume eines Raums anzeigen

## Problem
Kunden sehen vor dem Buchen nicht, wann ein Raum schon belegt ist. Sie erfahren es erst nach dem
Absenden über einen 409-Fehler. Die Detailseite zeigt nur das statische Feld `roomStatus`
("Verfügbar"/"Gebucht"), nicht die tatsächlichen Buchungen.

## Ziel
Auf der Raum-Detailseite (`/rooms/[id]`) sieht jeder eingeloggte Nutzer die belegten Zeiträume des
Raums. Das Buchungsformular warnt schon vor dem Absenden, wenn der gewählte Zeitraum kollidiert.

## Backend
- Neuer Endpunkt `GET /room/{id}/booked-periods?from=YYYY-MM-DD&to=YYYY-MM-DD` im `RoomController`
- Antwort: Liste von `{ "startTime": "YYYY-MM-DD", "endTime": "YYYY-MM-DD" }`, sortiert nach `startTime`
- **Datenschutz:** keine User-Daten, keine bookingId, keine Zahlungsinfos in der Antwort
- `from`/`to` optional: Standard `from` = heute, `to` = heute + 90 Tage
- Validierung: `to` vor `from` → 400; Zeitraum > 366 Tage → 400
- Raum existiert nicht → 404
- Zugriff: jeder eingeloggte Nutzer (wie `GET /room/{id}`)
- Bestehende Query `BookingRepository.findOverlapping` wiederverwenden (excludeBookingId = null)
- Endpunkt mit `@Operation`/`@ApiResponses` dokumentieren

## Frontend
- `room.api.ts`: Funktion `getBookedPeriods(roomId, from?, to?)` und Typ `BookedPeriod`
- `rooms/[id]/page.tsx`: Abschnitt "Belegte Zeiträume (nächste 90 Tage)" als Liste
  im Format `TT.MM.JJJJ – TT.MM.JJJJ` (bestehendes `lib/formatDate.ts` nutzen)
  - Ladezustand, Fehlermeldung, Leerzustand ("Keine Buchungen in den nächsten 90 Tagen")
- `BookingAdd.tsx`: wenn `fixedRoomId` gesetzt ist und der gewählte Zeitraum mit einem belegten
  Zeitraum überlappt → Hinweis unter den Datumsfeldern und Absende-Button deaktiviert
  (Grenzen inklusive, wie im Backend: `start <= belegtEnde && ende >= belegtStart`)
- Nach erfolgreicher Buchung die Liste neu laden

## Akzeptanzkriterien
- [ ] Endpunkt liefert nur Start/Ende, sortiert, im angefragten Fenster
- [ ] 400 bei ungültigem Zeitraum, 404 bei unbekanntem Raum, 401 ohne Login
- [ ] Backend-Tests: Service (Überlappung, Sortierung, Defaults, Validierung) und Controller/Security
- [ ] Detailseite zeigt Liste inkl. Lade-, Fehler- und Leerzustand
- [ ] Formular blockiert kollidierende Zeiträume vor dem Absenden
- [ ] Backend-Tests, Typecheck und Build grün; keine neuen Lint-Errors

## Nicht anfassen
- Buchungs-, Zahlungs- und Stripe-Logik (`BookingServiceImpl.addBooking`, `payment/`)
- Login/JWT, `SecurityConfig` (außer falls für den neuen Pfad zwingend nötig, dann begründen)
- Bestehende Flyway-Migrationen (es wird keine neue Migration gebraucht)
- Admin-Bereich
