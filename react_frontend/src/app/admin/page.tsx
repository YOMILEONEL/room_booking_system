"use client";

import * as React from "react";
import Link from "next/link";
import { useSession } from "next-auth/react";
import { fetchAdminDashboard, type AdminDashboard } from "../api/admin.api";
import { confirmPayment } from "../api/payment.api";
import { Card, Badge, Button, Alert } from "../components/ui";

const currency = new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" });

type TileTone = "success" | "warning" | "info" | "primary";

const TONE_CLASSES: Record<TileTone, string> = {
  success: "border-success/30 bg-success/[0.06]",
  warning: "border-warning/30 bg-warning/[0.06]",
  info: "border-primary/30 bg-primary/[0.06]",
  primary: "border-primary/40 bg-primary/[0.08]",
};

const TONE_VALUE_CLASSES: Record<TileTone, string> = {
  success: "text-success",
  warning: "text-warning",
  info: "text-primary",
  primary: "text-primary",
};

export default function AdminOverviewPage() {
  const { data: session } = useSession();
  const [data, setData] = React.useState<AdminDashboard | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [confirmingId, setConfirmingId] = React.useState<string | null>(null);

  const loadDashboard = React.useCallback(async () => {
    try {
      const result = await fetchAdminDashboard();
      setData(result);
    } catch (err) {
      console.error("Fehler beim Laden des Dashboards:", err);
      setError("Dashboard-Daten konnten nicht geladen werden.");
    }
  }, []);

  React.useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const handleConfirmPayment = async (paymentId: string) => {
    setConfirmingId(paymentId);
    try {
      await confirmPayment(paymentId);
      await loadDashboard();
    } catch (err) {
      console.error("Zahlung konnte nicht bestätigt werden:", err);
      alert("Zahlung konnte nicht bestätigt werden.");
    } finally {
      setConfirmingId(null);
    }
  };

  const tiles: { label: string; value: string; tone: TileTone; href: string }[] = data
    ? [
        { label: "Verfügbare Räume", value: String(data.availableRooms), tone: "success", href: "/admin/rooms" },
        { label: "Belegte Räume", value: String(data.bookedRooms), tone: "warning", href: "/admin/rooms" },
        { label: "Benutzer", value: String(data.totalUsers), tone: "info", href: "/admin/users" },
        { label: "Bevorstehende Buchungen", value: String(data.upcomingBookings), tone: "primary", href: "/admin/bookings" },
        { label: "Umsatz diesen Monat", value: currency.format(data.revenueThisMonth), tone: "primary", href: "/admin/bookings" },
      ]
    : [];

  return (
    <div className="grid gap-8">
      <div>
        <h1 className="text-2xl font-bold">Willkommen, {session?.user?.name}.</h1>
        <p className="text-sm text-text-secondary mt-1">Hier ist der aktuelle Stand von Spacio.</p>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
        {tiles.map((tile) => (
          <Link key={tile.label} href={tile.href}>
            <Card className={`h-full transition-colors hover:brightness-110 ${TONE_CLASSES[tile.tone]}`}>
              <p className="text-sm text-text-secondary mb-2">{tile.label}</p>
              <p className={`text-2xl font-bold ${TONE_VALUE_CLASSES[tile.tone]}`}>{tile.value}</p>
              <p className="text-xs text-text-muted mt-3">Details ansehen →</p>
            </Card>
          </Link>
        ))}
      </div>

      <section>
        <h2 className="text-lg font-bold mb-4">Letzte Buchungen</h2>

        {data && data.recentBookings.length === 0 && (
          <p className="text-text-muted text-sm">Noch keine Buchungen vorhanden.</p>
        )}

        {data && data.recentBookings.length > 0 && (
          <Card className="p-0 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border-subtle text-left text-text-secondary">
                    <th className="px-4 py-3 font-semibold">Raum</th>
                    <th className="px-4 py-3 font-semibold">Benutzer</th>
                    <th className="px-4 py-3 font-semibold">Zeitraum</th>
                    <th className="px-4 py-3 font-semibold">Zahlung</th>
                  </tr>
                </thead>
                <tbody>
                  {data.recentBookings.map((b) => (
                    <tr key={b.bookingId} className="border-b border-border-subtle last:border-0">
                      <td className="px-4 py-3">{b.room?.name}</td>
                      <td className="px-4 py-3">{b.user?.username}</td>
                      <td className="px-4 py-3 text-text-secondary">
                        {b.startTime} – {b.endTime}
                      </td>
                      <td className="px-4 py-3">
                        {b.payment && (
                          <Badge variant={b.payment.status === "PAID" ? "paid" : "pending"}>
                            {b.payment.status === "PAID" ? "Bezahlt" : "Offen"}
                          </Badge>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="px-4 py-3 border-t border-border-subtle">
              <Link href="/admin/bookings" className="text-sm font-medium text-primary hover:text-primary-hover transition-colors">
                Alle Buchungen ansehen →
              </Link>
            </div>
          </Card>
        )}
      </section>

      <div className="grid gap-6 lg:grid-cols-3">
        <section>
          <h2 className="text-lg font-bold mb-4">Offene Zahlungen</h2>
          <Card className="grid gap-3">
            {data && data.pendingPayments.length === 0 && (
              <p className="text-text-muted text-sm">Keine offenen Zahlungen.</p>
            )}
            {data?.pendingPayments.map((b) => (
              <div
                key={b.bookingId}
                className="flex items-center justify-between gap-3 pb-3 border-b border-border-subtle last:border-0 last:pb-0"
              >
                <div className="min-w-0">
                  <p className="font-medium truncate">{b.room?.name}</p>
                  <p className="text-xs text-text-muted truncate">{b.user?.username}</p>
                </div>
                <div className="flex flex-col items-end gap-1.5 shrink-0">
                  {b.payment && (
                    <span className="text-sm font-semibold text-primary whitespace-nowrap">
                      {currency.format(b.payment.amount)}
                    </span>
                  )}
                  <Button
                    variant="secondary"
                    className="text-xs py-1.5 px-2.5"
                    disabled={confirmingId === b.payment?.id}
                    onClick={() => b.payment && handleConfirmPayment(b.payment.id)}
                  >
                    {confirmingId === b.payment?.id ? "..." : "Bestätigen"}
                  </Button>
                </div>
              </div>
            ))}
          </Card>
        </section>

        <section>
          <h2 className="text-lg font-bold mb-4">Meistgebuchte Räume</h2>
          <Card className="grid gap-3">
            {data && data.topRooms.length === 0 && (
              <p className="text-text-muted text-sm">Noch keine Buchungen vorhanden.</p>
            )}
            {data?.topRooms.map((r, i) => (
              <div
                key={r.roomName}
                className="flex items-center justify-between gap-3 pb-3 border-b border-border-subtle last:border-0 last:pb-0"
              >
                <span className="truncate">
                  <span className="text-text-muted mr-2">{i + 1}.</span>
                  {r.roomName}
                </span>
                <Badge variant="info">{r.bookingCount}×</Badge>
              </div>
            ))}
          </Card>
        </section>

        <section>
          <h2 className="text-lg font-bold mb-4">Aktivste Kunden</h2>
          <Card className="grid gap-3">
            {data && data.topCustomers.length === 0 && (
              <p className="text-text-muted text-sm">Noch keine Buchungen vorhanden.</p>
            )}
            {data?.topCustomers.map((c, i) => (
              <div
                key={c.username}
                className="flex items-center justify-between gap-3 pb-3 border-b border-border-subtle last:border-0 last:pb-0"
              >
                <span className="truncate">
                  <span className="text-text-muted mr-2">{i + 1}.</span>
                  {c.username}
                </span>
                <Badge variant="info">{c.bookingCount}×</Badge>
              </div>
            ))}
          </Card>
        </section>
      </div>
    </div>
  );
}
