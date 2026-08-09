"use client";

import * as React from "react";
import UserTable from "../../components/UserTable";
import { Alert, ConfirmDialog } from "../../components/ui";
import { extractErrorMessage } from "../../api/apiClient";
import { fetchAllUsers, deleteUser, type User } from "../../api/user.api";

export default function AdminUsersPage() {
  const [users, setUsers] = React.useState<User[]>([]);
  const [error, setError] = React.useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = React.useState<string | null>(null);

  const getAll = React.useCallback(async () => {
    try {
      const data = await fetchAllUsers();
      setUsers(data);
    } catch (error) {
      console.error("Error by call of Users:", error);
      setError("Benutzer konnten nicht geladen werden.");
    }
  }, []);

  React.useEffect(() => {
    getAll();
  }, [getAll]);

  const handleDelete = async () => {
    if (!pendingDeleteId) return;
    setError(null);
    try {
      await deleteUser(pendingDeleteId);
      await getAll();
    } catch (error) {
      console.error("Failed to delete user:", error);
      setError(extractErrorMessage(error, "Benutzer konnte nicht gelöscht werden."));
    } finally {
      setPendingDeleteId(null);
    }
  };

  return (
    <div>
      <h1 className="text-xl font-bold mb-4">Benutzerverwaltung</h1>
      {error && (
        <div className="mb-4">
          <Alert variant="danger">{error}</Alert>
        </div>
      )}
      <UserTable users={users} onDelete={setPendingDeleteId} />

      <ConfirmDialog
        open={pendingDeleteId !== null}
        title="Benutzer löschen"
        message="Möchtest du diesen Benutzer wirklich löschen? Das kann nicht rückgängig gemacht werden."
        confirmLabel="Löschen"
        onConfirm={handleDelete}
        onCancel={() => setPendingDeleteId(null)}
      />
    </div>
  );
}
