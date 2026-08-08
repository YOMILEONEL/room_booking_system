"use client";
import * as React from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import NavBar from "./NavBar";
import { Button } from "./ui";

const showcaseImages = [
  { src: "/room/Konf.jpeg", alt: "Konferenzraum" },
  { src: "/room/room1.jpeg", alt: "Besprechungsraum" },
  { src: "/room/vorlesungssaal-epsilon.jpg", alt: "Vorlesungssaal" },
  { src: "/room/Party.jpeg", alt: "Veranstaltungsraum" },
  { src: "/room/Mathe.jpeg", alt: "Seminarraum" },
  { src: "/room/big.jpg", alt: "Großer Saal" },
];

const features = [
  {
    title: "Räume in Echtzeit",
    text: "Verfügbarkeit, Kapazität und Preis pro Nacht auf einen Blick.",
  },
  {
    title: "Sichere Buchung",
    text: "Anmeldung per Konto, keine doppelt vergebenen Räume dank Überschneidungsprüfung.",
  },
  {
    title: "Transparente Preise",
    text: "Rabattcodes werden direkt bei der Buchung berücksichtigt.",
  },
];

export default function Start() {
  const router = useRouter();
  const { status } = useSession();

  return (
    <div>
      <NavBar />

      <section className="max-w-6xl mx-auto px-4 sm:px-6 pt-16 pb-12 text-center">
        <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight mb-4">
          Räume buchen, ohne Umwege
        </h1>
        <p className="text-text-secondary text-base sm:text-lg max-w-2xl mx-auto mb-8">
          Planen und buchen Sie Besprechungs-, Veranstaltungs- und Schulungsräume
          einfach und effizient — mit Echtzeit-Verfügbarkeit und transparenten Preisen.
        </p>
        <div className="flex flex-wrap items-center justify-center gap-3">
          <Button onClick={() => router.push(status === "authenticated" ? "/rooms" : "/login")}>
            Jetzt buchen
          </Button>
          {status !== "authenticated" && (
            <Button variant="secondary" onClick={() => router.push("/login?mode=register")}>
              Konto erstellen
            </Button>
          )}
        </div>
      </section>

      <section className="max-w-6xl mx-auto px-4 sm:px-6 pb-16">
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 sm:gap-4">
          {showcaseImages.map((img) => (
            <div
              key={img.src}
              className="aspect-[4/3] rounded-2xl overflow-hidden border border-border-subtle group"
            >
              <img
                src={img.src}
                alt={img.alt}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
              />
            </div>
          ))}
        </div>
      </section>

      <section className="max-w-6xl mx-auto px-4 sm:px-6 pb-20 grid gap-4 sm:grid-cols-3">
        {features.map((feature) => (
          <div
            key={feature.title}
            className="bg-card border border-border-subtle rounded-2xl p-5"
          >
            <h3 className="font-semibold mb-1.5">{feature.title}</h3>
            <p className="text-sm text-text-secondary">{feature.text}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
