# CLAUDE.md — Project Prometheus

Context-recovery doc for future sessions. Read this first.

## What this is

Personal information OS that watches the web and (eventually) delivers updates
as audio. Built in phases. **This repo currently implements Phase A1 — the Web
Watcher MVP.** Single-user, self-hosted, no auth. See `SPEC.md` for the full
vision and phase roadmap.

## Architecture (A1)

Three decoupled services in a monorepo + external Supabase Postgres.

```
React (frontend) ──HTTP──▶ Spring Boot (backend) ──HTTP──▶ Python (scraper)
                                   │
                                   └──JDBC──▶ Supabase Postgres
```

- **scraper/** — stateless Python (FastAPI) extraction service. Given a URL,
  returns main text content. **Decoupled by design: own dir, own Dockerfile,
  no shared code, NO database access.** Extractable into its own repo without
  refactoring. Talks to nothing; only answers `POST /scrape`.
- **backend/** — Spring Boot (Java 21). **Sole owner of all database access.**
  Calls the scraper over HTTP, diffs against the last snapshot, persists
  snapshots + updates. Hosts the REST API and the daily scheduler.
- **frontend/** — React + Vite + TS. Talks **only** to the backend REST API,
  never to Supabase directly (no Supabase keys in the frontend in A1).
- **supabase/migrations/** — SQL schema. Backend owns these tables.

> Keep the scraper boundary clean. If you find yourself sharing code or DB
> access between backend and scraper, stop — that violates the core constraint.

## Status — DONE in A1

- [x] Scaffold monorepo structure
- [x] Python scraper: `POST /scrape`, `GET /health`. Extraction chain
      trafilatura → Playwright(Chromium)+trafilatura → BeautifulSoup.
      **Verified working** against live pages (all three strategies + 422 path).
- [x] Supabase SQL migration (`supabase/migrations/0001_init.sql`):
      `tracked_urls`, `snapshots`, `updates`.
- [x] Spring Boot backend — **compiles clean**. Endpoints:
      `POST /urls`, `DELETE /urls/{id}`, `GET /urls`,
      `POST /urls/{id}/check`, `POST /check-all`, `GET /updates`.
      Snapshot + SHA-256 hash compare + unified-diff (java-diff-utils).
- [x] `@Scheduled` daily cron (`prometheus.watcher.cron`, default 08:00).
- [x] React frontend — **builds clean**. Add-URL, tracked list with status +
      manual check + delete, update feed with diff preview. Dark utilitarian UI.
- [x] `docker-compose.yml` for all three services.

### Not yet done / needs YOU

- [ ] **Run the Supabase migration** (`supabase/migrations/0001_init.sql`) in
      the Supabase SQL editor.
- [ ] **Fill `.env`** from `.env.example` with the Supabase connection string.
      The backend will not start without it (`SPRING_DATASOURCE_*`).
- [ ] End-to-end run has not been executed against a live DB yet (no creds at
      build time). Scraper and both builds are independently verified.

## How to run

### Native (dev)

```bash
# 1. scraper
cd scraper
python -m venv .venv && source .venv/Scripts/activate   # Win: .venv\Scripts\activate
pip install -r requirements.txt
playwright install chromium          # one-time, downloads headless Chromium
uvicorn app.main:app --reload --port 8000

# 2. backend  (needs JAVA_HOME set to the JDK 21 install, and .env populated)
cd backend
export JAVA_HOME="/c/Program Files/Java/jdk-21"   # PowerShell: $env:JAVA_HOME="C:\Program Files\Java\jdk-21"
# load .env into the environment, then:
./mvnw spring-boot:run            # Win: .\mvnw.cmd spring-boot:run

# 3. frontend
cd frontend
npm install
npm run dev                        # http://localhost:5173
```

> The root `.env` is read by Docker Compose automatically. For **native** runs
> you must export the vars into the shell yourself (Spring Boot does not read
> `.env` files without help). Easiest: `set -a; . ../.env; set +a` before
> `./mvnw spring-boot:run`.

### Docker Compose

```bash
cp .env.example .env      # then fill in Supabase creds
docker compose up --build
# frontend :5173  backend :8080  scraper :8000
```

## Key decisions / gotchas

- **Maven Wrapper** (`backend/mvnw`) is committed — global `mvn` is not
  installed on this machine. First run downloads Maven 3.9.9.
- **`JAVA_HOME` is not set globally** on this machine; JDK 21 lives at
  `C:\Program Files\Java\jdk-21`. Set it before running the backend natively.
  (`java` on PATH is the Oracle javapath shim, not a full JDK home.)
- **`spring.jpa.hibernate.ddl-auto=validate`** — schema comes from the SQL
  migration, not Hibernate. If validation friction blocks a first run, set to
  `none` in `backend/src/main/resources/application.properties`.
- **Supabase connection**: prefer the **Session pooler** (port 5432) for a
  long-lived Hikari pool. Avoid the Transaction pooler (6543) — it breaks JPA
  prepared statements. JDBC URL needs `?sslmode=require`.
- **Scraper "sufficient content" threshold** is 200 chars
  (`SCRAPER_MIN_CONTENT_CHARS`). Tiny pages (e.g. example.com) legitimately
  return 422.
- **Diff** is line-based unified diff over the extracted text; equality is
  short-circuited by SHA-256 hash before diffing.

## What comes next — A2 (audio)

- Summarise each detected `update` (LLM) into a short briefing.
- Synthesize speech; store audio files in **Supabase Storage** (new in A2 —
  A1 is Postgres only).
- Surface/play audio in the frontend; likely a per-update "listen" action.
- Boundaries stay the same: backend owns persistence + storage; scraper stays
  stateless and untouched.

## Deployment targets (later)

Local dev now. Later: Beelink EQ12 Pro (Ubuntu Server 24.04, Nginx) **or** a
cloud host like Railway. The 3-service split + Compose keeps both open.
