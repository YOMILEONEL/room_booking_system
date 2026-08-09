import { apiFetch } from "./apiClient";

const BASE = "/api";

export type CustomerType = "ORGANISATION" | "KUNDE";

export type RegisterRequest = {
  email: string;
  password: string;
  customerType: CustomerType;
  organisationName?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber: string;
};

export async function registerUser(payload: RegisterRequest): Promise<void> {
  await apiFetch<void>(`${BASE}/register`, {
    method: "POST",
    body: payload,
    auth: false,
  });
}

export async function requestPasswordReset(email: string): Promise<void> {
  await apiFetch<void>(`${BASE}/forgot-password`, {
    method: "POST",
    body: { email },
    auth: false,
    responseType: "text",
  });
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  await apiFetch<void>(`${BASE}/reset-password`, {
    method: "POST",
    body: { token, newPassword },
    auth: false,
  });
}
