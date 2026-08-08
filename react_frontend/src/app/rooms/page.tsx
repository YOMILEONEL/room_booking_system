"use client";

import * as React from "react";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import RoomTable from "../components/RoomTable";

export default function RoomsPage() {
  const { status } = useSession();
  const router = useRouter();

  React.useEffect(() => {
    if (status === "unauthenticated") {
      router.push("/login");
    }
  }, [status, router]);

  if (status === "loading" || status === "unauthenticated") {
    return (
      <div className="flex justify-center mt-24">
        <p className="text-text-muted">Lädt...</p>
      </div>
    );
  }

  return (
    <div>
      <NavBar />
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8">
        <RoomTable />
      </div>

      <Footer />
    </div>
  );
}
