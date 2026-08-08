"use client";

import * as React from "react";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import AdminSidebar from "./AdminSidebar";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { data: session, status } = useSession();
  const router = useRouter();
  const isAdmin = session?.user?.role === "ADMIN";

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
      <AdminSidebar />

      <div className="ml-60 min-w-0">
        <header className="sticky top-0 z-40 bg-card/95 backdrop-blur border-b border-border-subtle px-6 py-4 flex items-center justify-between">
          <span className="font-semibold">Admin Dashboard</span>
          <span className="text-sm text-text-muted">{session?.user?.name}</span>
        </header>

        <main className="px-6 py-8 max-w-6xl w-full mx-auto">{children}</main>
      </div>
    </div>
  );
}
