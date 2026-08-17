"use client";

import * as React from "react";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import AssistantChat from "../components/AssistantChat";
import { Card } from "../components/ui";

export default function AssistantPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const isAdmin = session?.user?.role === "ADMIN";

  React.useEffect(() => {
    if (status === "unauthenticated") {
      router.push("/login");
    } else if (status === "authenticated" && isAdmin) {
      // Product decision, not a security boundary - see api/assistant/route.ts. Admins get
      // redirected the same way non-admins get redirected out of /admin.
      router.push("/");
    }
  }, [status, isAdmin, router]);

  if (status === "loading" || status === "unauthenticated" || isAdmin) {
    return (
      <div className="flex justify-center mt-24">
        <p className="text-text-muted">Lädt...</p>
      </div>
    );
  }

  return (
    <div>
      <NavBar />

      <div className="max-w-2xl mx-auto px-4 sm:px-6 py-8 grid gap-4">
        <div>
          <h1 className="text-xl font-bold">KI-Assistent</h1>
          <p className="text-sm text-text-secondary mt-1">
            Frag nach deinen Buchungen oder den Räumen — die Antworten kommen live aus deinen echten Daten.
          </p>
        </div>

        <Card className="p-0 overflow-hidden">
          <AssistantChat />
        </Card>
      </div>

      <Footer />
    </div>
  );
}
