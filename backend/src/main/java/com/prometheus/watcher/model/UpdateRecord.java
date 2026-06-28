package com.prometheus.watcher.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A detected change between two consecutive snapshots of a tracked URL. */
@Entity
@Table(name = "updates")
public class UpdateRecord {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "url_id", nullable = false, columnDefinition = "uuid")
    private UUID urlId;

    @Column(name = "snapshot_id", nullable = false, columnDefinition = "uuid")
    private UUID snapshotId;

    @Column(name = "previous_snapshot_id", columnDefinition = "uuid")
    private UUID previousSnapshotId;

    @Column(nullable = false, columnDefinition = "text")
    private String diff;

    @Column(name = "added_lines", nullable = false)
    private int addedLines;

    @Column(name = "removed_lines", nullable = false)
    private int removedLines;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    protected UpdateRecord() {
    }

    public UpdateRecord(UUID urlId, UUID snapshotId, UUID previousSnapshotId,
                        String diff, int addedLines, int removedLines) {
        this.id = UUID.randomUUID();
        this.urlId = urlId;
        this.snapshotId = snapshotId;
        this.previousSnapshotId = previousSnapshotId;
        this.diff = diff;
        this.addedLines = addedLines;
        this.removedLines = removedLines;
        this.detectedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUrlId() {
        return urlId;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public UUID getPreviousSnapshotId() {
        return previousSnapshotId;
    }

    public String getDiff() {
        return diff;
    }

    public int getAddedLines() {
        return addedLines;
    }

    public int getRemovedLines() {
        return removedLines;
    }

    public OffsetDateTime getDetectedAt() {
        return detectedAt;
    }
}
