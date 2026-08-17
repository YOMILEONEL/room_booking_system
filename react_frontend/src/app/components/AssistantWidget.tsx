"use client";

import * as React from "react";
import { useSession } from "next-auth/react";
import { usePathname } from "next/navigation";
import AssistantChat from "./AssistantChat";
import BotIcon from "./BotIcon";

// Mounted once in the root layout (see layout.tsx) so it floats over every page - except
// /assistant itself, which is already the full chat experience and would just duplicate this.
// Kunde/Organisation only, same product decision as the NavBar link and api/assistant/route.ts:
// admins already see everything via the Verwaltung-Dashboard.
export default function AssistantWidget() {
  const { data: session, status } = useSession();
  const pathname = usePathname();
  const [open, setOpen] = React.useState(false);

  const isEligible = status === "authenticated" && session?.user?.role !== "ADMIN";
  const onAssistantPage = pathname === "/assistant";

  if (!isEligible || onAssistantPage) {
    return null;
  }

  return (
    <div className="fixed bottom-5 right-5 z-40 flex flex-col items-end gap-3">
      {open && (
        <div className="w-[calc(100vw-2.5rem)] max-w-sm h-[28rem] max-h-[70vh] bg-card border border-border-subtle rounded-2xl shadow-2xl overflow-hidden flex flex-col">
          <div className="flex items-center justify-between px-4 py-3 border-b border-border-subtle shrink-0">
            <span className="font-semibold text-sm flex items-center gap-2">
              <BotIcon className="w-5 h-5 text-primary" />
              KI-Assistent
            </span>
            <button
              type="button"
              onClick={() => setOpen(false)}
              aria-label="KI-Assistent schließen"
              className="text-text-secondary hover:text-text-primary transition-colors text-lg leading-none px-1"
            >
              ×
            </button>
          </div>
          <div className="flex-1 min-h-0">
            <AssistantChat compact />
          </div>
        </div>
      )}

      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-label={open ? "KI-Assistent schließen" : "KI-Assistent öffnen"}
        aria-expanded={open}
        className="w-14 h-14 rounded-full bg-primary hover:bg-primary-hover text-white shadow-xl flex items-center justify-center transition-colors"
      >
        {open ? <span className="text-2xl leading-none">×</span> : <BotIcon className="w-7 h-7" />}
      </button>
    </div>
  );
}
