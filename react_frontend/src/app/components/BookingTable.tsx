"use client";

import React, { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { fetchBookingsForUser, deleteBooking, type Booking } from "../api/booking.api";
import { confirmPayment } from "../api/payment.api";
import { fetchInvoicePdf } from "../api/invoice.api";
import { extractErrorMessage } from "../api/apiClient";
import { roomImages, defaultRoomImage } from "../lib/roomImages";
import { Card, Badge, Button, Alert } from "./ui";

const currency = new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" });

const BookingTable: React.FC = () => {
  const { data: session } = useSession();
  const userId = session?.user?.id ?? null;
  const isAdmin = session?.user?.role === "ADMIN";

  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmingId, setConfirmingId] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  const getBookingStatus = (
    start: string,
    end: string
  ): "Zukünftig" | "Laufend" | "Abgelaufen" => {
    const today = new Date();
    const s = new Date(start);
    const e = new Date(end);

    s.setHours(0, 0, 0, 0);
    e.setHours(23, 59, 59, 999);
    today.setHours(12, 0, 0, 0);

    if (today < s) return "Zukünftig";
    if (today > e) return "Abgelaufen";
    return "Laufend";
  };

  const loadBookings = async () => {
    if (!userId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await fetchBookingsForUser(userId);
      setBookings(data);
    } catch (err) {
      console.error(err);
      setError("Buchungen konnten nicht geladen werden.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBookings();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  const handleDeleteBooking = async (bookingId: string) => {
    if (!userId) return;

    const confirmed = window.confirm(`Buchung mit ID ${bookingId} wirklich löschen?`);
    if (!confirmed) return;

    try {
      await deleteBooking(bookingId, userId);
      setBookings((prev) => prev.filter((b) => b.bookingId !== bookingId));
    } catch (err) {
      console.error(err);
      alert(extractErrorMessage(err, "Fehler beim Löschen"));
    }
  };

  const handleDownloadInvoice = async (booking: Booking) => {
    setDownloadingId(booking.bookingId);
    try {
      const blob = await fetchInvoicePdf(booking.bookingId);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `Rechnung-${booking.room?.name ?? booking.bookingId}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      alert(extractErrorMessage(err, "Rechnung konnte nicht heruntergeladen werden."));
    } finally {
      setDownloadingId(null);
    }
  };

  const handleConfirmPayment = async (paymentId: string) => {
    setConfirmingId(paymentId);
    try {
      await confirmPayment(paymentId);
      await loadBookings();
    } catch (err) {
      console.error(err);
      alert("Zahlung konnte nicht bestätigt werden.");
    } finally {
      setConfirmingId(null);
    }
  };

  if (loading) return <p className="text-text-muted text-sm">Lädt...</p>;
  if (error) return <Alert variant="danger">{error}</Alert>;
  if (bookings.length === 0)
    return <p className="text-text-muted text-sm">Keine Buchungen vorhanden.</p>;

  return (
    <div>
      <h2 className="text-lg font-bold mb-4">
        {isAdmin ? "Alle Buchungen" : "Meine Buchungen"}
      </h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {bookings.map((booking) => {
          const { room, user, startTime, endTime, payment } = booking;
          const status = getBookingStatus(startTime, endTime);
          const canDelete = status !== "Laufend" && payment?.status !== "PAID";
          const imageSrc = room?.imageUrl ?? (room?.name ? roomImages[room.name] : undefined) ?? defaultRoomImage;

          return (
            <Card key={booking.bookingId} className="p-0 overflow-hidden flex flex-col">
              <img src={imageSrc} alt={room?.name} className="w-full h-36 object-cover" />

              <div className="p-4 flex-1 flex flex-col gap-2">
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold">{room?.name}</h3>
                  {canDelete && (
                    <button
                      onClick={() => handleDeleteBooking(booking.bookingId)}
                      className="text-xs font-medium text-danger hover:text-danger/80 transition-colors"
                    >
                      Löschen
                    </button>
                  )}
                </div>

                <p className="text-sm text-text-secondary">Standort: {room?.location}</p>
                {isAdmin && (
                  <p className="text-sm text-text-secondary">
                    Von: {user?.username} ({user?.role})
                  </p>
                )}
                <p className="text-sm text-text-secondary">
                  Zeitraum: {startTime} – {endTime}
                </p>

                <div className="flex flex-wrap items-center gap-2 mt-1">
                  <Badge
                    variant={
                      status === "Laufend" ? "success" : status === "Zukünftig" ? "info" : "neutral"
                    }
                  >
                    {status}
                  </Badge>

                  {payment && (
                    <Badge variant={payment.status === "PAID" ? "paid" : "pending"}>
                      {payment.status === "PAID" ? "Bezahlt" : "Zahlung offen"}
                    </Badge>
                  )}
                </div>

                {payment && (
                  <div className="mt-1 text-sm">
                    <p className="font-semibold text-primary">{currency.format(payment.amount)}</p>
                    {payment.appliedDiscountCode && (
                      <p className="text-text-muted text-xs">
                        Rabattcode: {payment.appliedDiscountCode}
                      </p>
                    )}
                  </div>
                )}

                {payment && payment.status === "PAID" && (
                  <Button
                    variant="secondary"
                    className="mt-2 text-xs py-2"
                    disabled={downloadingId === booking.bookingId}
                    onClick={() => handleDownloadInvoice(booking)}
                  >
                    {downloadingId === booking.bookingId ? "Wird geladen..." : "Rechnung herunterladen"}
                  </Button>
                )}

                {isAdmin && payment && payment.status === "PENDING" && (
                  <Button
                    variant="secondary"
                    className="mt-2 text-xs py-2"
                    disabled={confirmingId === payment.id}
                    onClick={() => handleConfirmPayment(payment.id)}
                  >
                    {confirmingId === payment.id ? "Wird bestätigt..." : "Zahlung bestätigen"}
                  </Button>
                )}
              </div>
            </Card>
          );
        })}
      </div>
    </div>
  );
};

export default BookingTable;
