-- Project Prometheus — A2 Audio schema
-- Run this in the Supabase SQL editor after 0001_init.sql.
--
-- Stores metadata for generated Polly MP3 files. The actual audio lives in
-- Amazon S3 (private bucket); this table stores the S3 object key and serves
-- pre-signed URLs at read time. One audio clip per update (UNIQUE on update_id).

create table if not exists audio_clips (
    id          uuid primary key default gen_random_uuid(),
    update_id   uuid not null unique references updates(id) on delete cascade,
    s3_key      text not null,       -- e.g. "audio/<update-uuid>.mp3"
    char_count  integer not null default 0,
    created_at  timestamptz not null default now()
);

-- Used by the 30-day cleanup job to find clips by age.
create index if not exists idx_audio_clips_created on audio_clips (created_at);
