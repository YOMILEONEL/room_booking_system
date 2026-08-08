"use client";

import * as React from "react";
import { useState } from "react";
import Link from "next/link";
import { signIn, getSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import NavBar from "../components/NavBar";
import { Card, Button, TextInput, Alert } from "../components/ui";

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
      redirect: false,
    });

    setLoading(false);

    if (result?.error) {
      console.error("Login-Fehler:", result.error);
      setError("Benutzername oder Passwort falsch!");
      return;
    }

    // Admins landen im Admin-Dashboard, Kunden bleiben auf der normalen Seite.
    const session = await getSession();
    router.push(session?.user?.role === "ADMIN" ? "/admin" : "/rooms");
  };

  return (
    <div>
      <NavBar />
      <div className="max-w-md mx-auto px-4 py-16">
        <Card>
          <h1 className="text-2xl font-bold mb-6">Willkommen zurück</h1>

          <form onSubmit={handleLogin} className="grid gap-4">
            <TextInput
              label="Benutzername"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
            />
            <TextInput
              label="Passwort"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
            />

            <div className="text-right -mt-2">
              <Link href="/forgot-password" className="text-sm text-text-secondary hover:text-primary transition-colors">
                Passwort vergessen?
              </Link>
            </div>

            {error && <Alert variant="danger">{error}</Alert>}

            <Button type="submit" disabled={loading} className="w-full mt-2">
              {loading ? "Wird eingeloggt..." : "Einloggen"}
            </Button>
          </form>

          <p className="text-sm text-text-muted mt-6 text-center">
            Noch kein Konto?{" "}
            <Link href="/regist" className="text-primary hover:text-primary-hover font-medium">
              Jetzt registrieren
            </Link>
          </p>
        </Card>
      </div>
    </div>
  );
}
