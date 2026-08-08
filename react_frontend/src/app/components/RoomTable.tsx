"use client";

import * as React from "react";
import Link from "next/link";
import { fetchRooms, activateRoom, deactivateRoom, type Room } from "../api/room.api";
import { roomImages, defaultRoomImage } from "../lib/roomImages";
import { Card, Badge, Alert, TextInput } from "./ui";

const currency = new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" });

export default function RoomTable({ isAdmin = false }: { isAdmin?: boolean }) {
  const [rooms, setRooms] = React.useState<Room[]>([]);
  const [error, setError] = React.useState<string | null>(null);
  const [query, setQuery] = React.useState("");

  const loadRooms = React.useCallback(async () => {
    try {
      setError(null);
      const data = await fetchRooms();
      setRooms(data);
    } catch (err) {
      console.error("Fehler beim Laden der Räume:", err);
      setError("Räume konnten nicht geladen werden.");
    }
  }, []);

  React.useEffect(() => {
    loadRooms();
  }, [loadRooms]);

  const handleToggleActive = async (room: Room) => {
    try {
      if (room.active === false) {
        await activateRoom(room.id);
      } else {
        await deactivateRoom(room.id);
      }
      loadRooms();
    } catch (err) {
      console.error("Fehler beim Ändern des Status:", err);
      alert("Status konnte nicht geändert werden.");
    }
  };

  const normalizedQuery = query.trim().toLowerCase();
  const filteredRooms = normalizedQuery
    ? rooms.filter((room) => {
        const haystack = `${room.name} ${room.city ?? ""} ${room.description ?? ""}`.toLowerCase();
        return haystack.includes(normalizedQuery);
      })
    : rooms;

  return (
    <div>
      <h2 className="text-lg font-bold mb-4">Räume</h2>

      <div className="mb-5 max-w-sm">
        <TextInput
          label="Suche"
          placeholder="Stadt, Raumname oder Anlass …"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      {error && (
        <div className="mb-4">
          <Alert variant="danger">{error}</Alert>
        </div>
      )}

      {rooms.length === 0 && !error && (
        <p className="text-text-muted text-sm">Keine Räume gefunden.</p>
      )}

      {rooms.length > 0 && filteredRooms.length === 0 && !error && (
        <p className="text-text-muted text-sm">Keine Räume passen zu &bdquo;{query}&ldquo;.</p>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredRooms.map((room) => {
          const imgSrc = room.imageUrl ?? roomImages[room.name] ?? defaultRoomImage;
          const available = room.roomStatus === "VERFUGBAR";
          const isInactive = room.active === false;

          return (
            <Card
              key={room.id}
              className={`p-0 overflow-hidden flex flex-col ${isInactive ? "opacity-60" : ""}`}
            >
              <Link href={`/rooms/${room.id}`} className="flex flex-col flex-1">
                <img
                  src={imgSrc}
                  alt={room.name}
                  className="w-full h-36 object-cover"
                />
                <div className="p-4 flex-1 flex flex-col gap-2">
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-semibold">{room.name}</h3>
                    <div className="flex flex-col items-end gap-1">
                      {isInactive && <Badge variant="neutral">Inaktiv</Badge>}
                      {room.bookedUntil ? (
                        <Badge variant="pending">Gebucht bis {room.bookedUntil}</Badge>
                      ) : (
                        <Badge variant={available ? "success" : "neutral"}>
                          {available ? "Verfügbar" : "Gebucht"}
                        </Badge>
                      )}
                    </div>
                  </div>
                  <p className="text-sm text-text-secondary">{room.location}, {room.city}</p>
                  <p className="text-sm text-text-muted">
                    Kapazität: {room.capacity}
                    {room.sizeSquareMeters != null && ` · ${room.sizeSquareMeters} m²`}
                  </p>
                  <p className="text-sm font-semibold text-primary mt-auto">
                    {room.effectivePricePerDay < room.pricePerDay && (
                      <span className="line-through text-text-muted font-normal mr-1.5">
                        {currency.format(room.pricePerDay)}
                      </span>
                    )}
                    {currency.format(room.effectivePricePerDay)} / Tag
                  </p>
                </div>
              </Link>
              {isAdmin && (
                <button
                  onClick={() => handleToggleActive(room)}
                  className="text-sm font-medium text-text-secondary hover:text-text-primary transition-colors self-start mx-4 mb-4"
                >
                  {isInactive ? "Aktivieren" : "Deaktivieren"}
                </button>
              )}
            </Card>
          );
        })}
      </div>
    </div>
  );
}
