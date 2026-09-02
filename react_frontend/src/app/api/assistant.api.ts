// Talks to this Next.js app's own /api/assistant route (not the Spacio backend directly) -
// that route holds the session server-side via getServerSession and calls Gemini, so no
// Authorization header is needed here, just the browser's NextAuth session cookie.

export type AssistantResult =
  | { type: "text"; text: string }
  | {
      type: "pending_booking";
      roomId: string;
      roomName: string;
      startDate: string;
      endDate: string;
      pricePerDay: number;
      discountCode?: string;
    }
  | {
      type: "pending_cancellation";
      bookingId: string;
      roomName: string;
      startDate: string;
      endDate: string;
    };

export async function askAssistant(question: string): Promise<AssistantResult> {
  const res = await fetch("/api/assistant", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  });

  const data = await res.json().catch(() => null);

  if (!res.ok) {
    throw new Error(data?.error ?? `${res.status} ${res.statusText}`);
  }

  return data as AssistantResult;
}
