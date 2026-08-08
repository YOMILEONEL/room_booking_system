"use client";

import React, { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { createBooking, createBookingForCustomer } from "../api/booking.api";
import { fetchRooms, type Room } from "../api/room.api";
import { extractErrorMessage } from "../api/apiClient";
import { Card, Button, TextInput, Select, Alert } from "./ui";

const currency = new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" });

interface BookingAddProps {
  fixedRoomId?: string;
  fixedRoomName?: string;
  onBooked?: () => void;
}

const BookingAdd: React.FC<BookingAddProps> = ({ fixedRoomId, fixedRoomName, onBooked }) => {
  const { data: session } = useSession();
  const isAdmin = session?.user?.role === "ADMIN";
  const isOrganisation = session?.user?.customerType === "ORGANISATION";

  const [rooms, setRooms] = useState<Room[]>([]);
  const [roomId, setRoomId] = useState<string>(fixedRoomId ?? "");
  const [customerEmail, setCustomerEmail] = useState<string>("");
  const [startTime, setStartTime] = useState<string>("");
  const [endTime, setEndTime] = useState<string>("");
  const [discountCode, setDiscountCode] = useState<string>("");

  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    if (fixedRoomId) return;

    fetchRooms()
      .then((data) => setRooms(data))
      .catch((err) => console.error("Fehler beim Laden der Räume:", err));
  }, [fixedRoomId]);

  const resetForm = () => {
    setRoomId(fixedRoomId ?? "");
    setCustomerEmail("");
    setStartTime("");
    setEndTime("");
    setDiscountCode("");
  };

  const handleAddBooking = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (isAdmin) {
      if (roomId === "" || !customerEmail.trim() || !startTime || !endTime) {
        setError("Bitte alle Felder ausfüllen.");
        return;
      }

      try {
        const result = await createBookingForCustomer({
          roomId,
          customerEmail: customerEmail.trim(),
          startTime,
          endTime,
          discountCode: discountCode.trim() || undefined,
        });

        const amount = result.payment?.amount;
        setSuccess(
          amount !== undefined
            ? `Buchung für ${customerEmail.trim()} wurde angelegt. Preis: ${currency.format(amount)}`
            : `Buchung für ${customerEmail.trim()} wurde angelegt.`
        );
        resetForm();
        onBooked?.();
      } catch (err) {
        console.error("Fehler beim Anlegen der Kundenbuchung:", err);
        setError(extractErrorMessage(err, "Buchung konnte nicht gespeichert werden (Fehler vom Server)."));
      }
      return;
    }

    const userId = session?.user?.id ?? null;

    if (roomId === "" || !userId || !startTime || !endTime) {
      setError("Bitte alle Felder ausfüllen.");
      return;
    }

    try {
      const result = await createBooking({
        roomId,
        userId,
        startTime,
        endTime,
        discountCode: discountCode.trim() || undefined,
      });

      const amount = result.payment?.amount;
      setSuccess(
        amount !== undefined
          ? `Buchung wurde erfolgreich angelegt. Preis: ${currency.format(amount)}`
          : "Buchung wurde erfolgreich angelegt."
      );
      resetForm();
      onBooked?.();
    } catch (err) {
      console.error("Fehler beim Anlegen der Buchung:", err);
      setError("Buchung konnte nicht gespeichert werden (Fehler vom Server).");
    }
  };

  const heading = isAdmin
    ? fixedRoomName
      ? `${fixedRoomName} für Kunden buchen`
      : "Für Kunden buchen"
    : fixedRoomName
      ? `${fixedRoomName} buchen`
      : "Neue Buchung";

  return (
    <Card>
      <h2 className="text-lg font-bold mb-4">{heading}</h2>

      <form onSubmit={handleAddBooking} className="grid gap-4 sm:grid-cols-2">
        {!fixedRoomId && (
          <Select
            label="Raum"
            value={roomId}
            onChange={(e) => setRoomId(e.target.value)}
          >
            <option value="">Bitte wählen</option>
            {rooms.map((room) => (
              <option key={room.id} value={room.id}>
                {room.name}
              </option>
            ))}
          </Select>
        )}

        {isAdmin && (
          <TextInput
            label="Kunden-E-Mail"
            type="email"
            value={customerEmail}
            onChange={(e) => setCustomerEmail(e.target.value)}
            placeholder="kunde@beispiel.de"
          />
        )}

        {(isAdmin || !isOrganisation) && (
          <TextInput
            label="Rabattcode (optional)"
            value={discountCode}
            onChange={(e) => setDiscountCode(e.target.value)}
            placeholder="z. B. SUMMER10"
          />
        )}

        <TextInput
          label="Startdatum"
          type="date"
          value={startTime}
          onChange={(e) => setStartTime(e.target.value)}
        />

        <TextInput
          label="Enddatum"
          type="date"
          value={endTime}
          onChange={(e) => setEndTime(e.target.value)}
        />

        <div className="sm:col-span-2 grid gap-3">
          {error && <Alert variant="danger">{error}</Alert>}
          {success && <Alert variant="success">{success}</Alert>}

          <Button type="submit" className="w-full sm:w-auto justify-self-start">
            Buchung speichern
          </Button>
        </div>
      </form>
    </Card>
  );
};

export default BookingAdd;
