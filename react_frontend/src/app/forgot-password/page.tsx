"use client";

import * as React from "react";
import { useState } from "react";
import Link from "next/link";
import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import { Card, Button, TextInput, Alert } from "../components/ui";
import { requestPasswordReset } from "../api/auth.api";

const GENERIC_MESSAGE =
  "Falls dieser Benutzername existiert, wurde ein Link zum Zurücksetzen des Passworts erzeugt.";

export default function ForgotPasswordPage() {
  const [username, setUsername] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!username) return;

    setBusy(true);
    try {
      await requestPasswordReset(username);
    } catch (err) {
      console.error("Forgot-password error:", err);
    } finally {
      // Immer dieselbe Meldung - egal ob der Benutzername existiert (Anti-Enumeration).
      setMessage(GENERIC_MESSAGE);
      setBusy(false);
    }
  };

  return (
    <div>
      <NavBar />
      <div className="max-w-md mx-auto px-4 py-16">
        <Card>
          <h1 className="text-2xl font-bold mb-2">Passwort vergessen</h1>
          <p className="text-sm text-text-secondary mb-6">
            Gib deinen Benutzernamen ein. Falls das Konto existiert, wird ein
            Reset-Link erzeugt.
          </p>

          {message ? (
            <Alert variant="info">{message}</Alert>
          ) : (
            <form onSubmit={handleSubmit} className="grid gap-4">
              <TextInput
                label="Benutzername"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
              />
              <Button type="submit" disabled={busy} className="w-full">
                {busy ? "Wird gesendet..." : "Reset-Link anfordern"}
              </Button>
            </form>
          )}

          <p className="text-sm text-text-muted mt-6 text-center">
            <Link href="/login" className="text-primary hover:text-primary-hover font-medium">
              ← Zurück zum Login
            </Link>
          </p>
        </Card>
      </div>

      <Footer />
    </div>
  );
}
