import { useState } from "react";

interface Props {
  onAdd: (url: string, label: string) => Promise<void>;
}

export function AddUrl({ onAdd }: Props) {
  const [url, setUrl] = useState("");
  const [label, setLabel] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!url.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await onAdd(url.trim(), label.trim());
      setUrl("");
      setLabel("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add URL");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="add-url" onSubmit={submit}>
      <input
        className="input grow"
        type="url"
        placeholder="https://example.com/page-to-watch"
        value={url}
        onChange={(e) => setUrl(e.target.value)}
        disabled={busy}
      />
      <input
        className="input"
        type="text"
        placeholder="label (optional)"
        value={label}
        onChange={(e) => setLabel(e.target.value)}
        disabled={busy}
      />
      <button className="btn primary" type="submit" disabled={busy}>
        {busy ? "Adding…" : "Track"}
      </button>
      {error && <div className="error-inline">{error}</div>}
    </form>
  );
}
