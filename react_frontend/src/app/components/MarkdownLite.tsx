import * as React from "react";

// Gemini answers use a small, predictable Markdown subset (bold, occasional single-star
// asides, numbered lists) - not enough to justify pulling in a full Markdown/remark
// dependency. This renders just that subset; any stray single "*" left over from an emphasis
// marker we don't specially render is stripped rather than shown literally, since a bare
// asterisk in the UI reads as a rendering bug, not as content.
function renderInline(text: string): React.ReactNode[] {
  return text
    .split(/(\*\*[^*]+\*\*)/g)
    .filter((segment) => segment !== "")
    .map((segment, i) => {
      if (segment.startsWith("**") && segment.endsWith("**") && segment.length > 4) {
        return <strong key={i}>{segment.slice(2, -2)}</strong>;
      }
      return segment.replace(/\*/g, "");
    });
}

export default function MarkdownLite({ text }: { text: string }) {
  const blocks = text.trim().split(/\n\s*\n/);

  return (
    <div className="grid gap-2">
      {blocks.map((block, blockIndex) => {
        const lines = block.split("\n").filter((line) => line.trim() !== "");
        const isOrdered = lines.length > 0 && lines.every((line) => /^\d+[.)]\s+/.test(line.trim()));
        const isUnordered = !isOrdered && lines.length > 0 && lines.every((line) => /^[-*]\s+/.test(line.trim()));

        if (isOrdered) {
          return (
            <ol key={blockIndex} className="list-decimal pl-5 grid gap-1">
              {lines.map((line, lineIndex) => (
                <li key={lineIndex}>{renderInline(line.trim().replace(/^\d+[.)]\s+/, ""))}</li>
              ))}
            </ol>
          );
        }

        if (isUnordered) {
          return (
            <ul key={blockIndex} className="list-disc pl-5 grid gap-1">
              {lines.map((line, lineIndex) => (
                <li key={lineIndex}>{renderInline(line.trim().replace(/^[-*]\s+/, ""))}</li>
              ))}
            </ul>
          );
        }

        return (
          <p key={blockIndex}>
            {lines.map((line, lineIndex) => (
              <React.Fragment key={lineIndex}>
                {lineIndex > 0 && <br />}
                {renderInline(line)}
              </React.Fragment>
            ))}
          </p>
        );
      })}
    </div>
  );
}
