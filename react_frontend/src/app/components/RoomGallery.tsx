"use client";

import * as React from "react";

interface RoomGalleryProps {
  images: string[];
  alt: string;
}

export default function RoomGallery({ images, alt }: RoomGalleryProps) {
  const [activeIndex, setActiveIndex] = React.useState(0);

  const sources = images.length > 0 ? images : [];
  const clampedIndex = Math.min(activeIndex, Math.max(sources.length - 1, 0));

  if (sources.length === 0) {
    return null;
  }

  const showNav = sources.length > 1;

  const goPrev = () => setActiveIndex((i) => (i - 1 + sources.length) % sources.length);
  const goNext = () => setActiveIndex((i) => (i + 1) % sources.length);

  return (
    <div>
      <div className="relative">
        <img
          src={sources[clampedIndex]}
          alt={alt}
          className="w-full h-64 object-cover"
        />
        {showNav && (
          <>
            <button
              type="button"
              onClick={goPrev}
              aria-label="Vorheriges Bild"
              className="absolute left-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-black/40 hover:bg-black/60 text-white flex items-center justify-center text-lg transition-colors"
            >
              ‹
            </button>
            <button
              type="button"
              onClick={goNext}
              aria-label="Nächstes Bild"
              className="absolute right-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-black/40 hover:bg-black/60 text-white flex items-center justify-center text-lg transition-colors"
            >
              ›
            </button>
          </>
        )}
      </div>

      {showNav && (
        <div className="flex gap-2 p-3 overflow-x-auto">
          {sources.map((src, i) => (
            <button
              key={i}
              type="button"
              onClick={() => setActiveIndex(i)}
              aria-label={`Bild ${i + 1} von ${sources.length} anzeigen`}
              aria-current={i === clampedIndex}
              className={`shrink-0 w-16 h-16 rounded-lg overflow-hidden border-2 transition-colors ${
                i === clampedIndex ? "border-primary" : "border-transparent"
              }`}
            >
              <img src={src} alt="" className="w-full h-full object-cover" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
