"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { signOut } from "next-auth/react";
import { ConfirmDialog } from "../components/ui";

const links = [
  { href: "/admin", label: "Übersicht" },
  { href: "/admin/rooms", label: "Räume" },
  { href: "/admin/bookings", label: "Buchungen" },
  { href: "/admin/users", label: "Benutzer" },
  { href: "/admin/discount-codes", label: "Rabattcodes" },
];

export default function AdminSidebar({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const pathname = usePathname();
  const [confirmLogout, setConfirmLogout] = React.useState(false);

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 bg-black/40 z-40 md:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={`w-60 fixed inset-y-0 left-0 z-50 border-r border-border-subtle bg-card flex flex-col overflow-y-auto transition-transform duration-200 md:translate-x-0 ${
          open ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="px-5 py-5 border-b border-border-subtle">
          <span className="font-bold text-lg tracking-tight">Admin</span>
        </div>

        <nav className="flex-1 px-3 py-4 grid gap-1">
          {links.map((link) => {
            const active = pathname === link.href;
            return (
              <Link
                key={link.href}
                href={link.href}
                onClick={onClose}
                className={`px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  active
                    ? "bg-primary/10 text-primary"
                    : "text-text-secondary hover:text-text-primary hover:bg-black/[0.04]"
                }`}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>

        <div className="px-3 py-4 border-t border-border-subtle grid gap-1">
          <Link
            href="/"
            onClick={onClose}
            className="px-3 py-2.5 rounded-lg text-sm font-medium text-text-secondary hover:text-text-primary hover:bg-black/[0.04] transition-colors"
          >
            Zur Website
          </Link>
          <button
            onClick={() => setConfirmLogout(true)}
            className="px-3 py-2.5 rounded-lg text-sm font-medium text-left text-text-secondary hover:text-text-primary hover:bg-black/[0.04] transition-colors"
          >
            Logout
          </button>
        </div>

        <ConfirmDialog
          open={confirmLogout}
          title="Abmelden"
          message="Möchtest du dich wirklich abmelden?"
          confirmLabel="Abmelden"
          onConfirm={() => signOut({ callbackUrl: "/" })}
          onCancel={() => setConfirmLogout(false)}
        />
      </aside>
    </>
  );
}
