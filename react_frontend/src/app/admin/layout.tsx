"use client";

import * as React from "react";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import AdminSidebar from "./AdminSidebar";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { data: session, status } = useSession();
  const router = useRouter();
  const isAdmin = session?.user?.role === "ADMIN";
  const [sidebarOpen, setSidebarOpen] = React.useState(false);

  React.useEffect(() => {
    if (status === "unauthenticated") {
      router.push("/login");
    } else if (status === "authenticated" && !isAdmin) {
      router.push("/rooms");
    }
  }, [status, isAdmin, router]);

  if (status === "loading" || status === "unauthenticated" || !isAdmin) {
    return (
      <div className="flex justify-center mt-24">
        <p className="text-text-muted">Lädt...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <AdminSidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="md:ml-60 min-w-0">
        <header className="sticky top-0 z-30 bg-card/95 backdrop-blur border-b border-border-subtle px-4 sm:px-6 py-4 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setSidebarOpen(true)}
              className="md:hidden p-2 -ml-2 rounded-lg text-text-secondary hover:text-text-primary hover:bg-black/[0.04] transition-colors"
              aria-label="Menü öffnen"
            >
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <path d="M3 5h14M3 10h14M3 15h14" />
              </svg>
            </button>
            <span className="font-semibold">Admin Dashboard</span>
          </div>
          <span className="text-sm text-text-muted truncate">{session?.user?.name}</span>
        </header>

        <main className="px-4 sm:px-6 py-8 max-w-6xl w-full mx-auto">{children}</main>
      </div>
    </div>
  );
}
