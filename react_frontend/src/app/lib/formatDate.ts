const displayDateFormatter = new Intl.DateTimeFormat("de-DE", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});

// Backend dates are plain "YYYY-MM-DD" (LocalDate, no timezone). Building `new Date(isoDate)`
// directly parses that as UTC midnight, which display formatting would then render in the
// browser's local zone - shifting the shown date by a day west of UTC, the same class of bug
// fixed for booking status in docs/code-review.md, 6.2. Parsing the components manually and
// constructing a local Date avoids that entirely.
export function formatLocalDate(isoDate: string): string {
  const [year, month, day] = isoDate.split("-").map(Number);
  return displayDateFormatter.format(new Date(year, month - 1, day));
}
