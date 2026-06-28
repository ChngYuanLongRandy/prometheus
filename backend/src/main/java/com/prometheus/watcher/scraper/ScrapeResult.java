package com.prometheus.watcher.scraper;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Response shape from the scraper service's POST /scrape. */
public record ScrapeResult(
        String url,
        String title,
        String text,
        String method,
        @JsonProperty("char_count") int charCount
) {
}
