package com.prometheus.watcher.scraper;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the decoupled scraper microservice. This is the ONLY way the
 * backend reaches the scraper — no shared code, no shared database.
 */
@Component
public class ScraperClient {

    private final RestClient restClient;

    public ScraperClient(RestClient scraperRestClient) {
        this.restClient = scraperRestClient;
    }

    /**
     * Request extraction of a URL's main content.
     *
     * @throws ScraperException if the scraper is unreachable or returns an error
     */
    public ScrapeResult scrape(String url) {
        try {
            return restClient.post()
                    .uri("/scrape")
                    .body(Map.of("url", url))
                    .retrieve()
                    .body(ScrapeResult.class);
        } catch (Exception ex) {
            throw new ScraperException("Scraper call failed for " + url + ": " + ex.getMessage(), ex);
        }
    }

    public static class ScraperException extends RuntimeException {
        public ScraperException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
