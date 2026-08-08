"use client";

import * as React from "react";

export function Card({
  className = "",
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div
      className={`bg-card border border-border-subtle rounded-2xl p-6 ${className}`}
    >
      {children}
    </div>
  );
}

type ButtonVariant = "primary" | "secondary" | "danger";

export function Button({
  variant = "primary",
  className = "",
  children,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant }) {
  const variants: Record<ButtonVariant, string> = {
    primary:
      "bg-primary hover:bg-primary-hover text-white disabled:bg-primary/40",
    secondary:
      "bg-transparent border border-border-subtle text-text-primary hover:bg-card-hover disabled:opacity-40",
    danger:
      "bg-danger/10 border border-danger/30 text-danger hover:bg-danger/20 disabled:opacity-40",
  };

  return (
    <button
      className={`px-4 py-2.5 rounded-xl font-semibold text-sm transition-colors disabled:cursor-not-allowed ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}

export function TextInput({
  label,
  className = "",
  ...props
}: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return (
    <label className="grid gap-1.5">
      <span className="text-sm font-semibold text-text-secondary">{label}</span>
      <input
        className={`px-3.5 py-2.5 rounded-xl border border-border-subtle bg-black/[0.03] text-text-primary placeholder:text-text-muted outline-none focus:border-primary/60 focus:ring-2 focus:ring-primary/20 ${className}`}
        {...props}
      />
    </label>
  );
}

export function Textarea({
  label,
  className = "",
  ...props
}: React.TextareaHTMLAttributes<HTMLTextAreaElement> & { label: string }) {
  return (
    <label className="grid gap-1.5">
      <span className="text-sm font-semibold text-text-secondary">{label}</span>
      <textarea
        rows={4}
        className={`px-3.5 py-2.5 rounded-xl border border-border-subtle bg-black/[0.03] text-text-primary placeholder:text-text-muted outline-none focus:border-primary/60 focus:ring-2 focus:ring-primary/20 resize-y ${className}`}
        {...props}
      />
    </label>
  );
}

export function Select({
  label,
  className = "",
  children,
  ...props
}: React.SelectHTMLAttributes<HTMLSelectElement> & { label: string }) {
  return (
    <label className="grid gap-1.5">
      <span className="text-sm font-semibold text-text-secondary">{label}</span>
      <select
        className={`px-3.5 py-2.5 rounded-xl border border-border-subtle bg-card text-text-primary outline-none focus:border-primary/60 focus:ring-2 focus:ring-primary/20 ${className}`}
        {...props}
      >
        {children}
      </select>
    </label>
  );
}

type BadgeVariant = "pending" | "paid" | "success" | "danger" | "neutral" | "info";

export function Badge({
  variant = "neutral",
  children,
}: {
  variant?: BadgeVariant;
  children: React.ReactNode;
}) {
  const variants: Record<BadgeVariant, string> = {
    pending: "bg-warning/10 text-warning border-warning/30",
    paid: "bg-success/10 text-success border-success/30",
    success: "bg-success/10 text-success border-success/30",
    danger: "bg-danger/10 text-danger border-danger/30",
    info: "bg-primary/10 text-primary border-primary/30",
    neutral: "bg-black/[0.05] text-text-secondary border-border-subtle",
  };

  return (
    <span
      className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${variants[variant]}`}
    >
      {children}
    </span>
  );
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = "Bestätigen",
  cancelLabel = "Abbrechen",
  onConfirm,
  onCancel,
}: {
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center px-4">
      <div className="absolute inset-0 bg-black/40" onClick={onCancel} />
      <div className="relative bg-card border border-border-subtle rounded-2xl p-6 w-full max-w-sm grid gap-4 shadow-xl">
        <h2 className="text-lg font-bold">{title}</h2>
        <p className="text-sm text-text-secondary">{message}</p>
        <div className="flex justify-end gap-3">
          <Button variant="secondary" onClick={onCancel} className="text-sm py-2">
            {cancelLabel}
          </Button>
          <Button variant="danger" onClick={onConfirm} className="text-sm py-2">
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}

export function Alert({
  variant = "danger",
  children,
}: {
  variant?: "danger" | "success" | "info";
  children: React.ReactNode;
}) {
  const variants: Record<string, string> = {
    danger: "border-danger/30 bg-danger/10 text-danger",
    success: "border-success/30 bg-success/10 text-success",
    info: "border-border-subtle bg-black/[0.03] text-text-secondary",
  };

  return (
    <div className={`rounded-xl border px-4 py-3 text-sm font-medium ${variants[variant]}`}>
      {children}
    </div>
  );
}
