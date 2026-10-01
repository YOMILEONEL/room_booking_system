# NavBar: Vorname mit Profilbild, Profilbild-Upload im Profil

## Problem
Die NavBar (`react_frontend/src/app/components/NavBar.tsx`) zeigt für eingeloggte Nutzer zwei getrennte Dinge:
einen Link "Profil" und daneben als reinen Text den `displayName` ("Vorname Nachname" bzw. Organisationsname,
Fallback E-Mail). Der Name ist nicht klickbar, auf kleinen Bildschirmen ausgeblendet (`hidden sm:inline`), und
Nutzer können kein Profilbild hinterlegen.

## Ziel
In der NavBar steht statt "Profil" + vollem Namen ein **einziges klickbares Element**: Profilbild (rund) und
direkt daneben der **Vorname**. Klick öffnet `/profile`. Auf der Profilseite kann der Nutzer ein Profilbild
hochladen, ersetzen und entfernen. Das neue Bild erscheint sofort in der NavBar, ohne neuen Login.

## Ausgangslage (geprüft)
- `user/model/User.java` hat `firstName`, `lastName`, `organisationName`, `customerType`. Ein Bildfeld gibt es nicht.
- Login/Register liefern `AuthResponse` (`id, email, displayName, role, customerType, accessToken, refreshToken`).
  `displayName` kommt aus `User.getDisplayName()`.
- `lib/auth.ts` legt `displayName` im NextAuth-Token ab. `profile/page.tsx` aktualisiert ihn nach dem Speichern per
  `update({ email, displayName })`. Dasselbe Muster wird für das Bild genutzt.
- `storage/StorageServiceImpl.uploadRoomImage(...)` lädt in einen **öffentlichen** Supabase-Bucket hoch: prüft
  Content-Type und Magic Bytes, Dateiendung nur aus dem geprüften Typ, Key `rooms/<roomId>-<uuid>.<ext>`.
  Limit `spring.servlet.multipart.max-file-size=5MB`.
- `UserController` schützt `/user/...` per "eigenes Konto oder Admin" (z. B. `PUT /user/update/{id}`).
- Letzte Flyway-Migration: `V6__add_discount_codes.sql`.

## Backend
- Migration `V7__add_user_profile_image.sql`: Spalte `profile_image_url` (Text, nullable) in der User-Tabelle.
- `User.profileImageUrl`, ebenso in `UserDTO` und `AuthResponse` aufnehmen. Zusätzlich `firstName` in
  `AuthResponse`, damit die NavBar den Vornamen ohne Zusatzabfrage kennt.
- `StorageService`: neue Methode `uploadProfileImage(UUID userId, MultipartFile file)` mit derselben Prüfung wie
  `uploadRoomImage` (gemeinsame Validierung extrahieren, nicht kopieren). Key `avatars/<userId>-<uuid>.<ext>`.
  Dazu `delete(String publicUrl)` für das Entfernen des alten Bildes.
- Neue Endpunkte in `UserController` (mit `@Operation`/`@ApiResponses`):
  - `POST /user/{id}/profile-image` (multipart, Feld `file`) → 200 mit aktualisiertem `UserDTO`
  - `DELETE /user/{id}/profile-image` → 200 mit aktualisiertem `UserDTO` (`profileImageUrl = null`)
  - Zugriff: nur eigenes Konto oder Admin (gleiche Prüfung wie `update/{id}`), sonst 403. Ohne Login 401.
  - 400 bei fehlender, leerer oder ungültiger Datei. 413 bzw. 400 bei über 5 MB (bestehendes Verhalten des Multipart-Limits).
    Unbekannte ID → 404.
  - Beim Ersetzen bzw. Entfernen das alte Objekt im Bucket löschen (best effort: Fehler beim Löschen loggen, nicht
    an den Client durchreichen).
- Tests: Service (Ersetzen löscht altes Bild, Entfernen setzt `null`, Rechtecheck) mit gemocktem `StorageService`,
  Controller/Security (eigenes Konto 200, fremdes Konto 403, ohne Login 401, ungültige Datei 400).

## Frontend
- `api/user.api.ts`: `uploadProfileImage(userId, file)` und `deleteProfileImage(userId)`. `profileImageUrl` im
  User-Typ. Multipart über das bestehende `apiFetch`/`http.ts` (prüfen, wie `uploadRoomImage` es im Room-Client macht).
- `lib/auth.ts` und `types/next-auth.d.ts`: `firstName` und `profileImageUrl` wie `displayName` aus der
  Login-Antwort in Token und Session übernehmen und über `update(...)` aktualisierbar machen.
- Neue Komponente `components/Avatar.tsx`: rundes Bild. Ohne Bild ein Kreis mit Initiale (erster Buchstabe des
  angezeigten Namens). Größen über Prop. `<img>` wie in `RoomGallery.tsx` (kein `next/image`, damit keine
  `remotePatterns`-Änderung nötig ist).
- `NavBar.tsx`:
  - Link "Profil" und den Namenstext ersetzen durch **einen** `Link href="/profile"` mit `Avatar` (ca. 28 px) und
    dem Vornamen. `aria-label="Profil öffnen"`.
  - Angezeigter Name: `firstName` → bei fehlendem Vornamen (z. B. Organisationen) `displayName` → Teil der E-Mail
    vor dem `@`.
  - Mobil: nur der Avatar sichtbar, der Vorname ab `sm`.
  - Übrige Links (Räume, KI-Assistent, Verwaltung, Logout) unverändert.
- `profile/page.tsx`: Abschnitt "Profilbild" mit großer Vorschau (`Avatar`, ca. 96 px), Button "Bild hochladen"
  (Dateiauswahl `accept="image/png,image/jpeg,image/webp"`), Button "Entfernen" (nur wenn ein Bild existiert, mit
  `ConfirmDialog`). Clientseitige Vorprüfung: Typ und ≤ 5 MB, mit verständlicher Fehlermeldung. Ladezustand während
  des Uploads. Nach Erfolg `update({ profileImageUrl })`, damit die NavBar sofort aktualisiert. Beim Speichern des
  Profils auch `firstName` per `update` mitschicken.
- Keine neuen Lint-Errors (insbesondere kein synchrones `setState` im Effect-Body).

## Akzeptanzkriterien
- [ ] NavBar zeigt eingeloggt ein klickbares Element "Avatar + Vorname", das `/profile` öffnet. Der separate
      "Profil"-Link und der volle Name sind weg.
- [ ] Ohne Profilbild: Initialen-Kreis. Mit Profilbild: das Bild.
- [ ] Organisation ohne Vorname: es erscheint der Organisationsname (bzw. E-Mail-Teil).
- [ ] Profilseite: hochladen, ersetzen, entfernen funktioniert. NavBar aktualisiert sich ohne neuen Login.
- [ ] Nur eigenes Konto (oder Admin) darf ändern: 403 sonst, 401 ohne Login, 400 bei ungültiger Datei.
- [ ] Gefälschte Bilddatei (z. B. HTML als `image/png` deklariert) wird abgelehnt.
- [ ] Altes Bild wird beim Ersetzen bzw. Entfernen im Bucket gelöscht (best effort).
- [ ] Backend-Tests, Typecheck und Build grün; keine neuen Lint-Errors.

## Zu entscheiden, bevor es losgeht
1. **Sichtbarkeit der Bilder:** Der vorgeschlagene Weg nutzt den bestehenden **öffentlichen** Bucket (wie
   Raumfotos). Jeder mit der URL kann das Bild sehen. Die URL ist durch die UUID nicht erratbar, aber nicht geschützt.
   Alternative: privater Bucket mit signierten URLs (deutlich aufwendiger, URLs laufen ab).
2. **Organisationskunden:** Sie haben oft keinen Vornamen. Vorschlag: Organisationsname anzeigen. Alternativ:
   Vorname der Kontaktperson verpflichtend machen.
3. **Admins:** Vorschlag: Admins bekommen dasselbe Verhalten (Avatar und Vorname). Admins dürfen außerdem das
   Profilbild anderer Nutzer entfernen (z. B. bei unangemessenen Bildern). Eine Admin-UI dafür ist aber **nicht**
   Teil dieses Tasks.
4. **Zuschneiden:** Vorschlag: kein Zuschneide-Dialog. Das Bild wird per CSS rund und `object-cover` angezeigt.
   Ein Crop-Dialog wäre ein eigener Task (bräuchte vermutlich eine neue Dependency).

## Nicht anfassen
- `uploadRoomImage`-Verhalten und Raumfoto-Endpunkte (nur gemeinsame Validierung extrahieren, Verhalten gleich)
- Login/JWT-Logik, `SecurityConfig`, `JsonAuthenticationEntryPoint`
- Buchungs-, Zahlungs- und Stripe-Logik, Admin-Bereich (UI)
- Bestehende Flyway-Migrationen (nur neue `V7` hinzufügen)
- Keine neuen Frontend-Dependencies
- Keine Secrets aus `.env*` oder `application-prod.*` lesen. Supabase-Konfiguration nur über bestehende Properties.
