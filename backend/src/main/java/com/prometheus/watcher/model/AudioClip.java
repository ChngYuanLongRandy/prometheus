package com.prometheus.watcher.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Metadata for a Polly-generated MP3 stored in S3. One clip per update. */
@Entity
@Table(name = "audio_clips")
public class AudioClip {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "update_id", nullable = false, columnDefinition = "uuid")
    private UUID updateId;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "char_count", nullable = false)
    private int charCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AudioClip() {}

    public AudioClip(UUID updateId, String s3Key, int charCount) {
        this.id = UUID.randomUUID();
        this.updateId = updateId;
        this.s3Key = s3Key;
        this.charCount = charCount;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUpdateId() { return updateId; }
    public String getS3Key() { return s3Key; }
    public int getCharCount() { return charCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
