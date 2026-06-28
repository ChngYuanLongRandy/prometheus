import type { CheckResult, TrackedUrl, UpdateItem } from "./types";

const BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

async function req<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...init,
  });
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      detail = body.message ?? body.detail ?? detail;
    } catch {
      /* non-JSON error body */
    }
    throw new Error(`${res.status}: ${detail}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  listUrls: () => req<TrackedUrl[]>("/urls"),

  addUrl: (url: string, label?: string) =>
    req<TrackedUrl>("/urls", {
      method: "POST",
      body: JSON.stringify({ url, label: label || null }),
    }),

  deleteUrl: (id: string) => req<void>(`/urls/${id}`, { method: "DELETE" }),

  checkUrl: (id: string) =>
    req<CheckResult>(`/urls/${id}/check`, { method: "POST" }),

  checkAll: () =>
    req<{ checked: number; status: string }>("/check-all", { method: "POST" }),

  listUpdates: () => req<UpdateItem[]>("/updates"),
};
