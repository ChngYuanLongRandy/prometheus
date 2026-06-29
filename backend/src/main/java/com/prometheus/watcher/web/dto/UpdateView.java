package com.prometheus.watcher.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.prometheus.watcher.model.UpdateRecord;

public record UpdateView(
        UUID id,
        UUID urlId,
        String url,
        String label,
        String diff,
        int addedLines,
        int removedLines,
        OffsetDateTime detectedAt,
        String audioUrl
) {
    public static UpdateView of(UpdateRecord u, String url, String label, String audioUrl) {
        return new UpdateView(
                u.getId(), u.getUrlId(), url, label, u.getDiff(),
                u.getAddedLines(), u.getRemovedLines(), u.getDetectedAt(), audioUrl);
    }
}
