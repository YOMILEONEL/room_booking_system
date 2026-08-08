"use client";

import * as React from "react";
import UserTable from "../../components/UserTable";
import { fetchAllUsers, deleteUser, type User } from "../../api/user.api";

export default function AdminUsersPage() {
  const [users, setUsers] = React.useState<User[]>([]);

  const getAll = React.useCallback(async () => {
    try {
      const data = await fetchAllUsers();
      setUsers(data);
    } catch (error) {
      console.error("Error by call of Users:", error);
    }
  }, []);

  React.useEffect(() => {
    getAll();
  }, [getAll]);

  const handleDelete = async (id: string) => {
    try {
      await deleteUser(id);
      getAll();
    } catch (error) {
      console.error("Failed to delete user:", error);
    }
  };

  return (
    <div>
      <h1 className="text-xl font-bold mb-4">Benutzerverwaltung</h1>
      <UserTable users={users} onDelete={handleDelete} />
    </div>
  );
}
