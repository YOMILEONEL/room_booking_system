import { apiFetch } from "./apiClient";
import { Role } from "../regist/Role";

const BASE = "/user";

export type CustomerType = "ORGANISATION" | "KUNDE";

export type User = {
  id: string;
  email: string;
  role: Role;
  customerType?: CustomerType | null;
  organisationName?: string | null;
  firstName?: string | null;
  lastName?: string | null;
  phoneNumber?: string | null;
};

export type UpdateUserRequest = {
  email?: string;
  password?: string;
  currentPassword?: string;
  firstName?: string;
  lastName?: string;
  organisationName?: string;
};

export async function fetchUser(id: string): Promise<User> {
  return apiFetch<User>(`${BASE}/get/${id}`);
}

export async function fetchAllUsers(): Promise<User[]> {
  return apiFetch<User[]>(`${BASE}/getAll`);
}

export async function updateUser(id: string, payload: UpdateUserRequest): Promise<void> {
  await apiFetch<void>(`${BASE}/update/${id}`, { method: "PUT", body: payload });
}

export async function deleteUser(id: string): Promise<void> {
  await apiFetch<void>(`${BASE}/delete/${id}`, { method: "DELETE" });
}
