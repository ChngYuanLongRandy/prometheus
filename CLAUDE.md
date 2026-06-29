# CLAUDE.md — Project Prometheus

Context-recovery doc for future sessions. Read this first.

## What this is

Personal information OS that watches the web and delivers updates as audio.
Built in phases. **This repo currently implements Phase A2 — Web Watcher +
Audio Digest.** Single-user, self-hosted, no auth. See `SPEC.md` for the full
vision and phase roadmap.

## Architecture (A2)

Three decoupled services in a monorepo + external Supabase Postgres + Amazon S3.

```
React (frontend) ──HTTP──▶ Spring Boot (backend) ──HTTP──▶ Python (scraper)
                                   │
                                   ├──JDBC──▶ Supabase Postgres
                                   ├──HTTP──▶ Amazon Polly (TTS)
                                   └──HTTP──▶ Amazon S3 (audio .mp3 files)
```

- **scraper/** — stateless Python (FastAPI) extraction service. Given a URL,
  returns main text content. **Decoupled by design: own dir, own Dockerfile,
  no shared code, NO database access.** Extractable into its own repo without
  refactoring. Talks to nothing; only answers `POST /scrape`.
- **backend/** — Spring Boot (Java 21). **Sole owner of all database access.**
  Calls the scraper over HTTP, diffs against the last snapshot, persists
  snapshots + updates. On change: publishes a post-commit event that triggers
  Polly TTS + S3 upload (best-effort, never blocks change detection).
  Hosts the REST API and the daily scheduler.
- **frontend/** — React + Vite + TS. Talks **only** to the backend REST API,
  never to Supabase or AWS directly.
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

- [x] **End-to-end verified against the live Supabase DB** (2026-06-29):
      add URL → scrape → first snapshot → re-check → diff (+19/−23) → update
      feed → delete (cascade). All six endpoints exercised; pooler connection,
      schema `validate`, and cascade delete all confirmed working.

## Status — DONE in A2

- [x] Amazon Polly TTS — standard Joanna voice (en-US). Chunks text at 3,000
      chars; max 9,000 chars (3 chunks) per clip; longer content truncated.
      Speech text = "Change detected at [label]. Added N / removed M lines.
      New content: [extracted added lines]."
- [x] Amazon S3 audio storage — private bucket, ap-southeast-1. Pre-signed
      GET URLs (1-hour expiry) generated at read time by the backend.
      Objects stored at key `audio/<update-uuid>.mp3`.
- [x] Supabase migration `0002_audio.sql` — `audio_clips` table:
      `id`, `update_id` (FK→updates, UNIQUE), `s3_key`, `char_count`,
      `created_at`. Index on `created_at` for the cleanup job.
- [x] Best-effort audio wiring — `@TransactionalEventListener(AFTER_COMMIT)`
      fires after the change-detection transaction commits; TTS failure never
      rolls back or blocks the update record.
- [x] New backend endpoints:
      `GET /updates` — now includes `audioUrl` (pre-signed, null if no clip).
      `POST /updates/{id}/generate-audio` — manual (re)generate.
- [x] 30-day cleanup scheduler (`AudioCleanupScheduler`, 03:00 UTC daily) —
      deletes S3 objects and `audio_clips` rows older than 30 days.
- [x] React frontend — `<audio controls>` player in update feed when audio
      exists; "Generate audio" button with per-item loading state when it
      doesn't. Version subtitle bumped to A2.
- [x] AWS IAM least-privilege policy: `polly:Synthesize*` + `s3:PutObject /
      GetObject / DeleteObject` scoped to `audio/` prefix of one bucket.
      Credentials via env vars (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`).
- [x] `docker-compose.yml` and `.env.example` updated with AWS vars.

### Done by YOU (already complete)

- [x] Supabase migration 0001 run (schema `validate` passes, tables exist).
- [x] `.env` populated with Session-pooler credentials.
- [ ] **Run Supabase migration `0002_audio.sql`** (paste into SQL editor).
- [ ] **Create S3 bucket** in ap-southeast-1, all public access blocked.
- [ ] **Create IAM user** `prometheus-tts` with `prometheus-tts-policy`;
      generate access key and add to `.env`.
- [ ] Add `AWS_S3_AUDIO_BUCKET=your-bucket-name` to `.env`.

### Next time you run it

- Native backend needs `JAVA_HOME` set and `.env` exported into the shell
  (see "How to run"). Start order: scraper :8000 → backend :8080 → frontend.

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
  prepared statements. JDBC URL needs `?sslmode=require`. Username is
  ref-qualified: `postgres.<project-ref>`.
- **Pooler host gotcha (cost us a boot)**: this project lives on Supabase's
  newer pooler infra — the correct host is
  `aws-1-ap-southeast-1.pooler.supabase.com`, NOT `aws-0-…`. The `aws-0` host
  resolves and accepts the TCP connection but Supavisor rejects the tenant with
  `FATAL: (ENOTFOUND) tenant/user postgres.<ref> not found`. If you ever see
  that error, it's the host prefix/region, not the password — copy the exact
  string from Dashboard → Connect → Session pooler.
- **Scraper "sufficient content" threshold** is 200 chars
  (`SCRAPER_MIN_CONTENT_CHARS`). Tiny pages (e.g. example.com) legitimately
  return 422.
- **Diff** is line-based unified diff over the extracted text; equality is
  short-circuited by SHA-256 hash before diffing.
- **TTS wiring uses `@TransactionalEventListener(AFTER_COMMIT)`**: the event
  is published inside `WatcherService.check()` but delivered after the
  transaction commits, so the `audio_clips` FK to `updates` is always valid
  and TTS failure cannot affect the update record.
- **Polly standard Joanna / 3,000-char limit**: text > 3,000 chars is chunked
  and MP3 streams are concatenated (standard MP3 streams concatenate cleanly
  for speech). Hard cap at 9,000 chars (3 chunks); beyond that, truncated with
  "... content truncated." to control costs.
- **S3 pre-signed URLs**: 1-hour expiry. The stored value in `audio_clips` is
  the S3 key (`audio/<uuid>.mp3`), not the URL. The backend generates fresh
  pre-signed URLs on every `GET /updates` call. Never expose AWS credentials
  or keys to the frontend.
- **AWS credential chain**: `AwsConfig` beans use `DefaultCredentialsProvider`
  (no explicit credentials in Java code). The SDK reads `AWS_ACCESS_KEY_ID`
  and `AWS_SECRET_ACCESS_KEY` from the environment automatically.

## What comes next

### A2.5 — playback testing
- End-to-end test: add URL → trigger check → wait for TTS → verify audio
  player appears in the feed.
- Confirm IAM policy is tight: test that the credentials cannot write to other
  buckets or call non-Polly/non-S3 AWS APIs.
- Verify 30-day cleanup by manually creating a clip with a backdated
  `created_at` and running the scheduler.

### A3 — WhatsApp notifications
- On each detected change (or daily digest), send a WhatsApp message with the
  summary and a link to the audio clip.
- Likely via the WhatsApp Business Cloud API or Twilio.
- Backend owns the notification logic; no new services needed if HTTP-based.

## Deployment targets (later)

Local dev now. Later: Beelink EQ12 Pro (Ubuntu Server 24.04, Nginx) **or** a
cloud host like Railway. The 3-service split + Compose keeps both open.
