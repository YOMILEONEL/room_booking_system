import { apiFetch } from "./apiClient";
import type { Booking } from "./booking.api";

const BASE = "/admin";

export type RoomBookingCount = {
  roomName: string;
  bookingCount: number;
};

export type CustomerBookingCount = {
  username: string;
  bookingCount: number;
};

export type AdminDashboard = {
  availableRooms: number;
  bookedRooms: number;
  totalUsers: number;
  upcomingBookings: number;
  revenueThisMonth: number;
  recentBookings: Booking[];
  pendingPayments: Booking[];
  topRooms: RoomBookingCount[];
  topCustomers: CustomerBookingCount[];
};

export async function fetchAdminDashboard(): Promise<AdminDashboard> {
  return apiFetch<AdminDashboard>(`${BASE}/dashboard`);
}
