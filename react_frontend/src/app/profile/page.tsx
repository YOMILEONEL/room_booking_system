"use client";

import * as React from "react";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import BookingTable from "../components/BookingTable";
import { updateUser } from "../api/user.api";
import { extractErrorMessage } from "../api/apiClient";
import { Card, Button, TextInput, Alert } from "../components/ui";

export default function ProfilePage() {
  const { data: session, status, update } = useSession();
  const router = useRouter();
  const userId = session?.user?.id ?? null;

  const [editEmail, setEditEmail] = React.useState(false);
  const [email, setEmail] = React.useState("");
  const [emailError, setEmailError] = React.useState<string | null>(null);
  const [emailSuccess, setEmailSuccess] = React.useState<string | null>(null);
  const [savingEmail, setSavingEmail] = React.useState(false);

  const [editPassword, setEditPassword] = React.useState(false);
  const [currentPassword, setCurrentPassword] = React.useState("");
  const [newPassword, setNewPassword] = React.useState("");
  const [confirmPassword, setConfirmPassword] = React.useState("");
  const [passwordError, setPasswordError] = React.useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = React.useState<string | null>(null);
  const [savingPassword, setSavingPassword] = React.useState(false);

  React.useEffect(() => {
    if (status === "unauthenticated") {
      router.push("/login");
    }
  }, [status, router]);

  React.useEffect(() => {
    if (session?.user?.email) {
      setEmail(session.user.email);
    }
  }, [session?.user?.email]);

  if (status === "loading" || status === "unauthenticated") {
    return (
      <div className="flex justify-center mt-24">
        <p className="text-text-muted">Lädt...</p>
      </div>
    );
  }

  const handleSaveEmail = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setEmailError(null);
    setEmailSuccess(null);

    if (!userId || !email.trim()) {
      setEmailError("Bitte eine E-Mail-Adresse angeben.");
      return;
    }

    setSavingEmail(true);
    try {
      const trimmedEmail = email.trim();
      await updateUser(userId, { email: trimmedEmail });
      // Erzwingt einen frischen Access-Token mit der neuen E-Mail als "sub" - sonst schlägt
      // jeder Backend-Call mit der alten E-Mail fehl, bis das Token automatisch abläuft.
      await update({ email: trimmedEmail });
      setEmailSuccess("E-Mail-Adresse wurde aktualisiert.");
      setEditEmail(false);
    } catch (err) {
      console.error("Fehler beim Aktualisieren der E-Mail-Adresse:", err);
      setEmailError(extractErrorMessage(err, "E-Mail-Adresse konnte nicht gespeichert werden."));
    } finally {
      setSavingEmail(false);
    }
  };

  const handleSavePassword = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setPasswordError(null);
    setPasswordSuccess(null);

    if (!userId || !currentPassword || !newPassword) {
      setPasswordError("Bitte aktuelles und neues Passwort angeben.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError("Die Passwörter stimmen nicht überein.");
      return;
    }

    setSavingPassword(true);
    try {
      await updateUser(userId, { password: newPassword, currentPassword });
      setPasswordSuccess("Passwort wurde geändert.");
      setEditPassword(false);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      console.error("Fehler beim Ändern des Passworts:", err);
      setPasswordError(extractErrorMessage(err, "Passwort konnte nicht geändert werden."));
    } finally {
      setSavingPassword(false);
    }
  };

  return (
    <div>
      <NavBar />

      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8 grid gap-8">
        <h1 className="text-xl font-bold">Mein Profil</h1>

        <div className="grid gap-6 sm:grid-cols-2">
          <Card>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold">Meine Daten</h2>
              <button
                type="button"
                onClick={() => {
                  setEmailError(null);
                  setEmailSuccess(null);
                  setEditEmail((v) => !v);
                }}
                className="text-sm font-medium text-primary hover:text-primary-hover transition-colors"
              >
                {editEmail ? "Abbrechen" : "Daten ändern"}
              </button>
            </div>

            {!editEmail && (
              <div className="grid gap-1.5 text-sm">
                <p className="text-text-secondary">
                  <span className="font-semibold text-text-primary">E-Mail:</span>{" "}
                  {session?.user?.email}
                </p>
                <p className="text-text-secondary">
                  <span className="font-semibold text-text-primary">Rolle:</span>{" "}
                  {session?.user?.role}
                </p>
              </div>
            )}

            {editEmail && (
              <form onSubmit={handleSaveEmail} className="grid gap-4">
                <TextInput
                  label="E-Mail"
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
                {emailError && <Alert variant="danger">{emailError}</Alert>}
                <Button type="submit" disabled={savingEmail} className="justify-self-start">
                  {savingEmail ? "Speichert..." : "Speichern"}
                </Button>
              </form>
            )}

            {!editEmail && emailSuccess && (
              <div className="mt-3">
                <Alert variant="success">{emailSuccess}</Alert>
              </div>
            )}
          </Card>

          <Card>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold">Passwort</h2>
              <button
                type="button"
                onClick={() => {
                  setPasswordError(null);
                  setPasswordSuccess(null);
                  setCurrentPassword("");
                  setNewPassword("");
                  setConfirmPassword("");
                  setEditPassword((v) => !v);
                }}
                className="text-sm font-medium text-primary hover:text-primary-hover transition-colors"
              >
                {editPassword ? "Abbrechen" : "Passwort ändern"}
              </button>
            </div>

            {!editPassword && (
              <p className="text-sm text-text-muted">
                Aus Sicherheitsgründen wird dein Passwort nicht angezeigt.
              </p>
            )}

            {editPassword && (
              <form onSubmit={handleSavePassword} className="grid gap-4">
                <TextInput
                  label="Aktuelles Passwort"
                  type="password"
                  autoComplete="current-password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                />
                <TextInput
                  label="Neues Passwort"
                  type="password"
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
                <TextInput
                  label="Neues Passwort bestätigen"
                  type="password"
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
                {passwordError && <Alert variant="danger">{passwordError}</Alert>}
                <Button type="submit" disabled={savingPassword} className="justify-self-start">
                  {savingPassword ? "Speichert..." : "Speichern"}
                </Button>
              </form>
            )}

            {!editPassword && passwordSuccess && (
              <div className="mt-3">
                <Alert variant="success">{passwordSuccess}</Alert>
              </div>
            )}
          </Card>
        </div>

        <BookingTable />
      </div>

      <Footer />
    </div>
  );
}
