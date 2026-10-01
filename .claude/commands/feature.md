---
description: Setzt ein Feature per Subagent-Pipeline um (Plan, Dev, Test, Review)
argument-hint: <Taskbeschreibung oder Pfad zu tasks/xyz.md>
---
Setze dieses Feature um: $ARGUMENTS

Ist $ARGUMENTS ein Pfad zu einer .md-Datei, lies sie zuerst vollständig.

## Regeln
- Du bist der Orchestrator. Schreibe den Code nicht selbst, sondern delegiere an die Subagents.
- Subagents haben keinen gemeinsamen Kontext. Jeder Prompt an einen Subagent muss alles enthalten, was er braucht (siehe Übergabe-Vorlage).
- Code läuft über das Dateisystem/Git, Anweisungen über den Prompt, Ergebnisse als kurzer Bericht.
- Keine parallelen Subagents im selben Modul.
- Niemals committen, pushen oder deployen.

## Ablauf (strikt einhalten)

1. **Klären**: Sind Ziel oder Akzeptanzkriterien unklar, stelle mir vorab höchstens 3 Fragen. Sonst weiter.
2. **Plan**: Schreibe `.work/plan.md` mit Ziel, Akzeptanzkriterien, betroffenen Dateien/Modulen, API-Änderung (Endpunkt, Request/Response-DTO), "nicht anfassen" und der Aufteilung Backend/Frontend. Zeige mir eine Kurzfassung (max. 10 Zeilen).
3. **Backend** (falls betroffen): Delegiere an `backend-dev`. Prompt: "Lies `.work/plan.md` und setze den Backend-Teil um" plus die Übergabe-Vorlage.
4. **API-Client**: Es gibt keinen Generator. Die Anpassung der handgeschriebenen Module in `react_frontend/src/app/api/*.api.ts` ist Teil von Schritt 5. Gib `frontend-dev` dafür die tatsächlich umgesetzten Endpunkte/DTOs aus dem Bericht von `backend-dev` mit.
5. **Frontend** (falls betroffen): Delegiere an `frontend-dev`, ebenfalls mit Übergabe-Vorlage.
6. **Test**: Delegiere an `tester` (Bereiche nennen: backend und/oder frontend). Bei `FAIL`: gib den Fehlerkern an den passenden Dev-Agent zurück und teste erneut. **Maximal 4 Runden.** Danach abbrechen und mir berichten, was nicht klappt.
7. **Review**: Bei `PASS` delegiere an `reviewer`. Bei `CHANGES`: Punkte mit Schweregrad hoch/mittel an den passenden Dev-Agent, danach zurück zu Schritt 6. Maximal 2 Review-Runden.
8. **Abschluss**: Schreibe `.work/summary.md` und zeige mir: was wurde umgesetzt, geänderte Dateien, Teststatus, offene Punkte. Frage, ob ich committen möchte (Branch-Name vorschlagen). Nicht selbst committen oder deployen.

## Übergabe-Vorlage für jeden Subagent-Prompt
- Ziel (ein Satz) und Akzeptanzkriterien
- Pfad zur Planung (`.work/plan.md`) und betroffene Dateien/Module
- relevanter API-Vertrag
- Ergebnis des vorherigen Schritts (z. B. Fehlerkern des Testers, nicht das ganze Log)
- Was NICHT angefasst werden darf
- Erwartetes Antwortformat

## Bei Abbruch
Fasse zusammen: bis wohin gekommen, was blockiert, Vorschlag für den nächsten Schritt.
