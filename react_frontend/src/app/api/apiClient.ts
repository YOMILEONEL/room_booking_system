import { getSession } from "next-auth/react";
import { BACKEND_BASE_URL } from "./http";

export type ApiFetchResponseType = "json" | "text" | "blob";

type ApiFetchOptions = Omit<RequestInit, "headers" | "body"> & {
  auth?: boolean;
  body?: unknown;
  headers?: Record<string, string>;
  responseType?: ApiFetchResponseType;
};

export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions = {}
): Promise<T> {
  const { auth = true, responseType = "json", headers, body, ...rest } = options;

  const url = path.startsWith("http") ? path : `${BACKEND_BASE_URL}${path}`;

  let accessToken: string | undefined;
  if (auth) {
    const session = await getSession();
    accessToken = session?.accessToken;
  }

  const isFormData = body instanceof FormData;

  const finalHeaders: Record<string, string> = {
    // Omitted for FormData - the browser sets its own multipart boundary in Content-Type.
    ...(isFormData ? {} : { "Content-Type": "application/json" }),
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    ...(headers ?? {}),
  };

  const res = await fetch(url, {
    ...rest,
    headers: finalHeaders,
    body: body === undefined ? undefined : isFormData ? (body as FormData) : JSON.stringify(body),
  });

  if (!res.ok) {
    let details = "";
    try {
      details = await res.text();
    } catch {
      // ignore
    }
    throw new Error(
      details?.trim() ? `${res.status} ${res.statusText} – ${details}` : `${res.status} ${res.statusText}`
    );
  }

  if (res.status === 204) {
    return undefined as T;
  }

  if (responseType === "text") {
    return (await res.text()) as unknown as T;
  }

  if (responseType === "blob") {
    return (await res.blob()) as unknown as T;
  }

  const text = await res.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}

// apiFetch's thrown Error buries the backend's JSON error body (e.g. {"error": "..."}) inside
// its message as "<status> <statusText> – <body>". This pulls the actual "error" field back out
// so callers can show the specific backend reason instead of a generic fallback message.
export function extractErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof Error) {
    const match = err.message.match(/–\s*(\{.*\})\s*$/);
    if (match) {
      try {
        const parsed = JSON.parse(match[1]);
        if (typeof parsed?.error === "string") {
          return parsed.error;
        }
      } catch {
        // not JSON - fall through to the fallback
      }
    }
  }
  return fallback;
}
