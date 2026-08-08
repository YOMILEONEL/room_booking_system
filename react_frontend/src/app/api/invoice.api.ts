import { apiFetch } from "./apiClient";

const BASE = "/invoice";

export async function fetchInvoicePdf(bookingId: string): Promise<Blob> {
  return apiFetch<Blob>(`${BASE}/booking/${bookingId}/pdf`, { responseType: "blob" });
}
