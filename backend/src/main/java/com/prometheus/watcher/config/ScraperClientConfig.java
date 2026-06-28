package com.prometheus.watcher.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds the {@link RestClient} the backend uses to call the decoupled scraper
 * service over HTTP. The scraper base URL comes from config (env var
 * SCRAPER_BASE_URL); there is no shared code or DB between the two services.
 */
@Configuration
public class ScraperClientConfig {

    @Bean
    RestClient scraperRestClient(@Value("${prometheus.scraper.base-url}") String baseUrl) {
        // Scraping (esp. the Playwright fallback) can be slow — allow generous
        // read timeouts so a JS-heavy page does not abort the request.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
