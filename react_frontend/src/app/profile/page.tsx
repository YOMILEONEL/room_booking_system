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
  const { data: session, status } = useSession();
  const router = useRouter();
  const userId = session?.user?.id ?? null;

  const [editUsername, setEditUsername] = React.useState(false);
  const [username, setUsername] = React.useState("");
  const [usernameError, setUsernameError] = React.useState<string | null>(null);
  const [usernameSuccess, setUsernameSuccess] = React.useState<string | null>(null);
  const [savingUsername, setSavingUsername] = React.useState(false);

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
    if (session?.user?.name) {
      setUsername(session.user.name);
    }
  }, [session?.user?.name]);

  if (status === "loading" || status === "unauthenticated") {
    return (
      <div className="flex justify-center mt-24">
        <p className="text-text-muted">Lädt...</p>
      </div>
    );
  }

  const handleSaveUsername = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setUsernameError(null);
    setUsernameSuccess(null);

    if (!userId || !username.trim()) {
      setUsernameError("Bitte einen Benutzernamen angeben.");
      return;
    }

    setSavingUsername(true);
    try {
      await updateUser(userId, { username: username.trim() });
      setUsernameSuccess("Benutzername wurde aktualisiert.");
      setEditUsername(false);
    } catch (err) {
      console.error("Fehler beim Aktualisieren des Benutzernamens:", err);
      setUsernameError("Benutzername konnte nicht gespeichert werden.");
    } finally {
      setSavingUsername(false);
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
                  setUsernameError(null);
                  setUsernameSuccess(null);
                  setEditUsername((v) => !v);
                }}
                className="text-sm font-medium text-primary hover:text-primary-hover transition-colors"
              >
                {editUsername ? "Abbrechen" : "Daten ändern"}
              </button>
            </div>

            {!editUsername && (
              <div className="grid gap-1.5 text-sm">
                <p className="text-text-secondary">
                  <span className="font-semibold text-text-primary">Benutzername:</span>{" "}
                  {session?.user?.name}
                </p>
                <p className="text-text-secondary">
                  <span className="font-semibold text-text-primary">Rolle:</span>{" "}
                  {session?.user?.role}
                </p>
              </div>
            )}

            {editUsername && (
              <form onSubmit={handleSaveUsername} className="grid gap-4">
                <TextInput
                  label="Benutzername"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
                {usernameError && <Alert variant="danger">{usernameError}</Alert>}
                <Button type="submit" disabled={savingUsername} className="justify-self-start">
                  {savingUsername ? "Speichert..." : "Speichern"}
                </Button>
              </form>
            )}

            {!editUsername && usernameSuccess && (
              <div className="mt-3">
                <Alert variant="success">{usernameSuccess}</Alert>
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
