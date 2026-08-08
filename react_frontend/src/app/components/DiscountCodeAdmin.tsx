"use client";

import React, { useEffect, useState } from "react";
import {
  fetchDiscountCodes,
  createDiscountCode,
  deleteDiscountCode,
  type DiscountCode,
  type DiscountType,
} from "../api/discountCode.api";
import { Card, Button, TextInput, Select, Badge, Alert } from "./ui";

const TYPES: DiscountType[] = ["PERCENT", "ABSOLUTE"];

export default function DiscountCodeAdmin() {
  const [codes, setCodes] = useState<DiscountCode[]>([]);
  const [error, setError] = useState<string | null>(null);

  const [code, setCode] = useState("");
  const [type, setType] = useState<DiscountType>("PERCENT");
  const [value, setValue] = useState<number | "">("");
  const [validFrom, setValidFrom] = useState("");
  const [validUntil, setValidUntil] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const loadCodes = async () => {
    try {
      setError(null);
      const data = await fetchDiscountCodes();
      setCodes(data);
    } catch (err) {
      console.error("Fehler beim Laden der Rabattcodes:", err);
      setError("Rabattcodes konnten nicht geladen werden.");
    }
  };

  useEffect(() => {
    loadCodes();
  }, []);

  const handleCreate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setFormError(null);

    if (!code || value === "" || value <= 0 || !validFrom || !validUntil) {
      setFormError("Bitte alle Felder korrekt ausfüllen.");
      return;
    }

    try {
      await createDiscountCode({ code, type, value, validFrom, validUntil, active: true });
      setCode("");
      setValue("");
      setValidFrom("");
      setValidUntil("");
      await loadCodes();
    } catch (err) {
      console.error("Fehler beim Erstellen des Rabattcodes:", err);
      setFormError("Rabattcode konnte nicht erstellt werden.");
    }
  };

  const handleDelete = async (id: string) => {
    const confirmed = window.confirm("Rabattcode wirklich löschen?");
    if (!confirmed) return;

    try {
      await deleteDiscountCode(id);
      setCodes((prev) => prev.filter((c) => c.id !== id));
    } catch (err) {
      console.error("Fehler beim Löschen des Rabattcodes:", err);
      alert("Rabattcode konnte nicht gelöscht werden.");
    }
  };

  return (
    <div className="grid gap-6">
      <Card>
        <h2 className="text-lg font-bold mb-4">Rabattcode erstellen</h2>

        <form onSubmit={handleCreate} className="grid gap-4 sm:grid-cols-2">
          <TextInput label="Code" value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} />

          <Select label="Typ" value={type} onChange={(e) => setType(e.target.value as DiscountType)}>
            {TYPES.map((t) => (
              <option key={t} value={t}>
                {t === "PERCENT" ? "Prozent" : "Fester Betrag"}
              </option>
            ))}
          </Select>

          <TextInput
            label={type === "PERCENT" ? "Wert (%)" : "Wert (€)"}
            type="number"
            min={0}
            step="0.01"
            value={value}
            onChange={(e) => setValue(e.target.value === "" ? "" : Number(e.target.value))}
          />

          <div />

          <TextInput
            label="Gültig ab"
            type="date"
            value={validFrom}
            onChange={(e) => setValidFrom(e.target.value)}
          />

          <TextInput
            label="Gültig bis"
            type="date"
            value={validUntil}
            onChange={(e) => setValidUntil(e.target.value)}
          />

          <div className="sm:col-span-2 grid gap-3">
            {formError && <Alert variant="danger">{formError}</Alert>}
            <Button type="submit" className="w-full sm:w-auto justify-self-start">
              Rabattcode erstellen
            </Button>
          </div>
        </form>
      </Card>

      <div>
        <h2 className="text-lg font-bold mb-4">Rabattcodes</h2>
        {error && <Alert variant="danger">{error}</Alert>}
        {codes.length === 0 && !error && (
          <p className="text-text-muted text-sm">Keine Rabattcodes vorhanden.</p>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {codes.map((c) => (
            <Card key={c.id}>
              <div className="flex items-start justify-between gap-2 mb-2">
                <h3 className="font-semibold">{c.code}</h3>
                <Badge variant={c.active ? "success" : "neutral"}>
                  {c.active ? "Aktiv" : "Inaktiv"}
                </Badge>
              </div>
              <p className="text-sm text-text-secondary">
                {c.type === "PERCENT" ? `${c.value}%` : `${c.value} €`} Rabatt
              </p>
              <p className="text-sm text-text-muted">
                Gültig: {c.validFrom} – {c.validUntil}
              </p>
              <button
                onClick={() => handleDelete(c.id)}
                className="text-sm font-medium text-danger hover:text-danger/80 transition-colors mt-2"
              >
                Löschen
              </button>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
