"use client";
import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSession, signOut } from "next-auth/react";
import { ConfirmDialog } from "./ui";

export default function NavBar() {
  const router = useRouter();
  const { data: session, status } = useSession();
  const [confirmLogout, setConfirmLogout] = React.useState(false);

  const handleLogout = () => {
    signOut({ callbackUrl: "/" });
  };

  return (
    <header className="sticky top-0 z-50 bg-card/95 backdrop-blur border-b border-border-subtle">
      <div className="max-w-6xl mx-auto flex items-center justify-between gap-4 px-4 sm:px-6 py-3.5">
        <Link
          href="/"
          className="font-bold text-base sm:text-lg tracking-tight hover:text-primary transition-colors"
        >
          Raumbuchungssystem
        </Link>

        <nav className="flex items-center gap-2 sm:gap-3">
          <Link
            href="/"
            className="hidden sm:inline-block px-3 py-2 rounded-lg text-sm font-medium text-text-secondary hover:text-text-primary hover:bg-black/[0.04] transition-colors"
          >
            Startseite
          </Link>

          {status === "authenticated" ? (
            <>
              <Link
                href="/rooms"
                className="px-3 py-2 rounded-lg text-sm font-medium text-text-secondary hover:text-text-primary hover:bg-black/[0.04] transition-colors"
              >
                Räume
              </Link>
              <Link
                href="/profile"
                className="px-3 py-2 rounded-lg text-sm font-medium text-text-secondary hover:text-text-primary hover:bg-black/[0.04] transition-colors"
              >
                Profil
              </Link>
              {session?.user?.role === "ADMIN" && (
                <Link
                  href="/admin"
                  className="px-3 py-2 rounded-lg text-sm font-medium text-text-secondary hover:text-text-primary hover:bg-black/[0.04] transition-colors"
                >
                  Verwaltung
                </Link>
              )}
              <span className="hidden sm:inline text-sm text-text-muted px-1">
                {session.user?.name}
              </span>
              <button
                onClick={() => setConfirmLogout(true)}
                className="px-3.5 py-2 rounded-lg text-sm font-semibold bg-black/[0.06] hover:bg-black/[0.1] transition-colors"
              >
                Logout
              </button>
            </>
          ) : (
            <button
              onClick={() => router.push("/login")}
              className="px-3.5 py-2 rounded-lg text-sm font-semibold bg-primary hover:bg-primary-hover text-white transition-colors"
            >
              Login
            </button>
          )}
        </nav>
      </div>

      <ConfirmDialog
        open={confirmLogout}
        title="Abmelden"
        message="Möchtest du dich wirklich abmelden?"
        confirmLabel="Abmelden"
        onConfirm={handleLogout}
        onCancel={() => setConfirmLogout(false)}
      />
    </header>
  );
}
