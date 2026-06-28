package com.prometheus.watcher.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "snapshots")
public class Snapshot {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "url_id", nullable = false, columnDefinition = "uuid")
    private UUID urlId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "content_hash", nullable = false)
    private String contentHash;

    @Column(name = "char_count", nullable = false)
    private int charCount;

    private String method;

    @Column(name = "scraped_at", nullable = false)
    private OffsetDateTime scrapedAt;

    protected Snapshot() {
    }

    public Snapshot(UUID urlId, String content, String contentHash, int charCount, String method) {
        this.id = UUID.randomUUID();
        this.urlId = urlId;
        this.content = content;
        this.contentHash = contentHash;
        this.charCount = charCount;
        this.method = method;
        this.scrapedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUrlId() {
        return urlId;
    }

    public String getContent() {
        return content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public int getCharCount() {
        return charCount;
    }

    public String getMethod() {
        return method;
    }

    public OffsetDateTime getScrapedAt() {
        return scrapedAt;
    }
}
