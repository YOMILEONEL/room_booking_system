"use client";

import React, { useState } from "react";
import {
  Box,
  Paper,
  TextField,
  Button,
  Typography,
} from "@mui/material";
import { useSession } from "next-auth/react";
import { apiFetch } from "../api/apiClient";

const BookingAdd: React.FC = () => {
  const { data: session } = useSession();

  // Formularzustände
  const [roomId, setRoomId] = useState<number | "">("");
  const [startTime, setStartTime] = useState<string>("");
  const [endTime, setEndTime] = useState<string>("");

  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Handler zum Absenden der Buchung
  const handleAddBooking = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    const userId = session?.user?.id ? Number(session.user.id) : null;

    // einfache Validierung
    if (roomId === "" || !userId || !startTime || !endTime) {
      setError("Bitte alle Felder ausfüllen.");
      return;
    }

    // BookingDTO-Objekt gemäß Backend
    const newBooking = {
      roomId: Number(roomId),
      userId,
      startTime, // "YYYY-MM-DD" passt zu LocalDate
      endTime,
    };

    try {
      await apiFetch("/booking/add", { method: "POST", body: newBooking });

      setSuccess("Buchung wurde erfolgreich angelegt.");
      // Formular zurücksetzen
      setRoomId("");
      setStartTime("");
      setEndTime("");
    } catch (err) {
      console.error("Fehler beim Anlegen der Buchung:", err);
      setError("Buchung konnte nicht gespeichert werden (Fehler vom Server).");
    }
  };

  return (
    <Box sx={{ width: "100%", p: 2 }}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>
          Neue Buchung hinzufügen
        </Typography>

        <form
          onSubmit={handleAddBooking}
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "16px",
          }}
        >
          {/* Room ID */}
          <TextField
            label="Raum-ID"
            type="number"
            value={roomId}
            onChange={(e) =>
              setRoomId(e.target.value === "" ? "" : Number(e.target.value))
            }
            fullWidth
          />

          {/* Startdatum */}
          <TextField
            label="Startdatum"
            type="date"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
            fullWidth
            InputLabelProps={{ shrink: true }}
          />

          {/* Enddatum */}
          <TextField
            label="Enddatum"
            type="date"
            value={endTime}
            onChange={(e) => setEndTime(e.target.value)}
            fullWidth
            InputLabelProps={{ shrink: true }}
          />

          {error && (
            <Typography color="error" sx={{ mt: 1 }}>
              {error}
            </Typography>
          )}
          {success && (
            <Typography color="success.main" sx={{ mt: 1 }}>
              {success}
            </Typography>
          )}

          <Button type="submit" variant="contained">
            Buchung speichern
          </Button>
        </form>
      </Paper>
    </Box>
  );
};

export default BookingAdd;
