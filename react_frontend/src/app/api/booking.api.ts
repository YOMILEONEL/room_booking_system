import { apiFetch } from "./apiClient";
import type { Payment } from "./payment.api";
import type { Room } from "./room.api";
import type { User } from "./user.api";

const BASE = "/booking";

export type Booking = {
  bookingId: string;
  room: Room | null;
  user: User | null;
  startTime: string;
  endTime: string;
  payment: Payment | null;
};

export type CreateBookingRequest = {
  roomId: string;
  userId: string;
  startTime: string;
  endTime: string;
  discountCode?: string;
};

export type AdminBookingRequest = {
  roomId: string;
  customerEmail: string;
  startTime: string;
  endTime: string;
  discountCode?: string;
};

// The backend derives "which user" from the authenticated session, not from the URL - the old
// {userId} path segment was accepted but ignored there (see docs/code-review.md, section 3).
export async function fetchBookings(): Promise<Booking[]> {
  const data = await apiFetch<Booking[] | null>(`${BASE}/getAll`);
  return Array.isArray(data) ? data : [];
}

export async function createBooking(payload: CreateBookingRequest): Promise<Booking> {
  return apiFetch<Booking>(`${BASE}/add`, { method: "POST", body: payload });
}

export async function createBookingForCustomer(payload: AdminBookingRequest): Promise<Booking> {
  return apiFetch<Booking>(`${BASE}/admin-add`, { method: "POST", body: payload });
}

export async function deleteBooking(bookingId: string): Promise<void> {
  await apiFetch<void>(`${BASE}/delete/${bookingId}`, { method: "DELETE" });
}
