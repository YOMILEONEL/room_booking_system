"use client";

import * as React from "react";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import BookingTable from "../components/BookingTable";
import { fetchUser, updateUser, type User } from "../api/user.api";
import { extractErrorMessage } from "../api/apiClient";
import { Card, Button, TextInput, Alert } from "../components/ui";

// Mirrors the backend's User.getDisplayName() (steve.bookingssystem.user.model.User) - used
// right after a save to refresh the NextAuth session immediately (see update() call below),
// without waiting for a full page reload / fresh /user/get fetch. Deliberately does NOT check
// customerType: some accounts have real firstName/lastName but a null customerType (rows
// predating that field, or created outside /api/register), and gating on it made those show
// their email forever with no way to fix it even though the name was right there.
function computeDisplayName(user: {
  email: string;
  organisationName?: string | null;
  firstName?: string | null;
  lastName?: string | null;
}): string {
  if (user.firstName?.trim() && user.lastName?.trim()) {
    return `${user.firstName.trim()} ${user.lastName.trim()}`;
  }
  if (user.organisationName?.trim()) {
    return user.organisationName.trim();
  }
  return user.email;
}

export default function ProfilePage() {
  const { data: session, status, update } = useSession();
  const router = useRouter();
  const userId = session?.user?.id ?? null;

  const [userDetails, setUserDetails] = React.useState<User | null>(null);

  const [editEmail, setEditEmail] = React.useState(false);
  const [email, setEmail] = React.useState("");
  const [firstName, setFirstName] = React.useState("");
  const [lastName, setLastName] = React.useState("");
  const [organisationName, setOrganisationName] = React.useState("");
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

  React.useEffect(() => {
    if (!userId) return;
    fetchUser(userId)
      .then((u) => {
        setUserDetails(u);
        setFirstName(u.firstName ?? "");
        setLastName(u.lastName ?? "");
        setOrganisationName(u.organisationName ?? "");
      })
      .catch((err) => console.error("Fehler beim Laden der Profildaten:", err));
  }, [userId]);

  if (status === "loading" || status === "unauthenticated") {
    return (
      <div className="flex justify-center mt-24">
        <p className="text-text-muted">Lädt...</p>
      </div>
    );
  }

  const isOrganisation = userDetails?.customerType === "ORGANISATION";
  const currentDisplayName = userDetails ? computeDisplayName(userDetails) : null;

  const handleSaveEmail = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setEmailError(null);
    setEmailSuccess(null);

    const trimmedEmail = email.trim();
    const trimmedFirstName = firstName.trim();
    const trimmedLastName = lastName.trim();
    const trimmedOrganisationName = organisationName.trim();

    if (!userId || !userDetails || !trimmedEmail) {
      setEmailError("Bitte eine E-Mail-Adresse angeben.");
      return;
    }
    if (isOrganisation && !trimmedOrganisationName) {
      setEmailError("Bitte einen Organisationsnamen angeben.");
      return;
    }
    if (!isOrganisation && (!trimmedFirstName || !trimmedLastName)) {
      setEmailError("Bitte Vor- und Nachnamen angeben.");
      return;
    }

    setSavingEmail(true);
    try {
      await updateUser(userId, {
        email: trimmedEmail,
        firstName: isOrganisation ? undefined : trimmedFirstName,
        lastName: isOrganisation ? undefined : trimmedLastName,
        organisationName: isOrganisation ? trimmedOrganisationName : undefined,
      });

      const updatedDetails: User = {
        ...(userDetails as User),
        email: trimmedEmail,
        firstName: isOrganisation ? userDetails?.firstName : trimmedFirstName,
        lastName: isOrganisation ? userDetails?.lastName : trimmedLastName,
        organisationName: isOrganisation ? trimmedOrganisationName : userDetails?.organisationName,
      };
      setUserDetails(updatedDetails);

      // Erzwingt einen frischen Access-Token mit der neuen E-Mail als "sub" - sonst schlägt
      // jeder Backend-Call mit der alten E-Mail fehl, bis das Token automatisch abläuft.
      // displayName wird im selben Aufruf mitgeschickt, sonst zeigt die NavBar bis zum
      // nächsten Login weiter den alten Namen bzw. die E-Mail-Adresse.
      await update({ email: trimmedEmail, displayName: computeDisplayName(updatedDetails) });
      setEmailSuccess("Angaben wurden aktualisiert.");
      setEditEmail(false);
    } catch (err) {
      console.error("Fehler beim Aktualisieren der Profildaten:", err);
      setEmailError(extractErrorMessage(err, "Angaben konnten nicht gespeichert werden."));
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
                  <span className="font-semibold text-text-primary">
                    {isOrganisation ? "Organisation:" : "Name:"}
                  </span>{" "}
                  {currentDisplayName === session?.user?.email ? (
                    <span className="text-text-muted italic">noch nicht angegeben</span>
                  ) : (
                    currentDisplayName
                  )}
                </p>
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
                {isOrganisation ? (
                  <TextInput
                    label="Organisationsname"
                    value={organisationName}
                    onChange={(e) => setOrganisationName(e.target.value)}
                  />
                ) : (
                  <>
                    <TextInput
                      label="Vorname"
                      autoComplete="given-name"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                    />
                    <TextInput
                      label="Nachname"
                      autoComplete="family-name"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                    />
                  </>
                )}
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
