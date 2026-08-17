import "server-only";

import { GoogleGenerativeAI, GoogleGenerativeAIFetchError, SchemaType, type Content } from "@google/generative-ai";
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

// Two tools, deliberately generic rather than one narrow function per question ("get cheapest
// room", "get cheapest available room", ...): list_rooms returns the full room list with price
// and availability, and the model itself picks the cheapest/cheapest-available/whatever variant
// is actually asked. That covers question rewordings ("unter 50 Euro", "zweitguenstigster
// Raum", ...) without a new tool per phrasing.
const toolDeclarations = [
  {
    name: "get_my_bookings",
    description:
      "Liefert alle Buchungen der aktuell angemeldeten Person (Kunde oder Organisation): " +
      "Anzahl und Details je Buchung (Raumname, Zeitraum, Zahlungsstatus).",
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
];

async function executeTool(name: string, accessToken: string): Promise<object> {
  if (name === "get_my_bookings") {
    const bookings = await fetchMyBookings(accessToken);
    return { count: bookings.length, bookings };
  }
  if (name === "list_rooms") {
    return { rooms: await fetchRoomsOverview(accessToken) };
  }
  throw new Error(`Unbekanntes Tool: ${name}`);
}

const SYSTEM_INSTRUCTION =
  "Du bist der KI-Assistent von Spacio, einer Raumbuchungs-Plattform. Du beantwortest " +
  "Fragen von eingeloggten Kunden und Organisationen zu ihren eigenen Buchungen und zu den " +
  "verfügbaren Räumen. Nutze IMMER die bereitgestellten Funktionen, um an echte, aktuelle " +
  "Daten zu kommen - erfinde niemals Buchungszahlen, Raumnamen oder Preise. Antworte kurz, " +
  "konkret und auf Deutsch. Preise sind in Euro pro Tag.";

// Runs the model, and if it asks for a tool call, executes it (server-side, scoped to the
// caller's own accessToken) and feeds the result back - up to a few rounds, in case the model
// chains two tool calls (e.g. bookings, then rooms) before it has enough to answer.
export async function askAssistant(question: string, accessToken: string): Promise<string> {
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

      const modelParts = result.response.candidates?.[0]?.content?.parts;
      contents.push({ role: "model", parts: modelParts ?? [] });

      const functionResponseParts = await Promise.all(
        calls.map(async (call) => ({
          functionResponse: {
            name: call.name,
            response: await executeTool(call.name, accessToken),
          },
        }))
      );
      contents.push({ role: "user", parts: functionResponseParts });

      result = await model.generateContent({ contents });
    }

    return result.response.text();
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
