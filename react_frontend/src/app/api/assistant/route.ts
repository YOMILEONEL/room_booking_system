import { getServerSession } from "next-auth";
import { NextResponse } from "next/server";
import { authOptions } from "../../lib/auth";
import { askAssistant, AssistantError, type AssistantResult } from "../../lib/assistant/openai-client";
import { logAssistantMessages } from "../../lib/assistant/tools";

const currency = new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" });

// Turns a result into the same human-readable summary the chat UI itself would show, so the
// saved history reads the same way live and on reload - never the raw roomId/bookingId.
function resultToLogText(result: AssistantResult): string {
  if (result.type === "text") {
    return result.text;
  }
  if (result.type === "pending_booking") {
    const discount = result.discountCode ? ` mit Rabattcode ${result.discountCode}` : "";
    return `Vorschlag: Buchung für "${result.roomName}", ${result.startDate} bis ${result.endDate} (${currency.format(result.pricePerDay)}/Tag)${discount} - wartet auf Bestätigung.`;
  }
  return `Vorschlag: Buchung für "${result.roomName}" (${result.startDate} bis ${result.endDate}) stornieren - wartet auf Bestätigung.`;
}

const MAX_QUESTION_LENGTH = 500;

export async function POST(request: Request) {
  const session = await getServerSession(authOptions);

  if (!session?.accessToken) {
    return NextResponse.json({ error: "Nicht angemeldet." }, { status: 401 });
  }

  // Product decision, not a security boundary (the backend already scopes every tool call to
  // the caller's own token): the assistant answers customer questions about "meine Buchungen" /
  // room prices, which doesn't map to an admin's role - admins see everything already via the
  // Verwaltung-Dashboard, so this feature is deliberately Kunde/Organisation-only.
  if (session.user?.role === "ADMIN") {
    return NextResponse.json({ error: "Der KI-Assistent ist nur für Kunden und Organisationen verfügbar." }, { status: 403 });
  }

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Ungültige Anfrage." }, { status: 400 });
  }

  const question = (body as { question?: unknown })?.question;
  if (typeof question !== "string" || !question.trim()) {
    return NextResponse.json({ error: "Bitte eine Frage eingeben." }, { status: 400 });
  }
  if (question.length > MAX_QUESTION_LENGTH) {
    return NextResponse.json({ error: `Frage ist zu lang (max. ${MAX_QUESTION_LENGTH} Zeichen).` }, { status: 400 });
  }

  try {
    const trimmedQuestion = question.trim();
    const result = await askAssistant(trimmedQuestion, session.accessToken);

    // Best-effort: the person is waiting on the reply above, a logging hiccup shouldn't turn
    // into a 500 for an otherwise-successful answer.
    logAssistantMessages(
      [
        { role: "USER", content: trimmedQuestion },
        { role: "ASSISTANT", content: resultToLogText(result) },
      ],
      session.accessToken
    ).catch((err) => console.error("[assistant] Verlauf konnte nicht gespeichert werden:", err));

    return NextResponse.json(result);
  } catch (error) {
    if (error instanceof AssistantError) {
      const status = error.code === "app_quota_exceeded" ? 429 : 502;
      return NextResponse.json({ error: error.message, code: error.code }, { status });
    }
    console.error("[assistant] Unerwarteter Fehler:", error);
    return NextResponse.json({ error: "Der KI-Assistent konnte gerade nicht antworten." }, { status: 500 });
  }
}
