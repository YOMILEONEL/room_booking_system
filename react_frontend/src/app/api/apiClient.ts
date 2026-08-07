import { getSession } from "next-auth/react";
import { BACKEND_BASE_URL } from "./http";

export type ApiFetchResponseType = "json" | "text";

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

  const finalHeaders: Record<string, string> = {
    "Content-Type": "application/json",
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    ...(headers ?? {}),
  };

  const res = await fetch(url, {
    ...rest,
    headers: finalHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
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

  const text = await res.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}
