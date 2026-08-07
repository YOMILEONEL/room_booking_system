"use client";

import React, { useEffect, useState } from "react";
import {
  Box,
  Grid,
  Card,
  CardContent,
  CardMedia,
  Typography,
  Chip,
  CircularProgress,
  IconButton,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import { useSession } from "next-auth/react";
import { apiFetch } from "../api/apiClient";
import { roomImages, defaultRoomImage } from "../lib/roomImages";

interface Room {
  id: number;
  name: string;
  capacity: number;
  location: string;
  roomStatus: string;
}

interface User {
  id: number;
  username: string;
  role: string;
}

interface Booking {
  bookingId: number;
  room: Room;
  user: User;
  startTime: string;
  endTime: string;
}

const BookingTable: React.FC = () => {
  const { data: session } = useSession();
  const userId = session?.user?.id ? Number(session.user.id) : null;

  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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

  useEffect(() => {
    if (!userId) return;

    const loadBookings = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await apiFetch<Booking[] | null>(`/booking/getAll/${userId}`);
        setBookings(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error(err);
        setError("Buchungen konnten nicht geladen werden.");
      } finally {
        setLoading(false);
      }
    };

    loadBookings();
  }, [userId]);

  // Delete Funktion angepasst an Java Endpoint
  const handleDeleteBooking = async (bookingId: number) => {
    if (!userId) return;

    const confirmed = window.confirm(
      `Buchung mit ID ${bookingId} wirklich löschen?`
    );
    if (!confirmed) return;

    try {
      await apiFetch(`/booking/delete/${bookingId}/${userId}`, { method: "DELETE" });

      // Erfolgreich → lokal entfernen
      setBookings((prev) =>
        prev.filter((b) => b.bookingId !== bookingId)
      );
    } catch (err) {
      console.error(err);
      alert("Fehler beim Löschen");
    }
  };

  if (loading) return <CircularProgress />;
  if (error) return <Typography>{error}</Typography>;
  if (bookings.length === 0)
    return <Typography>Keine Buchungen vorhanden.</Typography>;

  return (
    <Box sx={{ p: 2 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Alle Buchungen
      </Typography>

      <Grid container spacing={2}>
        {bookings.map((booking) => {
          const { room, user, startTime, endTime } = booking;
          const status = getBookingStatus(startTime, endTime);

          const imageSrc = roomImages[room?.name] ?? defaultRoomImage;

          return (
            <Grid item xs={12} sm={6} md={4} key={booking.bookingId}>
              <Card sx={{ height: "100%", boxShadow: 3 }}>
                <CardMedia
                  component="img"
                  height="140"
                  image={imageSrc}
                />

                <CardContent>
                  {/* HEADER: Raumname + Delete Button */}
                  <Box
                    sx={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                    }}
                  >
                    <Typography variant="h6">
                      {room?.name}
                    </Typography>

                    <IconButton
                      color="error"
                      onClick={() =>
                        handleDeleteBooking(booking.bookingId)
                      }
                    >
                      <DeleteIcon />
                    </IconButton>
                  </Box>

                  <Typography>Standort: {room.location}</Typography>
                  <Typography>
                    Benutzer: {user.username} ({user.role})
                  </Typography>
                  <Typography>
                    Zeitraum: {startTime} – {endTime}
                  </Typography>

                  <Chip
                    label={status}
                    color={
                      status === "Laufend"
                        ? "success"
                        : status === "Zukünftig"
                        ? "info"
                        : "default"
                    }
                    sx={{ mt: 1 }}
                  />
                </CardContent>
              </Card>
            </Grid>
          );
        })}
      </Grid>
    </Box>
  );
};

export default BookingTable;
