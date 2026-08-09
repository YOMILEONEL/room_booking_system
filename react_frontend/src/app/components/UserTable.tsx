"use client";
import * as React from "react";
import type { User } from "../api/user.api";
import { Card, Badge } from "./ui";

interface UserTableProps {
  users: User[];
  onDelete: (id: string) => void;
}

export default function UserTable({ users, onDelete }: UserTableProps) {
  if (users.length === 0) {
    return <p className="text-text-muted text-sm">Keine Benutzer gefunden.</p>;
  }

  return (
    <Card className="p-0 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border-subtle text-left text-text-secondary">
              <th className="px-4 py-3 font-semibold">ID</th>
              <th className="px-4 py-3 font-semibold">E-Mail</th>
              <th className="px-4 py-3 font-semibold">Rolle</th>
              <th className="px-4 py-3 font-semibold text-right">Aktionen</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id} className="border-b border-border-subtle last:border-0 hover:bg-black/[0.02]">
                <td className="px-4 py-3 text-text-muted">{user.id}</td>
                <td className="px-4 py-3">{user.email}</td>
                <td className="px-4 py-3">
                  <Badge variant={user.role === "ADMIN" ? "info" : "neutral"}>{user.role}</Badge>
                </td>
                <td className="px-4 py-3 text-right">
                  <button
                    onClick={() => onDelete(user.id)}
                    className="text-danger hover:text-danger/80 font-medium transition-colors"
                  >
                    Löschen
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}
