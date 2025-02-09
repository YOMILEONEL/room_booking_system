"use client";
import * as React from "react";
import Box from "@mui/material/Box";
import { Button, Paper, MenuItem, Select, FormControl, InputLabel } from "@mui/material";
import { useState, useEffect } from "react";
import NavBar from "../components/NavBar";
import TextField from "@mui/material/TextField";
import { useRouter } from 'next/navigation';



export default function Registration() {
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const router = useRouter();

 

  

  const HandleAddUser = async (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    const user = { username, password };

    try {
      const response = await fetch("http://localhost:8080/api/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(user),
      });

      if (response.ok) {
        console.log("Login successful");
        router.push("/homePage");
      } else {
        alert("Password or Username false");
      }
    } catch (error) {
      console.error("Error:", error);
    }


    setUsername("");

    setPassword("");
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
            Logen Sie sich bitte ein!
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
              Log In
            </Button>
          </form>
        </Paper>
      </Box>
      
    </Box>
  );
}
