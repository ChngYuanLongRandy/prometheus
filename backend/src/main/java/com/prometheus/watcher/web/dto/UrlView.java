package com.prometheus.watcher.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.prometheus.watcher.model.TrackedUrl;

public record UrlView(
        UUID id,
        String url,
        String label,
        OffsetDateTime createdAt,
        OffsetDateTime lastCheckedAt,
        String lastStatus,
        String lastMethod,
        String lastError
) {
    public static UrlView of(TrackedUrl t) {
        return new UrlView(
                t.getId(), t.getUrl(), t.getLabel(), t.getCreatedAt(),
                t.getLastCheckedAt(), t.getLastStatus(), t.getLastMethod(), t.getLastError());
    }
}
