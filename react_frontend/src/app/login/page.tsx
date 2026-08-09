"use client";

import * as React from "react";
import { Suspense, useState } from "react";
import Link from "next/link";
import { signIn, getSession } from "next-auth/react";
import { useRouter, useSearchParams } from "next/navigation";
import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import { Card, Button, TextInput, Select, Alert } from "../components/ui";
import { registerUser, type CustomerType } from "../api/auth.api";
import { extractErrorMessage } from "../api/apiClient";

type Mode = "login" | "register";

function AuthForm() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [mode, setMode] = useState<Mode>(searchParams.get("mode") === "register" ? "register" : "login");

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [customerType, setCustomerType] = useState<CustomerType>("KUNDE");
  const [organisationName, setOrganisationName] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const switchMode = (next: Mode) => {
    setMode(next);
    setError("");
  };

  const handleLogin = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError("");

    if (!email || !password) {
      setError("Bitte E-Mail-Adresse und Passwort eingeben.");
      return;
    }

    setLoading(true);
    const result = await signIn("credentials", { email, password, redirect: false });
    setLoading(false);

    if (result?.error) {
      console.error("Login-Fehler:", result.error);
      setError("E-Mail-Adresse oder Passwort falsch!");
      return;
    }

    // Admins landen im Admin-Dashboard, Kunden auf der Startseite.
    const session = await getSession();
    router.push(session?.user?.role === "ADMIN" ? "/admin" : "/");
  };

  const handleRegister = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError("");

    if (!email || !password || !phoneNumber.trim()) {
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
        email,
        password,
        customerType,
        organisationName: customerType === "ORGANISATION" ? organisationName.trim() : undefined,
        firstName: customerType === "KUNDE" ? firstName.trim() : undefined,
        lastName: customerType === "KUNDE" ? lastName.trim() : undefined,
        phoneNumber: phoneNumber.trim(),
      });

      // direkt einloggen, damit sofort eine Session existiert
      const result = await signIn("credentials", { email, password, redirect: false });
      if (result?.error) {
        setError("Registrierung erfolgreich, Login fehlgeschlagen. Bitte manuell einloggen.");
        switchMode("login");
        return;
      }
      router.push("/");
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
          <h1 className="text-2xl font-bold mb-6">{mode === "login" ? "Willkommen zurück" : "Konto erstellen"}</h1>

          <div className="flex gap-2 mb-6">
            <button
              type="button"
              disabled={loading}
              onClick={() => switchMode("login")}
              className={`flex-1 py-2.5 rounded-xl text-sm font-semibold border transition-colors ${
                mode === "login"
                  ? "bg-primary/10 border-primary/30 text-primary"
                  : "border-border-subtle text-text-secondary hover:text-text-primary"
              }`}
            >
              Login
            </button>
            <button
              type="button"
              disabled={loading}
              onClick={() => switchMode("register")}
              className={`flex-1 py-2.5 rounded-xl text-sm font-semibold border transition-colors ${
                mode === "register"
                  ? "bg-primary/10 border-primary/30 text-primary"
                  : "border-border-subtle text-text-secondary hover:text-text-primary"
              }`}
            >
              Registrieren
            </button>
          </div>

          {mode === "login" ? (
            <form onSubmit={handleLogin} className="grid gap-4">
              <TextInput
                label="E-Mail"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
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
          ) : (
            <form onSubmit={handleRegister} className="grid gap-4">
              <TextInput
                label="E-Mail"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
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
          )}
        </Card>
      </div>

      <Footer />
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <AuthForm />
    </Suspense>
  );
}
