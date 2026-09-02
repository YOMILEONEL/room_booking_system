import "server-only";

import {
  GoogleGenerativeAI,
  GoogleGenerativeAIFetchError,
  SchemaType,
  type Content,
  type FunctionDeclaration,
} from "@google/generative-ai";
import { fetchMyBookings, fetchRoomsOverview } from "./tools";

// "gemini-flash-latest" is Google's maintained alias, not a dated model name like
// "gemini-2.5-flash" - the CVio project (docs/ai-agent.md there) hit exactly this problem:
// a pinned dated name got silently cut off from new API keys. The alias moves that risk to
// Google instead of breaking this feature on every model rotation.
const MODEL_NAME = "gemini-flash-latest";

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
// actual mutation only happens if the person explicitly confirms it there - Gemini never gets
// to actually create or cancel a booking on its own.
const toolDeclarations: FunctionDeclaration[] = [
  {
    name: "get_my_bookings",
    description:
      "Liefert alle Buchungen der aktuell angemeldeten Person (Kunde oder Organisation): " +
      "Anzahl und Details je Buchung (bookingId, Raumname, Zeitraum, Zahlungsstatus). Nutze " +
      "dieses Tool auch, um vor einer Stornierung die passende bookingId zu ermitteln.",
    parameters: { type: SchemaType.OBJECT, properties: {} },
  },
  {
    name: "list_rooms",
    description:
      "Liefert alle aktiven Räume mit Preis pro Tag (inkl. eines eventuellen " +
      "Organisationsrabatts der anfragenden Person), Stadt, Kapazität und ob der Raum " +
      "aktuell verfügbar ist (nicht durch eine laufende Buchung belegt).",
    parameters: { type: SchemaType.OBJECT, properties: {} },
  },
  {
    name: "create_booking",
    description:
      "Bereitet eine neue Buchung vor. Erstellt NOCH KEINE echte Buchung - die Person muss den " +
      "Vorschlag danach in der Oberfläche noch bestätigen. Rufe dieses Tool erst auf, wenn " +
      "Raumname und Zeitraum aus dem Gespräch eindeutig hervorgehen (nutze list_rooms, um den " +
      "exakten Raumnamen zu bestätigen, falls unsicher).",
    parameters: {
      type: SchemaType.OBJECT,
      properties: {
        roomName: { type: SchemaType.STRING, description: "Exakter Raumname, wie von list_rooms geliefert." },
        startDate: { type: SchemaType.STRING, description: "Startdatum im Format JJJJ-MM-TT." },
        endDate: { type: SchemaType.STRING, description: "Enddatum im Format JJJJ-MM-TT." },
        discountCode: { type: SchemaType.STRING, description: "Optionaler Rabattcode, falls genannt." },
      },
      required: ["roomName", "startDate", "endDate"],
    },
  },
  {
    name: "cancel_booking",
    description:
      "Bereitet die Stornierung einer bestehenden Buchung vor. Storniert NOCH NICHTS - die " +
      "Person muss das danach in der Oberfläche noch bestätigen. Ermittle die bookingId vorher " +
      "über get_my_bookings, niemals raten.",
    parameters: {
      type: SchemaType.OBJECT,
      properties: {
        bookingId: { type: SchemaType.STRING, description: "Die bookingId aus get_my_bookings." },
      },
      required: ["bookingId"],
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
  "konkret und auf Deutsch. Preise sind in Euro pro Tag.";

// Runs the model, and if it asks for a tool call, executes it (server-side, scoped to the
// caller's own accessToken) and feeds the result back - up to a few rounds, in case the model
// chains two tool calls (e.g. bookings, then rooms) before it has enough to answer. A
// create_booking/cancel_booking call short-circuits this immediately (see the "terminal" branch
// below) instead of looping further - Gemini's role stops at proposing the action.
export async function askAssistant(question: string, accessToken: string): Promise<AssistantResult> {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new AssistantError(
      "Der KI-Assistent ist auf diesem Server nicht konfiguriert.",
      "missing_api_key"
    );
  }

  const genAI = new GoogleGenerativeAI(apiKey);
  const model = genAI.getGenerativeModel({
    model: MODEL_NAME,
    systemInstruction: SYSTEM_INSTRUCTION,
    tools: [{ functionDeclarations: toolDeclarations }],
  });

  // Built and driven manually via generateContent(), not model.startChat()/sendMessage():
  // the SDK's ChatSession hardcodes role "function" for a functionResponse turn (see
  // node_modules/@google/generative-ai/dist/index.js, formatNewContent), but the Gemini API
  // backend currently in service rejects that with "400 Role 'function' is not supported" -
  // it now expects the function response back as a "user" turn instead (confirmed live against
  // the real API). Managing `contents` by hand sidesteps the SDK's fixed role choice.
  const contents: Content[] = [{ role: "user", parts: [{ text: question }] }];

  try {
    let result = await model.generateContent({ contents });

    for (let round = 0; round < 3; round++) {
      const calls = result.response.functionCalls();
      if (!calls || calls.length === 0) {
        break;
      }

      const executions = await Promise.all(
        calls.map(async (call) => ({
          call,
          execution: await executeTool(call.name, (call.args ?? {}) as Record<string, unknown>, accessToken),
        }))
      );

      // create_booking/cancel_booking end the loop right here - the model never sees a
      // functionResponse for these, because there's nothing further for it to reason about
      // until the person has actually confirmed the action in the UI.
      const terminal = executions.find((e) => e.execution.kind === "terminal");
      if (terminal && terminal.execution.kind === "terminal") {
        return terminal.execution.result;
      }

      const modelParts = result.response.candidates?.[0]?.content?.parts;
      contents.push({ role: "model", parts: modelParts ?? [] });

      const functionResponseParts = executions.map(({ call, execution }) => ({
        functionResponse: {
          name: call.name,
          response: execution.kind === "data" ? execution.data : {},
        },
      }));
      contents.push({ role: "user", parts: functionResponseParts });

      result = await model.generateContent({ contents });
    }

    return { type: "text", text: result.response.text() };
  } catch (error) {
    console.error("[assistant] Gemini-Anfrage fehlgeschlagen:", error);

    if (error instanceof GoogleGenerativeAIFetchError) {
      if (error.status === 429) {
        throw new AssistantError(
          "Das kostenlose Tageskontingent des KI-Assistenten ist aufgebraucht. Bitte später erneut versuchen.",
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
