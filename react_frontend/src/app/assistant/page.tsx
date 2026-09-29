"use client";

import * as React from "react";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import NavBar from "../components/NavBar";
import AssistantChat from "../components/AssistantChat";
import { ConfirmDialog } from "../components/ui";
import {
  createAssistantSession,
  deleteAssistantSession,
  fetchAssistantSessions,
  type AssistantSessionSummary,
} from "../api/assistantSession.api";

const dateFormat = new Intl.DateTimeFormat("de-DE", { dateStyle: "short", timeStyle: "short" });

export default function AssistantPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const isAdmin = session?.user?.role === "ADMIN";

  const [sessions, setSessions] = React.useState<AssistantSessionSummary[]>([]);
  const [activeSessionId, setActiveSessionId] = React.useState<string | null>(null);
  const [loadingSessions, setLoadingSessions] = React.useState(true);
  const [sessionToDelete, setSessionToDelete] = React.useState<AssistantSessionSummary | null>(null);
  const [sidebarOpen, setSidebarOpen] = React.useState(true);

  React.useEffect(() => {
    if (status !== "authenticated" || isAdmin) return;

    let cancelled = false;

    const init = async () => {
      try {
        const list = await fetchAssistantSessions();
        if (cancelled) return;

        // The session created at this login (see lib/auth.ts) is always the default starting
        // point - it's already in `list` if the login-time backend call succeeded. Only create
        // a new one here as a fallback (e.g. that call failed, or this is an older login before
        // this feature existed).
        const freshId = session?.freshAssistantSessionId;
        const freshExists = freshId ? list.some((s) => s.id === freshId) : false;

        if (freshId && freshExists) {
          setSessions(list);
          setActiveSessionId(freshId);
        } else {
          const created = await createAssistantSession();
          if (cancelled) return;
          setSessions([created, ...list]);
          setActiveSessionId(created.id);
        }
      } catch (err) {
        console.error("Assistant-Sessions konnten nicht geladen werden:", err);
      } finally {
        if (!cancelled) setLoadingSessions(false);
      }
    };

    init();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only re-run on login state change, not on every session-list update
  }, [status, isAdmin, session?.freshAssistantSessionId]);

  const startNewSession = async () => {
    try {
      const created = await createAssistantSession();
      setSessions((prev) => [created, ...prev]);
      setActiveSessionId(created.id);
    } catch (err) {
      console.error("Neue Assistant-Session konnte nicht angelegt werden:", err);
    }
  };

  const confirmDelete = async () => {
    if (!sessionToDelete) return;
    const deletedId = sessionToDelete.id;
    setSessionToDelete(null);

    try {
      await deleteAssistantSession(deletedId);
      setSessions((prev) => {
        const remaining = prev.filter((s) => s.id !== deletedId);
        if (deletedId === activeSessionId) {
          if (remaining.length > 0) {
            setActiveSessionId(remaining[0].id);
          } else {
            // No sessions left at all - the chat needs an active session to render, so start a
            // fresh one right away instead of showing an empty picker with nothing to click.
            createAssistantSession()
              .then((created) => {
                setSessions([created]);
                setActiveSessionId(created.id);
              })
              .catch((err) => console.error("Neue Assistant-Session konnte nicht angelegt werden:", err));
          }
        }
        return remaining;
      });
    } catch (err) {
      console.error("Session konnte nicht gelöscht werden:", err);
    }
  };

  React.useEffect(() => {
    if (status === "unauthenticated") {
      router.push("/login");
    } else if (status === "authenticated" && isAdmin) {
      // Product decision, not a security boundary - see api/assistant/route.ts. Admins get
      // redirected the same way non-admins get redirected out of /admin.
      router.push("/");
    }
  }, [status, isAdmin, router]);

  if (status === "loading" || status === "unauthenticated" || isAdmin || loadingSessions || !activeSessionId) {
    return (
      <div className="flex justify-center mt-24">
        <p className="text-text-muted">Lädt...</p>
      </div>
    );
  }

  return (
    // h-screen + overflow-hidden (not min-h-screen) so the document itself never scrolls - only
    // the chat's own message list and the session sidebar scroll internally, each within their
    // own bounded box. min-h-screen let the chat's growing message list push the whole page
    // taller, which dragged the sidebar along with it when scrolling.
    <div className="h-screen flex flex-col overflow-hidden">
      <NavBar />

      <div className="flex-1 min-h-0 flex">
        <button
          type="button"
          onClick={() => setSidebarOpen((v) => !v)}
          aria-label={sidebarOpen ? "Session-Liste ausblenden" : "Session-Liste einblenden"}
          title={sidebarOpen ? "Session-Liste ausblenden" : "Session-Liste einblenden"}
          className="shrink-0 w-7 border-r border-border-subtle flex items-start justify-center pt-4 text-text-muted hover:text-text-primary hover:bg-black/[0.04] transition-colors"
        >
          {sidebarOpen ? "‹" : "›"}
        </button>

        {sidebarOpen && (
          <aside className="w-64 sm:w-72 shrink-0 border-r border-border-subtle flex flex-col min-h-0 overflow-hidden">
            <div className="p-3 border-b border-border-subtle shrink-0">
              <h1 className="text-sm font-bold px-1 mb-2">KI-Assistent</h1>
              <button
                type="button"
                onClick={startNewSession}
                className="w-full text-xs px-3 py-2 rounded-lg border border-primary text-primary font-medium hover:bg-primary/5 transition-colors"
              >
                + Neue Unterhaltung
              </button>
            </div>

            <div className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden p-2 flex flex-col gap-1">
              {sessions.map((s) => (
                <div
                  key={s.id}
                  className={`flex items-center gap-1 rounded-lg min-w-0 ${
                    s.id === activeSessionId ? "bg-primary/10" : "hover:bg-black/[0.04]"
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => setActiveSessionId(s.id)}
                    className="flex-1 min-w-0 text-left px-2.5 py-2 overflow-hidden"
                  >
                    <p
                      className={`text-xs truncate ${s.id === activeSessionId ? "text-primary font-medium" : "text-text-primary"}`}
                    >
                      {s.preview ?? "Neue Unterhaltung"}
                    </p>
                    <p className="text-[11px] text-text-muted truncate">{dateFormat.format(new Date(s.createdAt))}</p>
                  </button>
                  <button
                    type="button"
                    onClick={() => setSessionToDelete(s)}
                    aria-label="Unterhaltung löschen"
                    title="Unterhaltung löschen"
                    className="shrink-0 px-2 py-2 text-lg leading-none text-text-muted hover:text-danger transition-colors"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          </aside>
        )}

        <main className="flex-1 min-w-0 min-h-0 flex flex-col">
          <AssistantChat key={activeSessionId} sessionId={activeSessionId} />
        </main>
      </div>

      <ConfirmDialog
        open={sessionToDelete !== null}
        title="Unterhaltung löschen"
        message="Diese Unterhaltung und alle ihre Nachrichten werden endgültig gelöscht."
        confirmLabel="Löschen"
        onConfirm={confirmDelete}
        onCancel={() => setSessionToDelete(null)}
      />
    </div>
  );
}
