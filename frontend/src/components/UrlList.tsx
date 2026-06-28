import { useState } from "react";
import type { TrackedUrl } from "../types";

interface Props {
  urls: TrackedUrl[];
  onCheck: (id: string) => Promise<void>;
  onDelete: (id: string) => Promise<void>;
}

function timeAgo(iso: string | null): string {
  if (!iso) return "never";
  const then = new Date(iso).getTime();
  const secs = Math.floor((Date.now() - then) / 1000);
  if (secs < 60) return `${secs}s ago`;
  if (secs < 3600) return `${Math.floor(secs / 60)}m ago`;
  if (secs < 86400) return `${Math.floor(secs / 3600)}h ago`;
  return `${Math.floor(secs / 86400)}d ago`;
}

export function UrlList({ urls, onCheck, onDelete }: Props) {
  const [busyId, setBusyId] = useState<string | null>(null);

  async function run(id: string, fn: (id: string) => Promise<void>) {
    setBusyId(id);
    try {
      await fn(id);
    } finally {
      setBusyId(null);
    }
  }

  if (urls.length === 0) {
    return <p className="empty">No URLs tracked yet. Add one above.</p>;
  }

  return (
    <ul className="url-list">
      {urls.map((u) => (
        <li className="url-row" key={u.id}>
          <div className="url-main">
            <a className="url-link" href={u.url} target="_blank" rel="noreferrer">
              {u.label || u.url}
            </a>
            {u.label && <span className="url-sub">{u.url}</span>}
            <span className="url-meta">
              checked {timeAgo(u.lastCheckedAt)}
              {u.lastMethod ? ` · ${u.lastMethod}` : ""}
            </span>
            {u.lastError && <span className="url-error">{u.lastError}</span>}
          </div>
          <span className={`status status-${u.lastStatus}`}>{u.lastStatus}</span>
          <div className="url-actions">
            <button
              className="btn"
              disabled={busyId === u.id}
              onClick={() => run(u.id, onCheck)}
            >
              {busyId === u.id ? "…" : "Check"}
            </button>
            <button
              className="btn danger"
              disabled={busyId === u.id}
              onClick={() => run(u.id, onDelete)}
            >
              ✕
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}
