import { useCallback, useEffect, useState } from "react";
import { api } from "./api";
import type { TrackedUrl, UpdateItem } from "./types";
import { AddUrl } from "./components/AddUrl";
import { UrlList } from "./components/UrlList";
import { UpdateFeed } from "./components/UpdateFeed";

export default function App() {
  const [urls, setUrls] = useState<TrackedUrl[]>([]);
  const [updates, setUpdates] = useState<UpdateItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [checkingAll, setCheckingAll] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const [u, up] = await Promise.all([api.listUrls(), api.listUpdates()]);
      setUrls(u);
      setUpdates(up);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load data");
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const addUrl = async (url: string, label: string) => {
    await api.addUrl(url, label || undefined);
    await refresh();
  };

  const checkUrl = async (id: string) => {
    await api.checkUrl(id);
    await refresh();
  };

  const deleteUrl = async (id: string) => {
    await api.deleteUrl(id);
    await refresh();
  };

  const checkAll = async () => {
    setCheckingAll(true);
    try {
      await api.checkAll();
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "check-all failed");
    } finally {
      setCheckingAll(false);
    }
  };

  const generateAudio = async (id: string) => {
    await api.generateAudio(id);
    await refresh();
  };

  return (
    <div className="app">
      <header className="header">
        <div>
          <h1>PROMETHEUS</h1>
          <span className="subtitle">web watcher · A2</span>
        </div>
        <button className="btn" onClick={checkAll} disabled={checkingAll}>
          {checkingAll ? "Checking all…" : "Check all"}
        </button>
      </header>

      {error && <div className="error-banner">{error}</div>}

      <AddUrl onAdd={addUrl} />

      <section>
        <h2 className="section-title">Tracked ({urls.length})</h2>
        <UrlList urls={urls} onCheck={checkUrl} onDelete={deleteUrl} />
      </section>

      <section>
        <h2 className="section-title">Updates ({updates.length})</h2>
        <UpdateFeed updates={updates} onGenerateAudio={generateAudio} />
      </section>
    </div>
  );
}
