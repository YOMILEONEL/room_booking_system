import "server-only";

import { BACKEND_INTERNAL_URL } from "../../api/http";

export type AssistantBooking = {
  roomName: string;
  startTime: string;
  endTime: string;
  paymentStatus: "PENDING" | "PAID" | null;
};

export type AssistantRoom = {
  name: string;
  city: string;
  capacity: number;
  pricePerDay: number;
  effectivePricePerDay: number;
  available: boolean;
};

async function backendGet<T>(path: string, accessToken: string): Promise<T> {
  const res = await fetch(`${BACKEND_INTERNAL_URL}${path}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error(`Backend-Aufruf ${path} schlug fehl: ${res.status}`);
  }
  return (await res.json()) as T;
}

// Same endpoint the "Meine Buchungen" page already uses (GET /booking/getAll) - the backend
// scopes it to the caller's own bookings for a MEMBER token, so no extra userId filtering is
// needed here. Only lean, chat-relevant fields are extracted - not the raw nested Room/User/
// Payment objects - to keep the tool response small and unambiguous for the model.
export async function fetchMyBookings(accessToken: string): Promise<AssistantBooking[]> {
  type RawBooking = {
    room: { name: string } | null;
    startTime: string;
    endTime: string;
    payment: { status: "PENDING" | "PAID" } | null;
  };
  const bookings = await backendGet<RawBooking[]>("/booking/getAll", accessToken);
  return bookings.map((b) => ({
    roomName: b.room?.name ?? "Unbekannter Raum",
    startTime: b.startTime,
    endTime: b.endTime,
    paymentStatus: b.payment?.status ?? null,
  }));
}

// Mirrors the availability logic RoomTable.tsx already shows to the user: a room counts as
// "available" only if it isn't blocked by a booking covering today (bookedUntil) AND its
// stored roomStatus is VERFUGBAR - relying on roomStatus alone would miss rooms that are
// mid-booking right now but not yet flipped to GEBUCHT by an admin.
export async function fetchRoomsOverview(accessToken: string): Promise<AssistantRoom[]> {
  type RawRoom = {
    name: string;
    city: string;
    capacity: number;
    pricePerDay: number;
    effectivePricePerDay?: number;
    roomStatus: "VERFUGBAR" | "GEBUCHT";
    active?: boolean;
    bookedUntil?: string | null;
  };
  const rooms = await backendGet<RawRoom[]>("/room/Get", accessToken);
  return rooms
    .filter((r) => r.active !== false)
    .map((r) => ({
      name: r.name,
      city: r.city,
      capacity: r.capacity,
      pricePerDay: r.pricePerDay,
      effectivePricePerDay: r.effectivePricePerDay ?? r.pricePerDay,
      available: !r.bookedUntil && r.roomStatus === "VERFUGBAR",
    }));
}
