import { useState } from "react";
import type { UpdateItem } from "../types";

interface Props {
  updates: UpdateItem[];
  onGenerateAudio: (id: string) => Promise<void>;
}

function DiffPreview({ diff }: { diff: string }) {
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

export function UpdateFeed({ updates, onGenerateAudio }: Props) {
  const [generating, setGenerating] = useState<Set<string>>(new Set());

  if (updates.length === 0) {
    return <p className="empty">No changes detected yet.</p>;
  }

  const handleGenerate = async (id: string) => {
    setGenerating((prev) => new Set(prev).add(id));
    try {
      await onGenerateAudio(id);
    } finally {
      setGenerating((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    }
  };

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
          {u.audioUrl ? (
            <div className="audio-wrap">
              <audio className="audio-player" controls src={u.audioUrl} />
            </div>
          ) : (
            <button
              className="btn btn-audio"
              onClick={() => handleGenerate(u.id)}
              disabled={generating.has(u.id)}
            >
              {generating.has(u.id) ? "Generating…" : "Generate audio"}
            </button>
          )}
        </li>
      ))}
    </ul>
  );
}
