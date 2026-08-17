// Talks to this Next.js app's own /api/assistant route (not the Spacio backend directly) -
// that route holds the session server-side via getServerSession and calls Gemini, so no
// Authorization header is needed here, just the browser's NextAuth session cookie.

export async function askAssistant(question: string): Promise<string> {
  const res = await fetch("/api/assistant", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  });

  const data = await res.json().catch(() => null);

  if (!res.ok) {
    throw new Error(data?.error ?? `${res.status} ${res.statusText}`);
  }

  return data.answer as string;
}
