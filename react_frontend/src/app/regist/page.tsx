"use client";
import * as React from "react";
import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { signIn } from "next-auth/react";
import NavBar from "../components/NavBar";
import { Card, Button, TextInput, Select, Alert } from "../components/ui";
import { registerUser, type CustomerType } from "../api/auth.api";
import { extractErrorMessage } from "../api/apiClient";

export default function Registration() {
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [customerType, setCustomerType] = useState<CustomerType>("KUNDE");
  const [organisationName, setOrganisationName] = useState<string>("");
  const [firstName, setFirstName] = useState<string>("");
  const [lastName, setLastName] = useState<string>("");
  const [phoneNumber, setPhoneNumber] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(false);
  const [registStatus, setRegistStatus] = useState<boolean>(false);
  const router = useRouter();

  useEffect(() => {
    if (registStatus) {
      router.push("/rooms");
    }
  }, [registStatus, router]);

  const handleAddUser = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError("");

    if (!username || !password || !phoneNumber.trim()) {
      setError("Bitte alle Felder ausfüllen.");
      return;
    }
    if (customerType === "ORGANISATION" && !organisationName.trim()) {
      setError("Bitte den Namen der Organisation angeben.");
      return;
    }
    if (customerType === "KUNDE" && (!firstName.trim() || !lastName.trim())) {
      setError("Bitte Vor- und Nachnamen angeben.");
      return;
    }

    setLoading(true);

    try {
      await registerUser({
        username,
        password,
        customerType,
        organisationName: customerType === "ORGANISATION" ? organisationName.trim() : undefined,
        firstName: customerType === "KUNDE" ? firstName.trim() : undefined,
        lastName: customerType === "KUNDE" ? lastName.trim() : undefined,
        phoneNumber: phoneNumber.trim(),
      });

      // direkt einloggen, damit sofort eine Session existiert
      const result = await signIn("credentials", {
        username,
        password,
        redirect: false,
      });
      if (!result?.error) {
        setRegistStatus(true);
      } else {
        setError("Registrierung erfolgreich, Login fehlgeschlagen. Bitte manuell einloggen.");
        router.push("/login");
      }
    } catch (err) {
      console.error("Error:", err);
      setError(extractErrorMessage(err, "Registrierung fehlgeschlagen."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <NavBar />
      <div className="max-w-md mx-auto px-4 py-16">
        <Card>
          <h1 className="text-2xl font-bold mb-6">Konto erstellen</h1>

          <form onSubmit={handleAddUser} className="grid gap-4">
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
              autoComplete="new-password"
            />

            <Select
              label="Kontotyp"
              value={customerType}
              onChange={(e) => setCustomerType(e.target.value as CustomerType)}
            >
              <option value="KUNDE">Kunde</option>
              <option value="ORGANISATION">Organisation</option>
            </Select>

            {customerType === "ORGANISATION" ? (
              <TextInput
                label="Organisationsname"
                value={organisationName}
                onChange={(e) => setOrganisationName(e.target.value)}
              />
            ) : (
              <>
                <TextInput
                  label="Vorname"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  autoComplete="given-name"
                />
                <TextInput
                  label="Nachname"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  autoComplete="family-name"
                />
              </>
            )}

            <TextInput
              label="Telefonnummer"
              type="tel"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              autoComplete="tel"
            />

            {error && <Alert variant="danger">{error}</Alert>}

            <Button type="submit" disabled={loading} className="w-full mt-2">
              {loading ? "Wird registriert..." : "Registrieren"}
            </Button>
          </form>

          <p className="text-sm text-text-muted mt-6 text-center">
            Schon ein Konto?{" "}
            <Link href="/login" className="text-primary hover:text-primary-hover font-medium">
              Jetzt einloggen
            </Link>
          </p>
        </Card>
      </div>
    </div>
  );
}
