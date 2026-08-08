import Link from "next/link";
import LogoMark from "./LogoMark";

const LEGAL_LINKS = [
  { href: "/impressum", label: "Impressum" },
  { href: "/datenschutz", label: "Datenschutz" },
  { href: "/agb", label: "AGB" },
  { href: "/widerruf", label: "Widerruf" },
];

export default function Footer() {
  return (
    <footer className="border-t border-border-subtle mt-16">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <nav className="flex flex-wrap gap-x-5 gap-y-2">
          {LEGAL_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="text-sm text-text-secondary hover:text-primary transition-colors"
            >
              {link.label}
            </Link>
          ))}
        </nav>
        <p className="flex items-center gap-2 text-sm text-text-muted">
          <LogoMark className="w-4 h-4 text-primary" />
          © {new Date().getFullYear()} Spacio
        </p>
      </div>
    </footer>
  );
}
