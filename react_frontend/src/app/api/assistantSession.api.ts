import { apiFetch } from "./apiClient";

const BASE = "/assistant/sessions";

export type AssistantSessionSummary = {
  id: string;
  createdAt: string;
  preview: string | null;
};

export type AssistantMessageEntry = {
  id: string;
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt: string;
};

// All called directly against the backend (like booking.api.ts), not through /api/assistant -
// these are plain reads/writes of the person's own sessions/history, no LLM call involved.

export async function fetchAssistantSessions(): Promise<AssistantSessionSummary[]> {
  const data = await apiFetch<AssistantSessionSummary[] | null>(BASE);
  return Array.isArray(data) ? data : [];
}

export async function createAssistantSession(): Promise<AssistantSessionSummary> {
  return apiFetch<AssistantSessionSummary>(BASE, { method: "POST" });
}

// Best-effort, fire-and-forget from the caller (NavBar's logout handler) - deletes the given
// session only if it was never actually used (see AssistantSessionServiceImpl.deleteIfUnused on
// the backend), so it's safe to call even if the person did chat in it.
export async function deleteAssistantSessionIfUnused(sessionId: string): Promise<void> {
  await apiFetch<void>(`${BASE}/${sessionId}?onlyIfUnused=true`, { method: "DELETE" });
}

// Explicit, user-initiated delete (the "Löschen" button in the session list) - always deletes
// the session and its messages, unlike deleteAssistantSessionIfUnused above.
export async function deleteAssistantSession(sessionId: string): Promise<void> {
  await apiFetch<void>(`${BASE}/${sessionId}`, { method: "DELETE" });
}

export async function fetchAssistantHistory(sessionId: string): Promise<AssistantMessageEntry[]> {
  const data = await apiFetch<AssistantMessageEntry[] | null>(`${BASE}/${sessionId}/history`);
  return Array.isArray(data) ? data : [];
}

export async function logAssistantMessage(sessionId: string, content: string): Promise<void> {
  await apiFetch<void>(`${BASE}/${sessionId}/history`, {
    method: "POST",
    body: [{ role: "ASSISTANT", content }],
  });
}
