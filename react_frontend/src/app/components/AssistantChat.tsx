"use client";

import * as React from "react";
import { useSession } from "next-auth/react";
import { askAssistant, type AssistantResult } from "../api/assistant.api";
import { createBooking, deleteBooking } from "../api/booking.api";
import { extractErrorMessage } from "../api/apiClient";
import { formatLocalDate } from "../lib/formatDate";
import { Button, TextInput, Alert } from "./ui";
import MarkdownLite from "./MarkdownLite";

const currency = new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" });

type ActionStatus = "pending" | "processing" | "confirmed" | "declined" | "error";

type ChatMessage =
  | { id: string; role: "user"; kind: "text"; text: string }
  | { id: string; role: "assistant"; kind: "text"; text: string }
  | {
      id: string;
      role: "assistant";
      kind: "pending_booking";
      roomId: string;
      roomName: string;
      startDate: string;
      endDate: string;
      pricePerDay: number;
      discountCode?: string;
      status: ActionStatus;
      resultText?: string;
    }
  | {
      id: string;
      role: "assistant";
      kind: "pending_cancellation";
      bookingId: string;
      roomName: string;
      startDate: string;
      endDate: string;
      status: ActionStatus;
      resultText?: string;
    };

const EXAMPLE_QUESTIONS = [
  "Wie viele Buchungen habe ich?",
  "Was ist der günstigste Raum?",
  "Buch mir den günstigsten verfügbaren Raum für morgen bis übermorgen",
];

function resultToMessage(result: AssistantResult): ChatMessage {
  const id = crypto.randomUUID();
  if (result.type === "text") {
    return { id, role: "assistant", kind: "text", text: result.text };
  }
  if (result.type === "pending_booking") {
    return {
      id,
      role: "assistant",
      kind: "pending_booking",
      status: "pending",
      roomId: result.roomId,
      roomName: result.roomName,
      startDate: result.startDate,
      endDate: result.endDate,
      pricePerDay: result.pricePerDay,
      discountCode: result.discountCode,
    };
  }
  return {
    id,
    role: "assistant",
    kind: "pending_cancellation",
    status: "pending",
    bookingId: result.bookingId,
    roomName: result.roomName,
    startDate: result.startDate,
    endDate: result.endDate,
  };
}

// Shared by the full-page /assistant view and the site-wide floating AssistantWidget - only
// the surrounding chrome (page shell vs. floating panel) differs between the two.
export default function AssistantChat({ compact = false }: { compact?: boolean }) {
  const { data: session } = useSession();
  const userId = session?.user?.id ?? null;

  const [messages, setMessages] = React.useState<ChatMessage[]>([]);
  const [question, setQuestion] = React.useState("");
  const [sending, setSending] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const scrollRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages]);

  const updateMessage = (id: string, patch: Record<string, unknown>) => {
    setMessages((prev) => prev.map((m) => (m.id === id ? ({ ...m, ...patch } as ChatMessage) : m)));
  };

  const sendQuestion = async (text: string) => {
    const trimmed = text.trim();
    if (!trimmed || sending) return;

    setError(null);
    setMessages((prev) => [...prev, { id: crypto.randomUUID(), role: "user", kind: "text", text: trimmed }]);
    setQuestion("");
    setSending(true);

    try {
      const result = await askAssistant(trimmed);
      setMessages((prev) => [...prev, resultToMessage(result)]);
    } catch (err) {
      console.error("Fehler beim Anfragen des KI-Assistenten:", err);
      setError(err instanceof Error ? err.message : "Der KI-Assistent konnte nicht antworten.");
    } finally {
      setSending(false);
    }
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    sendQuestion(question);
  };

  // Confirmation is a hard gate, not just a prompt suggestion: Gemini only ever proposes
  // roomId/dates via create_booking - the actual mutation happens here, through the same
  // createBooking() call the normal booking form uses, and only after this explicit click.
  const confirmBooking = async (msg: Extract<ChatMessage, { kind: "pending_booking" }>) => {
    if (!userId) return;
    updateMessage(msg.id, { status: "processing" });
    try {
      const booking = await createBooking({
        roomId: msg.roomId,
        userId,
        startTime: msg.startDate,
        endTime: msg.endDate,
        discountCode: msg.discountCode,
      });
      const amount = booking.payment?.amount;
      updateMessage(msg.id, {
        status: "confirmed",
        resultText:
          amount !== undefined
            ? `Gebucht: ${msg.roomName}, ${formatLocalDate(msg.startDate)} – ${formatLocalDate(msg.endDate)}. Preis: ${currency.format(amount)}`
            : `Gebucht: ${msg.roomName}, ${formatLocalDate(msg.startDate)} – ${formatLocalDate(msg.endDate)}.`,
      });
    } catch (err) {
      console.error("Fehler beim Anlegen der Buchung:", err);
      updateMessage(msg.id, { status: "error", resultText: extractErrorMessage(err, "Buchung fehlgeschlagen.") });
    }
  };

  const confirmCancellation = async (msg: Extract<ChatMessage, { kind: "pending_cancellation" }>) => {
    updateMessage(msg.id, { status: "processing" });
    try {
      await deleteBooking(msg.bookingId);
      updateMessage(msg.id, {
        status: "confirmed",
        resultText: `Storniert: ${msg.roomName}, ${formatLocalDate(msg.startDate)} – ${formatLocalDate(msg.endDate)}.`,
      });
    } catch (err) {
      console.error("Fehler beim Stornieren der Buchung:", err);
      updateMessage(msg.id, { status: "error", resultText: extractErrorMessage(err, "Stornierung fehlgeschlagen.") });
    }
  };

  return (
    <div className={`flex flex-col ${compact ? "h-full" : "h-[60vh]"}`}>
      <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 grid gap-3 content-start">
        {messages.length === 0 && (
          <div className="grid gap-2">
            <p className="text-sm text-text-muted">Ein paar Beispiele zum Ausprobieren:</p>
            <div className="flex flex-wrap gap-2">
              {EXAMPLE_QUESTIONS.map((q) => (
                <button
                  key={q}
                  type="button"
                  onClick={() => sendQuestion(q)}
                  className="text-xs px-3 py-1.5 rounded-full border border-border-subtle text-text-secondary hover:text-text-primary hover:bg-black/[0.04] transition-colors"
                >
                  {q}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((m) => {
          if (m.kind === "text") {
            return (
              <div
                key={m.id}
                className={`max-w-[85%] px-3.5 py-2.5 rounded-2xl text-sm ${
                  m.role === "user"
                    ? "self-end bg-primary text-white rounded-br-sm"
                    : "self-start bg-black/[0.04] text-text-primary rounded-bl-sm"
                }`}
              >
                {m.role === "assistant" ? <MarkdownLite text={m.text} /> : m.text}
              </div>
            );
          }

          const isBooking = m.kind === "pending_booking";
          return (
            <div
              key={m.id}
              className="self-start max-w-[90%] rounded-2xl rounded-bl-sm border border-border-subtle bg-card p-3.5 text-sm grid gap-2.5"
            >
              {isBooking ? (
                <p>
                  <span className="font-semibold">{m.roomName}</span>
                  {", "}
                  {formatLocalDate(m.startDate)} – {formatLocalDate(m.endDate)}
                  {" · "}
                  {currency.format(m.pricePerDay)}/Tag
                  {m.discountCode && <> · Rabattcode: {m.discountCode}</>}
                </p>
              ) : (
                <p>
                  Buchung stornieren: <span className="font-semibold">{m.roomName}</span>
                  {", "}
                  {formatLocalDate(m.startDate)} – {formatLocalDate(m.endDate)}
                </p>
              )}

              {m.status === "pending" && (
                <div className="flex gap-2">
                  <Button
                    variant={isBooking ? "primary" : "danger"}
                    className="text-xs py-1.5 px-3"
                    onClick={() => (isBooking ? confirmBooking(m) : confirmCancellation(m))}
                  >
                    {isBooking ? "Buchen" : "Stornieren"}
                  </Button>
                  <Button
                    variant="secondary"
                    className="text-xs py-1.5 px-3"
                    onClick={() => updateMessage(m.id, { status: "declined" })}
                  >
                    Abbrechen
                  </Button>
                </div>
              )}

              {m.status === "processing" && <p className="text-text-muted">Wird verarbeitet...</p>}
              {m.status === "declined" && <p className="text-text-muted">Abgebrochen.</p>}
              {m.status === "confirmed" && <p className="text-success font-medium">{m.resultText}</p>}
              {m.status === "error" && <p className="text-danger font-medium">{m.resultText}</p>}
            </div>
          );
        })}

        {sending && (
          <div className="self-start px-3.5 py-2.5 rounded-2xl rounded-bl-sm bg-black/[0.04] text-sm text-text-muted">
            Denkt nach...
          </div>
        )}
      </div>

      {error && (
        <div className="px-4 pt-2">
          <Alert variant="danger">{error}</Alert>
        </div>
      )}

      <form onSubmit={handleSubmit} className="flex items-end gap-2 p-4 border-t border-border-subtle">
        <div className="flex-1">
          <TextInput
            label="Deine Frage"
            placeholder="z. B. wie viele Buchungen du hast..."
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            disabled={sending}
          />
        </div>
        <Button type="submit" disabled={sending || !question.trim()}>
          {sending ? "..." : "Senden"}
        </Button>
      </form>
    </div>
  );
}
