import { apiFetch } from "./apiClient";

const BASE = "/assistant/history";

export type AssistantMessageEntry = {
  id: string;
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt: string;
};

// Called directly against the backend (like booking.api.ts), not through /api/assistant - this
// is a plain read/append of the person's own saved conversation, no LLM call involved.
export async function fetchAssistantHistory(): Promise<AssistantMessageEntry[]> {
  const data = await apiFetch<AssistantMessageEntry[] | null>(BASE);
  return Array.isArray(data) ? data : [];
}

export async function logAssistantMessage(content: string): Promise<void> {
  await apiFetch<void>(BASE, {
    method: "POST",
    body: [{ role: "ASSISTANT", content }],
  });
}
