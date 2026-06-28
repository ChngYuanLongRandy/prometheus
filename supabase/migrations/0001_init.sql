-- Project Prometheus — A1 Web Watcher schema
-- Run this in the Supabase SQL editor (or `supabase db push`).
--
-- The backend (Spring Boot) is the SOLE owner of these tables. The scraper
-- service never touches the database. Postgres now; Supabase Storage (audio)
-- arrives in A2.

-- gen_random_uuid() is built into Postgres 13+ (Supabase runs newer).

-- ---------------------------------------------------------------------------
-- tracked_urls: one row per URL being watched
-- ---------------------------------------------------------------------------
create table if not exists tracked_urls (
    id              uuid primary key default gen_random_uuid(),
    url             text not null unique,
    label           text,
    created_at      timestamptz not null default now(),
    last_checked_at timestamptz,
    -- pending | ok | changed | unchanged | error
    last_status     text not null default 'pending',
    last_method     text,           -- scraper strategy used on last check
    last_error      text            -- populated when last_status = 'error'
);

-- ---------------------------------------------------------------------------
-- snapshots: full-text capture of a URL at a point in time
-- ---------------------------------------------------------------------------
create table if not exists snapshots (
    id           uuid primary key default gen_random_uuid(),
    url_id       uuid not null references tracked_urls(id) on delete cascade,
    content      text not null,
    content_hash text not null,      -- sha-256 of content, for fast equality
    char_count   integer not null,
    method       text,               -- scraper strategy that produced it
    scraped_at   timestamptz not null default now()
);

create index if not exists idx_snapshots_url_scraped
    on snapshots (url_id, scraped_at desc);

-- ---------------------------------------------------------------------------
-- updates: a detected change between two consecutive snapshots
-- ---------------------------------------------------------------------------
create table if not exists updates (
    id                   uuid primary key default gen_random_uuid(),
    url_id               uuid not null references tracked_urls(id) on delete cascade,
    snapshot_id          uuid not null references snapshots(id) on delete cascade,
    previous_snapshot_id uuid references snapshots(id) on delete set null,
    diff                 text not null,      -- unified diff, new vs previous
    added_lines          integer not null default 0,
    removed_lines        integer not null default 0,
    detected_at          timestamptz not null default now()
);

create index if not exists idx_updates_detected on updates (detected_at desc);
create index if not exists idx_updates_url on updates (url_id);
