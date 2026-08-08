import { apiFetch } from "./apiClient";

const BASE = "/room";

export type RoomStatus = "VERFUGBAR" | "GEBUCHT";

export type Room = {
  id: string;
  name: string;
  capacity: number;
  location: string;
  pricePerNight: number;
  effectivePricePerNight: number;
  roomStatus: RoomStatus;
  imageUrl?: string | null;
  active?: boolean;
  bookedUntil?: string | null;
};

export type CreateRoomRequest = {
  name: string;
  capacity: number;
  location: string;
  pricePerNight: number;
  roomStatus: RoomStatus;
};

export async function fetchRooms(): Promise<Room[]> {
  return apiFetch<Room[]>(`${BASE}/Get`);
}

export async function fetchRoomById(id: string): Promise<Room> {
  return apiFetch<Room>(`${BASE}/${id}`);
}

export async function createRoom(payload: CreateRoomRequest): Promise<Room> {
  return apiFetch<Room>(`${BASE}/save`, { method: "POST", body: payload });
}

export async function updateRoom(id: string, payload: CreateRoomRequest): Promise<Room> {
  return apiFetch<Room>(`${BASE}/update/${id}`, { method: "PUT", body: payload });
}

export async function activateRoom(id: string): Promise<Room> {
  return apiFetch<Room>(`${BASE}/${id}/activate`, { method: "PUT" });
}

export async function deactivateRoom(id: string): Promise<Room> {
  return apiFetch<Room>(`${BASE}/${id}/deactivate`, { method: "PUT" });
}

export type RoomImage = {
  id: string;
  imageUrl: string;
  position: number;
};

export async function fetchRoomImages(roomId: string): Promise<RoomImage[]> {
  return apiFetch<RoomImage[]>(`${BASE}/${roomId}/images`);
}

export async function uploadRoomImage(id: string, file: File): Promise<RoomImage> {
  const formData = new FormData();
  formData.append("file", file);
  return apiFetch<RoomImage>(`${BASE}/${id}/image`, { method: "POST", body: formData });
}

export async function deleteRoomImage(roomId: string, imageId: string): Promise<void> {
  await apiFetch<void>(`${BASE}/${roomId}/images/${imageId}`, { method: "DELETE" });
}

export async function reorderRoomImages(roomId: string, orderedImageIds: string[]): Promise<RoomImage[]> {
  return apiFetch<RoomImage[]>(`${BASE}/${roomId}/images/reorder`, { method: "PUT", body: orderedImageIds });
}
