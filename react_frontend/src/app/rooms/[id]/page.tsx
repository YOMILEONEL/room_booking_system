"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import Link from "next/link";
import NavBar from "../../components/NavBar";
import Footer from "../../components/Footer";
import BookingAdd from "../../components/BookingAdd";
import RoomGallery from "../../components/RoomGallery";
import {
  fetchRoomById,
  fetchRoomImages,
  uploadRoomImage,
  deleteRoomImage,
  reorderRoomImages,
  updateRoom,
  type Room,
  type RoomImage,
  type RoomStatus,
} from "../../api/room.api";
import { roomImages, defaultRoomImage } from "../../lib/roomImages";
import { Card, Badge, Button, TextInput, Textarea, Select, Alert } from "../../components/ui";

const currency = new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" });
const ROOM_STATUS: RoomStatus[] = ["VERFUGBAR", "GEBUCHT"];

export default function RoomDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: session, status } = useSession();
  const router = useRouter();
  const isAdmin = session?.user?.role === "ADMIN";

  const [room, setRoom] = React.useState<Room | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  const [images, setImages] = React.useState<RoomImage[]>([]);
  const [galleryError, setGalleryError] = React.useState<string | null>(null);
  const [uploading, setUploading] = React.useState(false);
  const fileInputRef = React.useRef<HTMLInputElement>(null);

  const [editRoom, setEditRoom] = React.useState(false);
  const [editName, setEditName] = React.useState("");
  const [editLocation, setEditLocation] = React.useState("");
  const [editCity, setEditCity] = React.useState("");
  const [editDescription, setEditDescription] = React.useState("");
  const [editCapacity, setEditCapacity] = React.useState<number | "">("");
  const [editSizeSquareMeters, setEditSizeSquareMeters] = React.useState<number | "">("");
  const [editPrice, setEditPrice] = React.useState<number | "">("");
  const [editStatus, setEditStatus] = React.useState<RoomStatus>("VERFUGBAR");
  const [editError, setEditError] = React.useState<string | null>(null);
  const [editSuccess, setEditSuccess] = React.useState<string | null>(null);
  const [savingRoom, setSavingRoom] = React.useState(false);

  React.useEffect(() => {
    if (status === "unauthenticated") {
      router.push("/login");
    }
  }, [status, router]);

  React.useEffect(() => {
    if (status !== "authenticated") return;

    fetchRoomById(id)
      .then(setRoom)
      .catch((err) => {
        console.error("Fehler beim Laden des Raums:", err);
        setError("Raum konnte nicht geladen werden.");
      });
  }, [id, status]);

  const loadImages = React.useCallback(() => {
    fetchRoomImages(id)
      .then(setImages)
      .catch((err) => console.error("Fehler beim Laden der Bilder:", err));
  }, [id]);

  React.useEffect(() => {
    if (status !== "authenticated") return;
    loadImages();
  }, [status, loadImages]);

  if (status === "loading" || status === "unauthenticated") {
    return (
      <div className="flex justify-center mt-24">
        <p className="text-text-muted">Lädt...</p>
      </div>
    );
  }

  const fallbackSrc = room ? room.imageUrl ?? roomImages[room.name] ?? defaultRoomImage : defaultRoomImage;
  const gallerySources = images.length > 0 ? images.map((img) => img.imageUrl) : [fallbackSrc];
  const available = room?.roomStatus === "VERFUGBAR";

  const handleUpload = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const file = fileInputRef.current?.files?.[0];
    if (!file) return;

    setGalleryError(null);
    setUploading(true);
    try {
      await uploadRoomImage(id, file);
      if (fileInputRef.current) fileInputRef.current.value = "";
      loadImages();
    } catch (err) {
      console.error("Fehler beim Hochladen:", err);
      setGalleryError("Bild konnte nicht hochgeladen werden.");
    } finally {
      setUploading(false);
    }
  };

  const handleDeleteImage = async (imageId: string) => {
    setGalleryError(null);
    try {
      await deleteRoomImage(id, imageId);
      loadImages();
    } catch (err) {
      console.error("Fehler beim Löschen des Bildes:", err);
      setGalleryError("Bild konnte nicht gelöscht werden.");
    }
  };

  const handleToggleEditRoom = () => {
    if (!editRoom && room) {
      setEditName(room.name);
      setEditLocation(room.location);
      setEditCity(room.city);
      setEditDescription(room.description);
      setEditCapacity(room.capacity);
      setEditSizeSquareMeters(room.sizeSquareMeters ?? "");
      setEditPrice(room.pricePerDay);
      setEditStatus(room.roomStatus);
    }
    setEditError(null);
    setEditSuccess(null);
    setEditRoom((v) => !v);
  };

  const handleSaveRoom = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setEditError(null);
    setEditSuccess(null);

    if (
      !editName ||
      !editLocation ||
      !editCity ||
      !editDescription ||
      editCapacity === "" ||
      editCapacity <= 0 ||
      editSizeSquareMeters === "" ||
      editSizeSquareMeters <= 0 ||
      editPrice === "" ||
      editPrice <= 0
    ) {
      setEditError("Bitte Name, Kapazität (>0), Größe (>0), Standort, Stadt, Beschreibung und Preis/Tag (>0) ausfüllen.");
      return;
    }

    setSavingRoom(true);
    try {
      const updated = await updateRoom(id, {
        name: editName,
        capacity: editCapacity,
        sizeSquareMeters: editSizeSquareMeters,
        location: editLocation,
        city: editCity,
        description: editDescription,
        pricePerDay: editPrice,
        roomStatus: editStatus,
      });
      // updateRoom returns the raw Room entity (no effectivePricePerDay) - this form is
      // admin-only, and admins never get the organisation discount, so the effective price
      // always equals the raw price here.
      setRoom((prev) => (prev ? { ...prev, ...updated, effectivePricePerDay: updated.pricePerDay } : prev));
      setEditSuccess("Raum wurde aktualisiert.");
      setEditRoom(false);
    } catch (err) {
      console.error("Fehler beim Aktualisieren des Raums:", err);
      setEditError("Raum konnte nicht gespeichert werden.");
    } finally {
      setSavingRoom(false);
    }
  };

  const handleMove = async (index: number, direction: -1 | 1) => {
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= images.length) return;

    const reordered = [...images];
    [reordered[index], reordered[targetIndex]] = [reordered[targetIndex], reordered[index]];

    setGalleryError(null);
    try {
      const updated = await reorderRoomImages(id, reordered.map((img) => img.id));
      setImages(updated);
    } catch (err) {
      console.error("Fehler beim Sortieren:", err);
      setGalleryError("Reihenfolge konnte nicht geändert werden.");
    }
  };

  return (
    <div>
      <NavBar />

      <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8 grid gap-6">
        <Link href="/rooms" className="text-sm text-text-secondary hover:text-text-primary transition-colors">
          ← Zurück zu den Räumen
        </Link>

        {error && <Alert variant="danger">{error}</Alert>}

        {!error && !room && <p className="text-text-muted text-sm">Lädt...</p>}

        {room && (
          <>
            <Card className="p-0 overflow-hidden">
              <RoomGallery images={gallerySources} alt={room.name} />
              <div className="p-6 grid gap-3">
                <div className="flex items-start justify-between gap-2">
                  <h1 className="text-2xl font-bold">{room.name}</h1>
                  {room.bookedUntil ? (
                    <Badge variant="pending">Gebucht bis {room.bookedUntil}</Badge>
                  ) : (
                    <Badge variant={available ? "success" : "neutral"}>
                      {available ? "Verfügbar" : "Gebucht"}
                    </Badge>
                  )}
                </div>
                <p className="text-text-secondary">Standort: {room.location}, {room.city}</p>
                <p className="text-text-secondary">
                  Kapazität: {room.capacity} Personen
                  {room.sizeSquareMeters != null && ` · ${room.sizeSquareMeters} m²`}
                </p>
                <p className="text-lg font-semibold text-primary">
                  {room.effectivePricePerDay < room.pricePerDay && (
                    <span className="line-through text-text-muted font-normal mr-1.5">
                      {currency.format(room.pricePerDay)}
                    </span>
                  )}
                  {currency.format(room.effectivePricePerDay)} / Tag
                </p>
                {room.description && (
                  <div className="pt-2 mt-1 border-t border-border-subtle">
                    <h2 className="text-sm font-semibold text-text-primary mb-1.5">Über diesen Raum</h2>
                    <p className="text-text-secondary whitespace-pre-line">{room.description}</p>
                  </div>
                )}
              </div>
            </Card>

            {isAdmin && (
              <Card>
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-lg font-bold">Raumdetails</h2>
                  <button
                    type="button"
                    onClick={handleToggleEditRoom}
                    className="text-sm font-medium text-primary hover:text-primary-hover transition-colors"
                  >
                    {editRoom ? "Abbrechen" : "Bearbeiten"}
                  </button>
                </div>

                {editRoom && (
                  <form onSubmit={handleSaveRoom} className="grid gap-4 sm:grid-cols-2">
                    <TextInput
                      label="Raumname"
                      value={editName}
                      onChange={(e) => setEditName(e.target.value)}
                    />
                    <TextInput
                      label="Standort"
                      value={editLocation}
                      onChange={(e) => setEditLocation(e.target.value)}
                    />
                    <TextInput
                      label="Stadt"
                      value={editCity}
                      onChange={(e) => setEditCity(e.target.value)}
                    />
                    <TextInput
                      label="Kapazität (Personen)"
                      type="number"
                      min={1}
                      value={editCapacity}
                      onChange={(e) => setEditCapacity(e.target.value === "" ? "" : Number(e.target.value))}
                    />
                    <TextInput
                      label="Größe (m²)"
                      type="number"
                      min={0}
                      step="0.5"
                      value={editSizeSquareMeters}
                      onChange={(e) => setEditSizeSquareMeters(e.target.value === "" ? "" : Number(e.target.value))}
                    />
                    <TextInput
                      label="Preis pro Tag (€)"
                      type="number"
                      min={0}
                      step="0.01"
                      value={editPrice}
                      onChange={(e) => setEditPrice(e.target.value === "" ? "" : Number(e.target.value))}
                    />
                    <Select
                      label="Status"
                      value={editStatus}
                      onChange={(e) => setEditStatus(e.target.value as RoomStatus)}
                    >
                      {ROOM_STATUS.map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                    </Select>

                    <div className="sm:col-span-2">
                      <Textarea
                        label="Beschreibung"
                        placeholder="Ausstattung, Atmosphäre, für welche Anlässe der Raum geeignet ist …"
                        value={editDescription}
                        onChange={(e) => setEditDescription(e.target.value)}
                      />
                    </div>

                    <div className="sm:col-span-2 grid gap-3">
                      {editError && <Alert variant="danger">{editError}</Alert>}
                      <Button type="submit" disabled={savingRoom} className="w-full sm:w-auto justify-self-start">
                        {savingRoom ? "Speichert..." : "Speichern"}
                      </Button>
                    </div>
                  </form>
                )}

                {!editRoom && editSuccess && (
                  <div className="mt-1">
                    <Alert variant="success">{editSuccess}</Alert>
                  </div>
                )}
              </Card>
            )}

            {isAdmin && (
              <Card>
                <h2 className="text-lg font-bold mb-4">Fotos verwalten</h2>

                <form onSubmit={handleUpload} className="flex flex-wrap items-center gap-3 mb-4">
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    className="text-sm text-text-secondary file:mr-3 file:px-3.5 file:py-2 file:rounded-xl file:border-0 file:bg-primary file:text-white file:text-sm file:font-semibold file:cursor-pointer"
                  />
                  <Button type="submit" variant="secondary" disabled={uploading} className="text-sm py-2">
                    {uploading ? "Wird hochgeladen..." : "Hinzufügen"}
                  </Button>
                </form>

                {galleryError && (
                  <div className="mb-4">
                    <Alert variant="danger">{galleryError}</Alert>
                  </div>
                )}

                {images.length === 0 && (
                  <p className="text-text-muted text-sm">Noch keine Fotos hochgeladen.</p>
                )}

                {images.length > 0 && (
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                    {images.map((img, index) => (
                      <div key={img.id} className="relative rounded-xl overflow-hidden border border-border-subtle">
                        <img src={img.imageUrl} alt="" className="w-full h-24 object-cover" />
                        {index === 0 && (
                          <span className="absolute top-1.5 left-1.5">
                            <Badge variant="info">Titelbild</Badge>
                          </span>
                        )}
                        <div className="flex items-center justify-between gap-1 px-1.5 py-1.5 bg-card">
                          <button
                            type="button"
                            onClick={() => handleMove(index, -1)}
                            disabled={index === 0}
                            className="text-xs px-1.5 py-1 rounded text-text-secondary hover:text-text-primary hover:bg-black/[0.04] disabled:opacity-30 transition-colors"
                            aria-label="Nach oben"
                          >
                            ▲
                          </button>
                          <button
                            type="button"
                            onClick={() => handleMove(index, 1)}
                            disabled={index === images.length - 1}
                            className="text-xs px-1.5 py-1 rounded text-text-secondary hover:text-text-primary hover:bg-black/[0.04] disabled:opacity-30 transition-colors"
                            aria-label="Nach unten"
                          >
                            ▼
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDeleteImage(img.id)}
                            className="text-xs px-1.5 py-1 rounded text-danger hover:text-danger/80 hover:bg-black/[0.04] transition-colors"
                            aria-label="Löschen"
                          >
                            ✕
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            )}

            <BookingAdd fixedRoomId={room.id} fixedRoomName={room.name} />
          </>
        )}
      </div>

      <Footer />
    </div>
  );
}
