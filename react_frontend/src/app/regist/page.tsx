"use client";
import * as React from "react";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { Button, Paper, MenuItem, Select, FormControl, InputLabel } from "@mui/material";
import { useState, useEffect } from "react";
import NavBar from "../components/NavBar";
import TextField from "@mui/material/TextField";
import { Role } from "./Role";
import { useRouter } from "next/navigation";



export default function Registration() {
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [role, setRole] = useState<Role | null>(null);
  const [registStatus, setRegistStatus]= useState<Boolean>(false);
  const router = useRouter();
  
  useEffect(() => {
    if (registStatus) {
      router.push("/homePage");
    }
  }, [registStatus, router]);

  const HandleAddUser = async (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    if (username=== null && password===null){
      alert("kk");
      
    }
    const user = { username, password, role };

    try {
      const response = await fetch("http://localhost:8080/api/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(user),
      });

      if (response.ok) {
        console.log("New user added");
        setRegistStatus(true);
      } else {
        console.error("Failed to add user");
      }
    } catch (error) {
      console.error("Error:", error);
    }

    setUsername("");
    setPassword("");
    setRole(null);
  };

  return (
    <Box>
      <NavBar />
      <Box>
        <Paper
          elevation={6}
          sx={{
            margin: "20px auto",
            padding: "30px",
            textAlign: "center",
            maxWidth: 500,
            backgroundColor: "#f9f9f9",
            borderRadius: "10px",
          }}
        >
          <h1
            className="title"
            style={{ fontSize: "24px", marginBottom: "20px", color: "#333" }}
          >
            Registrieren Sie sich bitte!
          </h1>
          <form
            className="input"
            style={{ display: "flex", flexDirection: "column", gap: "15px" }}
          >
            <TextField
              id="username"
              label="Name"
              variant="outlined"
              fullWidth
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
            <TextField
              id="password"
              label="Password"
              variant="outlined"
              type="password"
              fullWidth
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <FormControl fullWidth>
              <InputLabel id="role-label">Rolle</InputLabel>
              <Select
                labelId="role-label"
                id="role"
                value={role || ""}
                onChange={(e) => setRole(e.target.value as Role)}
                label="Rolle"
              >
                <MenuItem value={Role.MEMBER}>MEMBER</MenuItem>
                <MenuItem value={Role.ADMIN}>ADMIN</MenuItem>
                
              </Select>
            </FormControl>
            <Button
              variant="contained"
              sx={{
                padding: "10px",
                fontWeight: "bold",
                backgroundColor: "#1976d2",
                "&:hover": {
                  backgroundColor: "#145a96",
                },
              }}
              onClick={HandleAddUser}
            >
              Add New User
            </Button>
          </form>
        </Paper>
      </Box>
    </Box>
  );
}
