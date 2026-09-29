import { apiFetch } from "./apiClient";

const BASE = "/payment";

export type PaymentStatus = "PENDING" | "PAID";

export type Payment = {
  id: string;
  amount: number;
  status: PaymentStatus;
  appliedDiscountCode: string | null;
};

// Technical admin-only override, no longer wired into the UI - see PaymentController.confirmPayment.
export async function confirmPayment(paymentId: string): Promise<void> {
  await apiFetch<void>(`${BASE}/${paymentId}/confirm`, { method: "PUT" });
}

// Starts the real payment flow: creates a Stripe Checkout Session and returns its hosted-page
// URL - the caller redirects the browser there (window.location.href = url). The booking only
// actually becomes PAID once Stripe's webhook confirms the payment, not from this call itself.
export async function createCheckoutSession(paymentId: string): Promise<{ url: string }> {
  return apiFetch<{ url: string }>(`${BASE}/${paymentId}/checkout-session`, { method: "POST" });
}
