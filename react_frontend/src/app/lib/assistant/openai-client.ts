import "server-only";

import OpenAI, { APIError } from "openai";
import type { ChatCompletionMessageParam, ChatCompletionTool } from "openai/resources/chat/completions";
import { fetchMyBookings, fetchRoomsOverview } from "./tools";

// gpt-5-nano is OpenAI's cheapest chat model (as of 2026-09: $0.05 / 1M input tokens,
// $0.40 / 1M output tokens) - plenty for this tool-calling assistant, which mostly relays
// short, structured data rather than doing heavy reasoning.
const MODEL_NAME = "gpt-5-nano";

// Every request gets an explicit timeout: a stalled upstream connection previously hung this
// feature for 5+ minutes with no error and no way for the UI to recover (see docs/ai-agent.md).
const REQUEST_TIMEOUT_MS = 20_000;

export type AssistantErrorCode =
  | "missing_api_key"
  | "app_quota_exceeded"
  | "invalid_api_key"
  | "request_failed";

export class AssistantError extends Error {
  code: AssistantErrorCode;

  constructor(message: string, code: AssistantErrorCode, options?: ErrorOptions) {
    super(message, options);
    this.code = code;
  }
}

// Read tools stay deliberately generic rather than one narrow function per question ("get
// cheapest room", "get cheapest available room", ...): list_rooms returns the full room list
// with price and availability, and the model itself picks the cheapest/cheapest-available/
// whatever variant is actually asked. That covers question rewordings ("unter 50 Euro",
// "zweitguenstigster Raum", ...) without a new tool per phrasing.
//
// create_booking/cancel_booking are different in kind, not just in name: they don't return data
// for the model to keep reasoning with, they END the tool-calling loop (see the "terminal"
// branch in executeTool/askAssistant below) and hand a proposed action back to the UI. The
// actual mutation only happens if the person explicitly confirms it there - the model never
// gets to actually create or cancel a booking on its own.
const tools: ChatCompletionTool[] = [
  {
    type: "function",
    function: {
      name: "get_my_bookings",
      description:
        "Liefert alle Buchungen der aktuell angemeldeten Person (Kunde oder Organisation): " +
        "Anzahl und Details je Buchung (bookingId, Raumname, Zeitraum, Zahlungsstatus). Nutze " +
        "dieses Tool auch, um vor einer Stornierung die passende bookingId zu ermitteln.",
      parameters: { type: "object", properties: {} },
    },
  },
  {
    type: "function",
    function: {
      name: "list_rooms",
      description:
        "Liefert alle aktiven Räume mit Preis pro Tag (inkl. eines eventuellen " +
        "Organisationsrabatts der anfragenden Person), Stadt, Kapazität und ob der Raum " +
        "aktuell verfügbar ist (nicht durch eine laufende Buchung belegt).",
      parameters: { type: "object", properties: {} },
    },
  },
  {
    type: "function",
    function: {
      name: "create_booking",
      description:
        "Bereitet eine neue Buchung vor. Erstellt NOCH KEINE echte Buchung - die Person muss den " +
        "Vorschlag danach in der Oberfläche noch bestätigen. Rufe dieses Tool erst auf, wenn " +
        "Raumname und Zeitraum aus dem Gespräch eindeutig hervorgehen (nutze list_rooms, um den " +
        "exakten Raumnamen zu bestätigen, falls unsicher).",
      parameters: {
        type: "object",
        properties: {
          roomName: { type: "string", description: "Exakter Raumname, wie von list_rooms geliefert." },
          startDate: { type: "string", description: "Startdatum im Format JJJJ-MM-TT." },
          endDate: { type: "string", description: "Enddatum im Format JJJJ-MM-TT." },
          discountCode: { type: "string", description: "Optionaler Rabattcode, falls genannt." },
        },
        required: ["roomName", "startDate", "endDate"],
      },
    },
  },
  {
    type: "function",
    function: {
      name: "cancel_booking",
      description:
        "Bereitet die Stornierung einer bestehenden Buchung vor. Storniert NOCH NICHTS - die " +
        "Person muss das danach in der Oberfläche noch bestätigen. Ermittle die bookingId vorher " +
        "über get_my_bookings, niemals raten.",
      parameters: {
        type: "object",
        properties: {
          bookingId: { type: "string", description: "Die bookingId aus get_my_bookings." },
        },
        required: ["bookingId"],
      },
    },
  },
];

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

type ToolExecution = { kind: "data"; data: object } | { kind: "terminal"; result: AssistantResult };

async function executeTool(
  name: string,
  args: Record<string, unknown>,
  accessToken: string
): Promise<ToolExecution> {
  if (name === "get_my_bookings") {
    const bookings = await fetchMyBookings(accessToken);
    return { kind: "data", data: { count: bookings.length, bookings } };
  }

  if (name === "list_rooms") {
    return { kind: "data", data: { rooms: await fetchRoomsOverview(accessToken) } };
  }

  if (name === "create_booking") {
    const roomName = String(args.roomName ?? "").trim();
    const startDate = String(args.startDate ?? "").trim();
    const endDate = String(args.endDate ?? "").trim();
    const discountCode =
      typeof args.discountCode === "string" && args.discountCode.trim() ? args.discountCode.trim() : undefined;

    const rooms = await fetchRoomsOverview(accessToken);
    const room = rooms.find((r) => r.name.toLowerCase() === roomName.toLowerCase());
    if (!room) {
      return {
        kind: "data",
        data: { error: `Raum "${roomName}" wurde nicht gefunden. Bitte den exakten Namen aus list_rooms verwenden.` },
      };
    }

    return {
      kind: "terminal",
      result: {
        type: "pending_booking",
        roomId: room.id,
        roomName: room.name,
        startDate,
        endDate,
        pricePerDay: room.effectivePricePerDay,
        discountCode,
      },
    };
  }

  if (name === "cancel_booking") {
    const bookingId = String(args.bookingId ?? "").trim();
    const bookings = await fetchMyBookings(accessToken);
    const booking = bookings.find((b) => b.bookingId === bookingId);
    if (!booking) {
      return {
        kind: "data",
        data: { error: "Buchung nicht gefunden. Bitte zuerst get_my_bookings aufrufen, um die richtige bookingId zu ermitteln." },
      };
    }

    return {
      kind: "terminal",
      result: {
        type: "pending_cancellation",
        bookingId: booking.bookingId,
        roomName: booking.roomName,
        startDate: booking.startTime,
        endDate: booking.endTime,
      },
    };
  }

  throw new Error(`Unbekanntes Tool: ${name}`);
}

const SYSTEM_INSTRUCTION =
  "Du bist der KI-Assistent von Spacio, einer Raumbuchungs-Plattform. Du hilfst eingeloggten " +
  "Kunden und Organisationen, Fragen zu ihren eigenen Buchungen und zu den verfügbaren Räumen " +
  "zu beantworten, und kannst Buchungen für sie vorbereiten oder stornieren. Nutze IMMER die " +
  "bereitgestellten Funktionen, um an echte, aktuelle Daten zu kommen - erfinde niemals " +
  "Buchungszahlen, Raumnamen oder Preise. Bevor du create_booking oder cancel_booking aufrufst, " +
  "fasse kurz zusammen, was du vorschlägst - die endgültige Bestätigung holt sich die Oberfläche " +
  "danach separat von der Person ein, du musst nicht selbst nochmal nachfragen. Antworte kurz, " +
  "konkret und auf Deutsch. Preise sind in Euro pro Tag.\n\n" +
  "Formatierung der Antworten: Schreib in normalen, kurzen Sätzen oder als einfache Aufzählung " +
  "mit '-', nie mit '|' getrennt und nie als rohe Tabelle. Nenne bei Buchungen Raum, Zeitraum " +
  "und Zahlungsstatus in Worten (z. B. 'Hochzeit-Raum, 08.08. bis 10.08., bezahlt') - die " +
  "technische bookingId (die lange Zeichenfolge mit Bindestrichen) NIE in der Antwort zeigen, " +
  "sie ist nur intern für dich zum Zuordnen bei einer Stornierung gedacht. Wenn mehrere " +
  "Buchungen zur gleichen Beschreibung passen (z. B. gleicher Raum, unterschiedliche Termine), " +
  "unterscheide sie über den Zeitraum, nicht über eine ID. Keine Markdown-Formatierung " +
  "(kein **fett**, keine #Überschriften).";

// Runs the model, and if it asks for a tool call, executes it (server-side, scoped to the
// caller's own accessToken) and feeds the result back - up to a few rounds, in case the model
// chains two tool calls (e.g. bookings, then rooms) before it has enough to answer. A
// create_booking/cancel_booking call short-circuits this immediately (see the "terminal" branch
// below) instead of looping further - the model's role stops at proposing the action.
export async function askAssistant(question: string, accessToken: string): Promise<AssistantResult> {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    throw new AssistantError(
      "Der KI-Assistent ist auf diesem Server nicht konfiguriert.",
      "missing_api_key"
    );
  }

  const client = new OpenAI({ apiKey, timeout: REQUEST_TIMEOUT_MS, maxRetries: 1 });

  const messages: ChatCompletionMessageParam[] = [
    { role: "system", content: SYSTEM_INSTRUCTION },
    { role: "user", content: question },
  ];

  try {
    let response = await client.chat.completions.create({
      model: MODEL_NAME,
      messages,
      tools,
    });

    for (let round = 0; round < 3; round++) {
      const choice = response.choices[0];
      const toolCalls = choice.message.tool_calls;
      if (!toolCalls || toolCalls.length === 0) {
        break;
      }

      messages.push(choice.message);

      // Only "function" tools are declared above, so any "custom" tool call would be a
      // model error - narrow it away rather than crashing on a missing `.function` field.
      const functionCalls = toolCalls.filter((toolCall) => toolCall.type === "function");

      const executions = await Promise.all(
        functionCalls.map(async (toolCall) => ({
          toolCall,
          execution: await executeTool(
            toolCall.function.name,
            JSON.parse(toolCall.function.arguments || "{}") as Record<string, unknown>,
            accessToken
          ),
        }))
      );

      // create_booking/cancel_booking end the loop right here - the model never sees a tool
      // result for these, because there's nothing further for it to reason about until the
      // person has actually confirmed the action in the UI.
      const terminal = executions.find((e) => e.execution.kind === "terminal");
      if (terminal && terminal.execution.kind === "terminal") {
        return terminal.execution.result;
      }

      for (const { toolCall, execution } of executions) {
        messages.push({
          role: "tool",
          tool_call_id: toolCall.id,
          content: JSON.stringify(execution.kind === "data" ? execution.data : {}),
        });
      }

      response = await client.chat.completions.create({
        model: MODEL_NAME,
        messages,
        tools,
      });
    }

    return { type: "text", text: response.choices[0].message.content ?? "" };
  } catch (error) {
    console.error("[assistant] OpenAI-Anfrage fehlgeschlagen:", error);

    if (error instanceof APIError) {
      if (error.status === 429) {
        throw new AssistantError(
          "Das Guthaben oder Tageskontingent des KI-Assistenten ist aufgebraucht. Bitte später erneut versuchen.",
          "app_quota_exceeded",
          { cause: error }
        );
      }
      if (error.status === 401 || error.status === 403) {
        throw new AssistantError(
          "Der KI-Assistent ist falsch konfiguriert (ungültiger API-Key).",
          "invalid_api_key",
          { cause: error }
        );
      }
    }

    if (error instanceof AssistantError) {
      throw error;
    }

    throw new AssistantError(
      "Der KI-Assistent konnte gerade nicht antworten.",
      "request_failed",
      { cause: error }
    );
  }
}
