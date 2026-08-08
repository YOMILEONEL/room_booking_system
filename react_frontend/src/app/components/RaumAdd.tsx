"use client";

import React, { useRef, useState } from "react";
import RoomTable from "./RoomTable";
import { createRoom, uploadRoomImage, type RoomStatus } from "../api/room.api";
import { Card, Button, TextInput, Textarea, Select, Alert } from "./ui";

const ROOM_STATUS: RoomStatus[] = ["VERFUGBAR", "GEBUCHT"];

export default function RaumAdd() {
  const [name, setName] = useState("");
  const [capacity, setCapacity] = useState<number | "">("");
  const [sizeSquareMeters, setSizeSquareMeters] = useState<number | "">("");
  const [location, setLocation] = useState("");
  const [city, setCity] = useState("");
  const [description, setDescription] = useState("");
  const [pricePerDay, setPricePerDay] = useState<number | "">("");
  const [roomStatus, setRoomStatus] = useState<RoomStatus>("VERFUGBAR");
  const [imageFile, setImageFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Wird erhöht, um RoomTable neu zu laden (über key-Prop)
  const [reloadCounter, setReloadCounter] = useState(0);

  const handleAddRoom = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (
      !name ||
      !location ||
      !city ||
      !description ||
      capacity === "" ||
      capacity <= 0 ||
      sizeSquareMeters === "" ||
      sizeSquareMeters <= 0 ||
      pricePerDay === "" ||
      pricePerDay <= 0
    ) {
      setError("Bitte Name, Kapazität (>0), Größe (>0), Standort, Stadt, Beschreibung und Preis/Tag (>0) ausfüllen.");
      return;
    }

    const newRoom = {
      name,
      capacity,
      sizeSquareMeters,
      location,
      city,
      description,
      pricePerDay,
      roomStatus,
    };

    try {
      const room = await createRoom(newRoom);
      let imageFailed = false;

      if (imageFile) {
        try {
          await uploadRoomImage(room.id, imageFile);
        } catch (imgErr) {
          console.error("Fehler beim Hochladen des Fotos:", imgErr);
          imageFailed = true;
        }
      }

      setSuccess(
        imageFailed
          ? "Raum wurde erstellt, das Foto konnte aber nicht hochgeladen werden."
          : "Raum wurde erfolgreich erstellt."
      );
      setName("");
      setCapacity("");
      setSizeSquareMeters("");
      setLocation("");
      setCity("");
      setDescription("");
      setPricePerDay("");
      setRoomStatus("VERFUGBAR");
      setImageFile(null);
      if (fileInputRef.current) fileInputRef.current.value = "";

      setReloadCounter((c) => c + 1);
    } catch (err) {
      console.error("Fehler beim Erstellen des Raums:", err);
      setError("Es ist ein Fehler beim Speichern aufgetreten.");
    }
  };

  return (
    <div className="grid gap-6">
      <Card>
        <h2 className="text-lg font-bold mb-4">Neuen Raum hinzufügen</h2>

        <form onSubmit={handleAddRoom} className="grid gap-4 sm:grid-cols-2">
          <TextInput
            label="Raumname"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />

          <TextInput
            label="Standort"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
          />

          <TextInput
            label="Stadt"
            value={city}
            onChange={(e) => setCity(e.target.value)}
          />

          <TextInput
            label="Kapazität (Personen)"
            type="number"
            min={1}
            value={capacity}
            onChange={(e) => setCapacity(e.target.value === "" ? "" : Number(e.target.value))}
          />

          <TextInput
            label="Größe (m²)"
            type="number"
            min={0}
            step="0.5"
            value={sizeSquareMeters}
            onChange={(e) => setSizeSquareMeters(e.target.value === "" ? "" : Number(e.target.value))}
          />

          <TextInput
            label="Preis pro Tag (€)"
            type="number"
            min={0}
            step="0.01"
            value={pricePerDay}
            onChange={(e) => setPricePerDay(e.target.value === "" ? "" : Number(e.target.value))}
          />

          <Select
            label="Status"
            value={roomStatus}
            onChange={(e) => setRoomStatus(e.target.value as RoomStatus)}
          >
            {ROOM_STATUS.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </Select>

          <div className="sm:col-span-2">
            <Textarea
              label="Beschreibung"
              placeholder="Ausstattung, Atmosphäre, für welche Anlässe der Raum geeignet ist …"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <label className="grid gap-1.5">
            <span className="text-sm font-semibold text-text-secondary">Foto (optional)</span>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={(e) => setImageFile(e.target.files?.[0] ?? null)}
              className="text-sm text-text-secondary file:mr-3 file:px-3.5 file:py-2 file:rounded-xl file:border-0 file:bg-primary file:text-white file:text-sm file:font-semibold file:cursor-pointer"
            />
          </label>

          <div className="sm:col-span-2 grid gap-3">
            {error && <Alert variant="danger">{error}</Alert>}
            {success && <Alert variant="success">{success}</Alert>}

            <Button type="submit" className="w-full sm:w-auto justify-self-start">
              Raum erstellen
            </Button>
          </div>
        </form>
      </Card>

      <RoomTable key={reloadCounter} isAdmin />
    </div>
  );
}
