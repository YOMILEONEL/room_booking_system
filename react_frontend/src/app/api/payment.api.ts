import { apiFetch } from "./apiClient";

const BASE = "/payment";

export type PaymentStatus = "PENDING" | "PAID";

export type Payment = {
  id: string;
  amount: number;
  status: PaymentStatus;
  appliedDiscountCode: string | null;
};

export async function confirmPayment(paymentId: string): Promise<void> {
  await apiFetch<void>(`${BASE}/${paymentId}/confirm`, { method: "PUT" });
}
