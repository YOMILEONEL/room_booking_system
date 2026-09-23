# Monitoring (Prometheus + Loki + Grafana)

Lokaler Observability-Stack für das Backend: **Metriken** (Request-Latenzen, Fehlerraten, JVM,
...) über Prometheus, **Logs** (inkl. Fehlertexte/Stacktraces) über Loki, beides zusammengeführt
in Grafana. Kein separater Log-Shipper-Container (z. B. Promtail) nötig - das Backend pusht seine
Logs direkt aus Logback an Loki.

## Warum Metriken UND Logs

Prometheus beantwortet "wie viele Fehler, wie oft, welcher Trend" (Zahlen). Es sagt aber nicht,
**was** der Fehler war - dafür braucht es die eigentlichen Log-Zeilen inkl. Stacktrace, die nur
Loki durchsuchbar macht. Beide zusammen in Grafana: ein Anstieg in einem Prometheus-Panel
("http_server_requests_seconds_count{status=~"5.."}") lässt sich direkt zu den passenden
Loki-Log-Zeilen im selben Zeitfenster verfolgen.

## Starten

Läuft **nicht** bei einem normalen `docker compose up -d` mit - nur explizit über das
`monitoring`-Profil, damit das Standard-Setup (nur `backend`+`frontend`) unverändert bleibt:

```bash
docker compose --profile monitoring up -d
```

| Was | URL |
|---|---|
| Grafana | http://localhost:3001 (Login: `admin` / `admin` bzw. `GRAFANA_ADMIN_PASSWORD` aus `.env`) |
| Prometheus | http://localhost:9090 |
| Loki (rohe HTTP-API) | http://localhost:3100 |

Grafana hat Prometheus und Loki bereits als Datenquellen vorkonfiguriert
(`monitoring/grafana/provisioning/datasources/datasources.yml`) - kein manuelles "Add data
source" nötig. Unter **Explore** direkt Metriken (Prometheus) oder Logs (Loki, z. B. Query
`{app="bookingssystem"}`) durchsuchen.

`docker compose up -d` (ohne `--profile monitoring`) stoppt oder startet die drei
Monitoring-Container nicht mit - sie laufen unabhängig weiter bzw. bleiben aus, je nachdem, was
zuletzt explizit gestartet wurde. Zum gezielten Stoppen: `docker compose --profile monitoring
stop prometheus loki grafana`.

## Wie die Verbindung funktioniert

### Metriken: Backend → Prometheus (Pull)

- `spring-boot-starter-actuator` + `micrometer-registry-prometheus` (siehe `pom.xml`) exponieren
  `/actuator/prometheus` im Prometheus-Textformat.
- `application.properties` gibt zusätzlich nur `health`, `info` und `prometheus` frei
  (`management.endpoints.web.exposure.include`) - bewusst nicht der volle `/actuator/**`-Umfang
  (`env`, `beans`, ...), der Konfigurationsdetails preisgeben würde.
- `SecurityConfig` erlaubt genau diese drei Pfade ohne Bearer-Token (`permitAll()`) - Prometheus
  hat keinen JWT zum Mitschicken. Jeder andere `/actuator/*`-Pfad bleibt hinter der normalen
  Authentifizierung.
- `monitoring/prometheus.yml` lässt Prometheus alle 15s `backend:8080/actuator/prometheus`
  abfragen (Docker-interner Service-Name, kein `localhost`).

### Logs: Backend → Loki (Push)

- `com.github.loki4j:loki-logback-appender` (in `pom.xml`) ist ein Logback-Appender, der Logs
  direkt über Lokis HTTP-Push-API sendet - `bookingssystem/src/main/resources/logback-spring.xml`
  konfiguriert ihn zusätzlich zum normalen Konsolen-Appender (`docker logs booking-backend` zeigt
  also weiterhin alles wie bisher).
- Ziel-URL kommt aus `LOKI_URL` (docker-compose.yml, Default `http://loki:3100/loki/api/v1/push`)
  über `application.properties`s `loki.url`.
- Label `level` (aus `%level`) + `app=bookingssystem` - bewusst wenige, kardinalitätsarme Labels;
  alles andere (Thread, Logger, Nachricht, Stacktrace) bleibt als durchsuchbarer Log-Text
  erhalten, wird aber nicht als Label indiziert (Loki-Best-Practice, sonst explodiert der Index).
- Der Appender ist intern non-blocking/gepuffert: ist Loki nicht erreichbar (z. B. `docker
  compose up -d` ohne `--profile monitoring`), blockiert oder crasht das Backend nicht - Logs
  gehen für dieses Fenster nur nicht nach Loki raus, die Konsolen-Logs sind unberührt. Gleiches
  Muster wie bei `OPENAI_API_KEY`/Supabase S3: optionale Integration, App funktioniert auch ohne.

### Versionswahl

`loki-logback-appender:2.0.3`, nicht das neuere 2.1.x - 2.1.x verlangt Logback 1.6.x, Spring Boot
3.5 bringt aber Logback 1.5.x mit (`spring-boot-dependencies` BOM). 2.0.x ist für Logback 1.5.x
gebaut und passt zur aktuellen Spring-Boot-Version.

## Was noch fehlt (bewusst nicht gebaut)

- Kein vorgefertigtes Grafana-Dashboard (JSON) - Explore reicht für den aktuellen Bedarf; ein
  Dashboard mit Panels für Request-Rate/Latenz/Fehlerrate lässt sich in Grafana selbst bauen und
  z. B. als JSON exportieren, falls gewünscht.
- Kein Alerting (Prometheus Alertmanager oder Grafana Alerting) konfiguriert.
- Kein Pro-Nutzer-/Pro-Request-Tracing (kein Micrometer Tracing/OpenTelemetry) - nur Metriken +
  Logs, keine verteilten Traces. Für dieses Projekt (ein Backend, kein Microservice-Mesh) bringt
  Tracing aktuell wenig zusätzlichen Wert gegenüber Metriken + Logs.
- Frontend (Next.js) ist nicht angebunden - nur das Spring-Boot-Backend liefert Metriken/Logs an
  diesen Stack.
