"use client";
import * as React from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import NavBar from "./NavBar";
import Footer from "./Footer";
import { Button, Badge } from "./ui";

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
    text: "Verfügbarkeit, Kapazität und Preis pro Tag auf einen Blick.",
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
  const videoRef = React.useRef<HTMLVideoElement>(null);
  const [muted, setMuted] = React.useState(true);

  const toggleSound = () => {
    const video = videoRef.current;
    if (!video) return;
    video.muted = !video.muted;
    setMuted(video.muted);
  };

  return (
    <div>
      <NavBar />

      <section className="relative overflow-hidden min-h-[480px] sm:min-h-[560px] flex items-center">
        <video
          ref={videoRef}
          className="absolute inset-0 w-full h-full object-cover motion-reduce:hidden"
          autoPlay
          loop
          muted
          playsInline
          poster="/room/Konf.jpeg"
        >
          <source src="/video/office-hero.mp4" type="video/mp4" />
        </video>
        <div className="absolute inset-0 bg-gradient-to-b from-black/65 via-black/55 to-black/70" />

        <button
          type="button"
          onClick={toggleSound}
          aria-label={muted ? "Ton einschalten" : "Ton ausschalten"}
          className="motion-reduce:hidden absolute bottom-4 right-4 sm:bottom-6 sm:right-6 z-10 w-10 h-10 rounded-full bg-black/40 hover:bg-black/55 border border-white/30 text-white flex items-center justify-center transition-colors"
        >
          {muted ? (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
              <line x1="23" y1="9" x2="17" y2="15" />
              <line x1="17" y1="9" x2="23" y2="15" />
            </svg>
          ) : (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
              <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
              <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
            </svg>
          )}
        </button>

        <div className="relative w-full max-w-6xl mx-auto px-4 sm:px-6 py-16 text-center">
          <p className="text-xs sm:text-sm font-semibold tracking-[0.14em] uppercase text-white/70 mb-4">
            Für Privatpersonen &amp; Organisationen
          </p>
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight mb-4 text-white">
            Der richtige Raum, wann und wo Sie ihn brauchen
          </h1>
          <p className="text-white/85 text-base sm:text-lg max-w-2xl mx-auto mb-8">
            Ob privat für eine Feier oder als Unternehmen für regelmäßige Meetings —
            ob große Stadt oder kleiner Standort: Spacio zeigt freie Räume in Echtzeit,
            zu fairen Preisen, mit einem Klick reserviert.
          </p>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <Button onClick={() => router.push(status === "authenticated" ? "/rooms" : "/login")}>
              Jetzt buchen
            </Button>
            {status !== "authenticated" && (
              <button
                type="button"
                onClick={() => router.push("/login?mode=register")}
                className="px-4 py-2.5 rounded-xl font-semibold text-sm border border-white/50 text-white hover:bg-white/10 transition-colors"
              >
                Konto erstellen
              </button>
            )}
          </div>
        </div>
      </section>

      <section className="max-w-6xl mx-auto px-4 sm:px-6 py-16">
        <div className="max-w-2xl mb-10">
          <h2 className="text-2xl sm:text-3xl font-bold tracking-tight mb-3">
            Eine Plattform, zwei Wege zum passenden Raum
          </h2>
          <p className="text-text-secondary text-base leading-relaxed">
            Räume suchen dauert normalerweise: anrufen, warten, hoffen, dass noch etwas frei
            ist. Spacio dreht das um — freie Räume sehen Sie sofort und buchen sie direkt
            selbst, egal ob Sie einmalig einen Raum brauchen oder als Organisation
            regelmäßig planen.
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div className="bg-card border border-border-subtle rounded-2xl p-6">
            <h3 className="font-semibold text-lg mb-2">Für Privatpersonen</h3>
            <p className="text-sm text-text-secondary leading-relaxed">
              Ob Familienfeier, Workshop oder Lerngruppe — finden Sie den passenden Raum in
              Ihrer Nähe, ganz ohne Anruf oder Wartezeit, und buchen Sie ihn direkt zum
              angezeigten Preis.
            </p>
          </div>
          <div className="bg-card border border-border-subtle rounded-2xl p-6">
            <div className="flex items-center gap-2 mb-2">
              <h3 className="font-semibold text-lg">Für Organisationen</h3>
              <Badge variant="info">-10 % automatisch</Badge>
            </div>
            <p className="text-sm text-text-secondary leading-relaxed">
              Ob großes Unternehmen in der Metropole oder kleines Team in der Kreisstadt:
              Als registrierte Organisation buchen Sie Besprechungs- und Schulungsräume zum
              Vorzugspreis — 10 % Rabatt automatisch auf jede Buchung, ganz ohne Verhandlung.
            </p>
          </div>
        </div>
      </section>

      <section className="max-w-6xl mx-auto px-4 sm:px-6 pb-16">
        <h2 className="text-xl font-bold tracking-tight mb-6">Räume für jeden Anlass</h2>
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

      <Footer />
    </div>
  );
}
