"use client";

import React, { useState } from "react";
import {
  Box,
  Paper,
  TextField,
  Button,
  Typography,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
} from "@mui/material";
import RoomTable from "./RoomTable";
import { apiFetch } from "../api/apiClient";

// Mögliche Raum-Status-Werte entsprechend deinem Enum
const ROOM_STATUS = ["VERFUGBAR", "GEBUCHT"];

export default function RaumAdd() {
  const [name, setName] = useState("");
  const [capacity, setCapacity] = useState<number | "">("");
  const [location, setLocation] = useState("");
  const [roomStatus, setRoomStatus] = useState<string>("VERFUGBAR");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Wird erhöht, um RoomTable neu zu laden (über key-Prop)
  const [reloadCounter, setReloadCounter] = useState(0);

  // Handler zum Anlegen eines neuen Raums
  const handleAddRoom = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    // Einfache Validierung
    if (!name || !location || capacity === "" || capacity <= 0) {
      setError("Bitte Name, Kapazität (>0) und Standort ausfüllen.");
      return;
    }

    const newRoom = {
      name,
      capacity,
      location,
      roomStatus, // wird vom Backend als Enum-String erwartet
    };

    try {
      await apiFetch("/room/save", { method: "POST", body: newRoom });

      setSuccess("Raum wurde erfolgreich erstellt.");
      setName("");
      setCapacity("");
      setLocation("");
      setRoomStatus("VERFUGBAR");

      // RoomTable neu laden, indem wir den key ändern
      setReloadCounter((c) => c + 1);
    } catch (err) {
      console.error("Fehler beim Erstellen des Raums:", err);
      setError("Es ist ein Fehler beim Speichern aufgetreten.");
    }
  };

  return (
    <Box sx={{ width: "100%", p: 2 }}>
      {/* Formular zum Hinzufügen eines neuen Raums */}
      <Paper sx={{ p: 3, mb: 4 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>
          Neuen Raum hinzufügen
        </Typography>

        <form
          onSubmit={handleAddRoom}
          style={{ display: "flex", flexDirection: "column", gap: "16px" }}
        >
          <TextField
            label="Raumname"
            value={name}
            onChange={(e) => setName(e.target.value)}
            fullWidth
          />

          <TextField
            label="Kapazität"
            type="number"
            value={capacity}
            onChange={(e) => setCapacity(e.target.value === "" ? "" : Number(e.target.value))}
            fullWidth
          />

          <TextField
            label="Standort"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            fullWidth
          />

          <FormControl fullWidth>
            <InputLabel id="roomstatus-label">Status</InputLabel>
            <Select
              labelId="roomstatus-label"
              value={roomStatus}
              label="Status"
              onChange={(e) => setRoomStatus(e.target.value as string)}
            >
              {ROOM_STATUS.map((status) => (
                <MenuItem key={status} value={status}>
                  {status}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

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
            Raum erstellen
          </Button>
        </form>
      </Paper>

      {/* Tabelle mit Räumen – key sorgt für Reload nach dem Hinzufügen */}
      <RoomTable key={reloadCounter} />
    </Box>
  );
}
