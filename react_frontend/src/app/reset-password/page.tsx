"use client";

import * as React from "react";
import { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import { Card, Button, TextInput, Alert } from "../components/ui";
import { resetPassword } from "../api/auth.api";

function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [token, setToken] = useState(searchParams.get("token") ?? "");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);

    if (!token || !newPassword) {
      setError("Bitte Token und neues Passwort eingeben.");
      return;
    }
    if (newPassword.length < 6) {
      setError("Das Passwort muss mindestens 6 Zeichen lang sein.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Die Passwörter stimmen nicht überein.");
      return;
    }

    setBusy(true);
    try {
      await resetPassword(token, newPassword);
      router.push("/login");
    } catch (err) {
      console.error("Reset-password error:", err);
      setError("Der Token ist ungültig oder abgelaufen. Bitte fordere einen neuen an.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Card>
      <h1 className="text-2xl font-bold mb-2">Neues Passwort festlegen</h1>
      <p className="text-sm text-text-secondary mb-6">
        Trage den Reset-Token und dein neues Passwort ein.
      </p>

      <form onSubmit={handleSubmit} className="grid gap-4">
        <TextInput
          label="Reset-Token"
          value={token}
          onChange={(e) => setToken(e.target.value)}
        />
        <TextInput
          label="Neues Passwort"
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          autoComplete="new-password"
        />
        <TextInput
          label="Passwort bestätigen"
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          autoComplete="new-password"
        />

        {error && <Alert variant="danger">{error}</Alert>}

        <Button type="submit" disabled={busy} className="w-full">
          {busy ? "Wird gespeichert..." : "Passwort zurücksetzen"}
        </Button>
      </form>

      <p className="text-sm text-text-muted mt-6 text-center">
        <Link href="/login" className="text-primary hover:text-primary-hover font-medium">
          ← Zurück zum Login
        </Link>
      </p>
    </Card>
  );
}

export default function ResetPasswordPage() {
  return (
    <div>
      <NavBar />
      <div className="max-w-md mx-auto px-4 py-16">
        <Suspense fallback={null}>
          <ResetPasswordForm />
        </Suspense>
      </div>

      <Footer />
    </div>
  );
}
