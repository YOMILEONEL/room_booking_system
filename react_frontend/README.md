# Spacio — Frontend

Next.js-Frontend (App Router) für [Spacio](../README.md). Spricht ausschließlich
über REST mit dem separaten Spring-Boot-Backend (`../bookingssystem`); Authentifizierung läuft
über NextAuth mit einem Credentials-Provider gegen die Backend-Login-Route.

## Tech-Stack

- Next.js 16 (App Router), React 19, TypeScript (`strict`)
- Tailwind CSS v3 — Farb-Tokens als CSS-Custom-Properties in `src/app/globals.css`, eingebunden
  über `tailwind.config.ts`; einheitliches, helles Theme über die ganze App
- NextAuth v4 (Credentials Provider, JWT-Sessions, automatischer Token-Refresh)
- OpenAI API (`gpt-5-nano`) für den KI-Assistenten (`lib/assistant/`, siehe
  [`../docs/ai-agent.md`](../docs/ai-agent.md))

## Struktur

```
src/app/
├── page.tsx, login/, rooms/, profile/, forgot-password/, reset-password/   Öffentliche/Kunden-Seiten
├── admin/                    Admin-Bereich (eigenes Layout mit Rollen-Guard + Sidebar)
├── components/                Geteilte UI-Bausteine (ui.tsx = Card/Button/Input/Badge/…),
│                               AssistantChat.tsx/AssistantWidget.tsx = KI-Assistent-Chat
├── lib/assistant/             OpenAI-Client, Tool-Deklarationen (KI-Assistent, serverseitig)
├── api/                       Ein Client-Modul pro Backend-Ressource (room.api.ts, booking.api.ts, …),
│                               alle über apiClient.ts (Auth-Header, Fehler-Handling, JSON/Text/Blob)
├── api/assistant/             Next.js-Route für den KI-Assistenten (ruft OpenAI serverseitig)
└── api/auth/[...nextauth]/    NextAuth-Route-Handler (Login, Token-Refresh, Session-Shape)
```

Jede Seite unter `rooms/`, `profile/` und `admin/*` prüft die Session clientseitig (`useSession` +
`useEffect`-Redirect); die eigentliche Berechtigungsprüfung erzwingt in jedem Fall das Backend.

## Entwicklung

```bash
npm install
npm run dev       # Dev-Server mit Turbopack, http://localhost:3000
npm run build     # Produktions-Build inkl. Type-Check und Lint
npm run start     # Produktions-Server (nach build)
npm run lint
```

Benötigt eine laufende Backend-Instanz (siehe [`../bookingssystem`](../bookingssystem)) sowie die
Umgebungsvariablen aus [`../README.md`](../README.md#lokal-starten) (`NEXTAUTH_SECRET`,
`NEXTAUTH_URL`, `NEXT_PUBLIC_BACKEND_URL`, optional `OPENAI_API_KEY`). Im Docker-Setup wird
zusätzlich `BACKEND_INTERNAL_URL` gesetzt, damit NextAuth serverseitig den Backend-Container statt
`localhost` erreicht.

Mehr Kontext (Feature-Überblick, Architektur, Requirements) im Projekt-weiten
[`docs/`](../docs/README.md)-Ordner.
