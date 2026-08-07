"use client";

import * as React from "react";
import Box from "@mui/material/Box";
import { Button, Paper, TextField } from "@mui/material";
import { useState } from "react";
import NavBar from "../components/NavBar"; // Pfad ggf. anpassen
import { signIn } from "next-auth/react";
import { useRouter } from "next/navigation";

export default function Login() {
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(false);
  const router = useRouter();

  const handleLogin = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    if (!username || !password) {
      setError("Bitte Benutzername und Passwort eingeben.");
      setLoading(false);
      return;
    }

    const result = await signIn("credentials", {
      username,
      password,
      redirect: false,   // kein Auto-Redirect durch NextAuth
    });

    setLoading(false);

    // Fehler vom Credentials-Provider (inkl. 401 von deinem Backend)
    if (result?.error) {
      console.error("Login-Fehler:", result.error);
      setError("Benutzername oder Passwort falsch!");
      return;
    }

    // Erfolgreich → manuelle Weiterleitung
    router.push("/homePage");
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
            style={{ fontSize: "24px", marginBottom: "20px", color: "#333" }}
          >
            Loggen Sie sich bitte ein!
          </h1>

          <form
            onSubmit={handleLogin}
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

            {error && (
              <p style={{ color: "red", margin: 0 }}>{error}</p>
            )}

            <Button
              type="submit"
              variant="contained"
              disabled={loading}
              sx={{
                padding: "10px",
                fontWeight: "bold",
                backgroundColor: "#1976d2",
                "&:hover": {
                  backgroundColor: "#145a96",
                },
              }}
            >
              {loading ? "Wird eingeloggt..." : "Log In"}
            </Button>
          </form>
        </Paper>
      </Box>
    </Box>
  );
}
