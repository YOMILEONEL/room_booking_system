"use client";

import * as React from "react";
import { askAssistant } from "../api/assistant.api";
import { Button, TextInput, Alert } from "./ui";
import MarkdownLite from "./MarkdownLite";

type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  text: string;
};

const EXAMPLE_QUESTIONS = [
  "Wie viele Buchungen habe ich?",
  "Was ist der günstigste Raum?",
  "Was ist der günstigste Raum, der gerade verfügbar ist?",
];

// Shared by the full-page /assistant view and the site-wide floating AssistantWidget - only
// the surrounding chrome (page shell vs. floating panel) differs between the two.
export default function AssistantChat({ compact = false }: { compact?: boolean }) {
  const [messages, setMessages] = React.useState<ChatMessage[]>([]);
  const [question, setQuestion] = React.useState("");
  const [sending, setSending] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const scrollRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages]);

  const sendQuestion = async (text: string) => {
    const trimmed = text.trim();
    if (!trimmed || sending) return;

    setError(null);
    setMessages((prev) => [...prev, { id: crypto.randomUUID(), role: "user", text: trimmed }]);
    setQuestion("");
    setSending(true);

    try {
      const answer = await askAssistant(trimmed);
      setMessages((prev) => [...prev, { id: crypto.randomUUID(), role: "assistant", text: answer }]);
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

        {messages.map((m) => (
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
        ))}

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
