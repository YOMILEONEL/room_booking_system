"use client";

import { SessionProvider } from "next-auth/react";
import AssistantWidget from "./components/AssistantWidget";

export default function Providers({ children }: { children: React.ReactNode }) {
  return (
    <SessionProvider>
      {children}
      <AssistantWidget />
    </SessionProvider>
  );
}
