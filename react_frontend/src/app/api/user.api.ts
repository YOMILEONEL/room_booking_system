import { apiFetch } from "./apiClient";
import { Role } from "../regist/Role";

const BASE = "/user";

export type User = {
  id: string;
  username: string;
  role: Role;
};

export type UpdateUserRequest = {
  username?: string;
  password?: string;
  currentPassword?: string;
};

export async function fetchAllUsers(): Promise<User[]> {
  return apiFetch<User[]>(`${BASE}/getAll`);
}

export async function updateUser(id: string, payload: UpdateUserRequest): Promise<void> {
  await apiFetch<void>(`${BASE}/update/${id}`, { method: "PUT", body: payload });
}

export async function deleteUser(id: string): Promise<void> {
  await apiFetch<void>(`${BASE}/delete/${id}`, { method: "DELETE" });
}
