"use client";

import * as React from "react";
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Toolbar,
  Typography,
  IconButton,
  Tooltip,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import { apiFetch } from "../api/apiClient";
import { roomImages, defaultRoomImage } from "../lib/roomImages";

type Room = {
  id: number;
  name: string;
  capacity: number;
  location: string;
  roomStatus: string;
};

export default function RoomTable() {
  const [rooms, setRooms] = React.useState<Room[]>([]);
  const [error, setError] = React.useState<string | null>(null);

  const [page, setPage] = React.useState(0);
  const [rowsPerPage, setRowsPerPage] = React.useState(5);

  // Räume laden
  const loadRooms = React.useCallback(async () => {
    try {
      setError(null);
      const data = await apiFetch<Room[]>("/room/Get");
      setRooms(data);
    } catch (err) {
      console.error("Fehler beim Laden der Räume:", err);
      setError("Räume konnten nicht geladen werden.");
    }
  }, []);

  React.useEffect(() => {
    loadRooms();
  }, [loadRooms]);

  // Raum löschen
  const handleDelete = async (id: number) => {
    const confirmed = window.confirm(`Raum mit ID ${id} wirklich löschen?`);
    if (!confirmed) return;

    try {
      await apiFetch(`/room/delete/${id}`, { method: "DELETE" });
      // Lokal aus der Liste entfernen
      setRooms((prev) => prev.filter((r) => r.id !== id));
    } catch (err) {
      console.error("Fehler beim Löschen:", err);
      alert("Es ist ein Fehler beim Löschen aufgetreten.");
    }
  };

  // Pagination-Handler
  const handleChangePage = (_: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const pagedRooms = React.useMemo(
    () =>
      rooms.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage),
    [rooms, page, rowsPerPage]
  );

  return (
    <Box sx={{ width: "100%" }}>
      {error && (
        <Typography color="error" sx={{ mb: 2 }}>
          {error}
        </Typography>
      )}

      <Paper sx={{ width: "100%", mb: 2 }}>
        <Toolbar>
          <Typography variant="h6" sx={{ flex: 1 }}>
            Räume
          </Typography>
        </Toolbar>

        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Foto</TableCell>
                <TableCell>Raumname</TableCell>
                <TableCell align="right">Kapazität</TableCell>
                <TableCell>Standort</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="center">Aktionen</TableCell>
              </TableRow>
            </TableHead>

            <TableBody>
              {pagedRooms.map((room) => {
                const imgSrc =
                  roomImages[room.name] ?? defaultRoomImage;

                return (
                  <TableRow key={room.id} hover>
                    {/* Foto */}
                    <TableCell>
                      <img
                        src={imgSrc}
                        alt={room.name}
                        style={{
                          width: 80,
                          height: 60,
                          objectFit: "cover",
                          borderRadius: 4,
                        }}
                      />
                    </TableCell>

                    {/* Name */}
                    <TableCell>{room.name}</TableCell>

                    {/* Kapazität */}
                    <TableCell align="right">{room.capacity}</TableCell>

                    {/* Standort */}
                    <TableCell>{room.location}</TableCell>

                    {/* Status */}
                    <TableCell>{room.roomStatus}</TableCell>

                    {/* Aktionen */}
                    <TableCell align="center">
                      <Tooltip title="Löschen">
                        <IconButton
                          color="error"
                          onClick={() => handleDelete(room.id)}
                        >
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                );
              })}

              {rooms.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    Keine Räume gefunden.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <TablePagination
          rowsPerPageOptions={[5, 10, 25]}
          component="div"
          count={rooms.length}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={handleChangePage}
          onRowsPerPageChange={handleChangeRowsPerPage}
        />
      </Paper>
    </Box>
  );
}
