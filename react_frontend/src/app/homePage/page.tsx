"use client";
import * as React from "react";
import { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import NavBar from "../components/NavBar";
import UserTable from "../components/UserTable";
import { Role } from "../regist/Role";
import RaumAdd from "../components/RaumAdd";
import BookingTable from "../components/BookingTable";
import BookingAdd from "../components/BookingAdd";
import { apiFetch } from "../api/apiClient";

interface User {
  id: number;
  username: string;
  role: Role;
}

export default function HomePage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const isAdmin = session?.user?.role === "ADMIN";

  const [users, setUsers] = useState<User[]>([]);

  useEffect(() => {
    if (status === "unauthenticated") {
      router.push("/login");
    }
  }, [status, router]);

  const getAll = async () => {
    try {
      const data = await apiFetch<User[]>("/user/getAll");
      setUsers(data);
    } catch (error) {
      console.error("Error by call of Users:", error);
    }
  };

  useEffect(() => {
    if (isAdmin) {
      getAll();
    }
  }, [isAdmin]);

  const handleDelete = async (id: number) => {
    try {
      await apiFetch(`/user/delete/${id}`, { method: "DELETE" });
      console.log("User deleted");
      getAll();
    } catch (error) {
      console.error("Failed to delete user:", error);
    }
  };

  if (status === "loading" || status === "unauthenticated") {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <NavBar />

      {isAdmin && (
        <Box sx={{ margin: 2 }}>
          <h1>Benutzerverwaltung</h1>
          <UserTable users={users} onDelete={handleDelete} />
        </Box>
      )}

      {isAdmin && (
        <Box>
          <RaumAdd />
        </Box>
      )}

      <BookingAdd />
      <Box>
        <BookingTable />
      </Box>
    </Box>
  );
}
