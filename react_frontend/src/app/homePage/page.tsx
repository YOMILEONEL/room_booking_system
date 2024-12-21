"use client";
import * as React from "react";
import { useEffect, useState } from "react";
import Box from "@mui/material/Box";
import NavBar from "../components/NavBar";
import UserTable from "../components/UserTable";
import { Role } from "../regist/Role";

interface User {
  id: number;
  username: string;
  role: Role;
}



export default function HomePage() {
  const [users, setUsers] = useState<User[]>([]);

  const getAll = async () => {
    try {
      const response = await fetch("http://localhost:8080/user/get");
      const data = await response.json();
      setUsers(data);
    } catch (error) {
      console.error("Error by call of Users:", error);
    }
  };

  useEffect(() => {
    getAll();
  }, []);


  const handleDelete = async (id: number) => {
    try {
      const response = await fetch(`http://localhost:8080/user/delete/${id}`, {
        method: "DELETE",
      });

      if (response.ok) {
        console.log("User deleted");
        getAll();
      } else {
        console.error("Failed to delete user");
      }
    } catch (error) {
      console.error("Error:", error);
    }
  };

  return (
    <Box>
      <NavBar />
      <Box sx={{ margin: 2 }}>
        <h1>Benutzerverwaltung</h1>
        <UserTable users={users} onDelete={handleDelete} />
      </Box>
    </Box>
  );
}
