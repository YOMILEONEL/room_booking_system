"use client";

import BookingTable from "../../components/BookingTable";

export default function AdminBookingsPage() {
  return (
    <div>
      <h1 className="text-xl font-bold mb-4">Buchungen</h1>
      <BookingTable />
    </div>
  );
}
