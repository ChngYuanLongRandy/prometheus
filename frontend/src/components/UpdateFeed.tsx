import type { UpdateItem } from "../types";

interface Props {
  updates: UpdateItem[];
}

function DiffPreview({ diff }: { diff: string }) {
  // Show the meaningful diff lines (added/removed), capped for preview.
  const lines = diff
    .split("\n")
    .filter((l) => /^[+-]/.test(l) && !/^(\+\+\+|---)/.test(l))
    .slice(0, 20);

  if (lines.length === 0) return null;

  return (
    <pre className="diff">
      {lines.map((l, i) => (
        <div
          key={i}
          className={l.startsWith("+") ? "diff-add" : "diff-del"}
        >
          {l}
        </div>
      ))}
    </pre>
  );
}

export function UpdateFeed({ updates }: Props) {
  if (updates.length === 0) {
    return <p className="empty">No changes detected yet.</p>;
  }

  return (
    <ul className="update-feed">
      {updates.map((u) => (
        <li className="update-row" key={u.id}>
          <div className="update-head">
            <a className="url-link" href={u.url} target="_blank" rel="noreferrer">
              {u.label || u.url}
            </a>
            <span className="update-time">
              {new Date(u.detectedAt).toLocaleString()}
            </span>
          </div>
          <div className="update-counts">
            <span className="count-add">+{u.addedLines}</span>
            <span className="count-del">−{u.removedLines}</span>
          </div>
          <DiffPreview diff={u.diff} />
        </li>
      ))}
    </ul>
  );
}
