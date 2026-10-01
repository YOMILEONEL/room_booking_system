"use client";
import * as React from "react";

type AvatarProps = {
  src?: string | null;
  name: string;
  size?: number;
  // true, wenn der Name direkt daneben als Text steht (dann alt="" gegen doppelte Ansage).
  decorative?: boolean;
};

export default function Avatar({ src, name, size = 28, decorative = true }: AvatarProps) {
  // Merkt sich die URL, die nicht geladen werden konnte. Vergleich statt Reset im Effect:
  // eine neue URL wird automatisch wieder versucht.
  const [failedSrc, setFailedSrc] = React.useState<string | null>(null);
  const showImage = !!src && src !== failedSrc;
  const initial = name.trim().charAt(0).toUpperCase() || "?";
  const style: React.CSSProperties = { width: size, height: size };

  if (showImage) {
    return (
      <img
        src={src}
        alt={decorative ? "" : name}
        style={style}
        className="rounded-full object-cover shrink-0"
        onError={() => setFailedSrc(src)}
      />
    );
  }

  return (
    <span
      aria-hidden={decorative ? true : undefined}
      style={{ ...style, fontSize: Math.round(size * 0.45) }}
      className="inline-flex items-center justify-center rounded-full bg-primary text-white font-semibold shrink-0 select-none"
    >
      {initial}
    </span>
  );
}
