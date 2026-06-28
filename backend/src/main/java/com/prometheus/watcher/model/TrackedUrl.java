package com.prometheus.watcher.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tracked_urls")
public class TrackedUrl {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String url;

    private String label;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_checked_at")
    private OffsetDateTime lastCheckedAt;

    /** pending | ok | changed | unchanged | error */
    @Column(name = "last_status", nullable = false)
    private String lastStatus = "pending";

    @Column(name = "last_method")
    private String lastMethod;

    @Column(name = "last_error")
    private String lastError;

    protected TrackedUrl() {
    }

    public TrackedUrl(String url, String label) {
        this.id = UUID.randomUUID();
        this.url = url;
        this.label = label;
        this.createdAt = OffsetDateTime.now();
        this.lastStatus = "pending";
    }

    public UUID getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getLabel() {
        return label;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(OffsetDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getLastMethod() {
        return lastMethod;
    }

    public void setLastMethod(String lastMethod) {
        this.lastMethod = lastMethod;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
