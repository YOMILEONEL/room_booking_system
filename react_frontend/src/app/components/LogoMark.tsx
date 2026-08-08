export default function LogoMark({ className = "w-6 h-6" }: { className?: string }) {
  return (
    <svg viewBox="0 0 96 96" className={className} fill="none" aria-hidden="true">
      <path
        d="M70 26 C70 14, 26 14, 26 30 C26 46, 70 46, 70 62 C70 78, 26 78, 26 66"
        stroke="currentColor"
        strokeWidth="12"
        strokeLinecap="round"
      />
    </svg>
  );
}
