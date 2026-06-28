# Project Prometheus — Specification

A personal information OS that monitors the web for things I care about and
delivers updates as audio. Built in phases; this repo currently implements
**Phase A1 — the Web Watcher MVP**.

## Vision

Watch a set of web pages, detect meaningful changes, and eventually surface
those changes as a personal audio briefing. Single-user, self-hosted.

## Phases

| Phase | Scope |
|-------|-------|
| **A1** | Web Watcher MVP: track URLs, scrape, diff, store, view changes. *(this milestone)* |
| A2 | Audio: summarise detected changes and synthesize speech (Supabase Storage for audio files). |
| later | Notifications, richer sources (RSS/APIs), summarisation quality, multi-device delivery. |

## A1 architecture

Three decoupled services in a monorepo, orchestrated by Docker Compose.

```
            ┌─────────────┐     HTTP      ┌──────────────┐    HTTP/JDBC
  Browser ──▶  React app  │──────────────▶│ Spring Boot  │───────────────┐
            └─────────────┘   (REST API)  │   backend    │               │
                                          └──────┬───────┘               │
                                   HTTP (POST /scrape)              ┌─────▼──────┐
                                          ┌──────▼───────┐          │  Supabase  │
                                          │   Python     │          │  Postgres  │
                                          │   scraper    │          └────────────┘
                                          └──────────────┘
```

### Boundaries (deliberate, enforced)

- **Scraper** is stateless and fully decoupled: its own directory, own
  Dockerfile, **no shared code and no database access**. It communicates with
  the backend only over HTTP and is extractable into its own repository
  without refactoring. It only receives a URL and returns extracted content.
- **Backend** owns *all* database access. It calls the scraper over HTTP,
  performs diffing, and persists snapshots/updates.
- **Frontend** talks only to the backend REST API — never to Supabase
  directly. No Supabase keys live in the frontend in A1.

## Components

### Scraper (Python, FastAPI)
- `POST /scrape { url }` → `{ url, title, text, method, char_count }`
- Extraction chain: trafilatura → Playwright-rendered + trafilatura →
  BeautifulSoup (last resort).

### Backend (Spring Boot, Java 21)
REST API:
- `POST   /urls`            — add a URL to track
- `DELETE /urls/{id}`       — remove a tracked URL
- `GET    /urls`            — list tracked URLs (last checked, status)
- `POST   /urls/{id}/check` — re-scrape one URL now
- `POST   /check-all`       — re-scrape all tracked URLs now
- `GET    /updates`         — detected changes, newest first

Snapshot + diff logic:
- First scrape stores a full-text snapshot.
- Later scrapes diff against the stored snapshot; if changed, store the new
  snapshot and record the delta (unified diff + added/removed line counts).

Scheduler: `@Scheduled` daily cron re-scrapes all tracked URLs.

### Frontend (React + Vite)
- Add-URL input, tracked-URL list with status/last-checked + manual check,
  update feed with timestamps and diff previews. Dark utilitarian aesthetic.

### Storage (Supabase Postgres)
- `tracked_urls`, `snapshots`, `updates`. See `supabase/migrations/0001_init.sql`.

## Deployment targets
- Local dev now (native or Docker Compose).
- Later: Beelink EQ12 Pro (Ubuntu Server 24.04, Nginx) **or** a cloud host
  such as Railway. The three-service split + Compose keeps either path open.

## Constraints
- Single-user; no auth in A1.
- All secrets via `.env`; nothing hardcoded.
- Supabase for all persistence.
