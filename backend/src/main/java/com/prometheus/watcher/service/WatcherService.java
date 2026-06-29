package com.prometheus.watcher.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.prometheus.watcher.event.UpdateDetectedEvent;
import com.prometheus.watcher.model.Snapshot;
import com.prometheus.watcher.model.TrackedUrl;
import com.prometheus.watcher.model.UpdateRecord;
import com.prometheus.watcher.repo.SnapshotRepository;
import com.prometheus.watcher.repo.TrackedUrlRepository;
import com.prometheus.watcher.repo.UpdateRepository;
import com.prometheus.watcher.scraper.ScrapeResult;
import com.prometheus.watcher.scraper.ScraperClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates a single check of a tracked URL:
 *   scrape (HTTP -> scraper) -> hash -> compare to last snapshot
 *   -> store snapshot + record an update if changed.
 *
 * The backend is the sole owner of the database; the scraper stays stateless.
 */
@Service
public class WatcherService {

    private static final Logger log = LoggerFactory.getLogger(WatcherService.class);

    private final TrackedUrlRepository urls;
    private final SnapshotRepository snapshots;
    private final UpdateRepository updates;
    private final ScraperClient scraper;
    private final DiffService diffService;
    private final ApplicationEventPublisher eventPublisher;

    public WatcherService(TrackedUrlRepository urls, SnapshotRepository snapshots,
                          UpdateRepository updates, ScraperClient scraper, DiffService diffService,
                          ApplicationEventPublisher eventPublisher) {
        this.urls = urls;
        this.snapshots = snapshots;
        this.updates = updates;
        this.scraper = scraper;
        this.diffService = diffService;
        this.eventPublisher = eventPublisher;
    }

    /** Result of checking one URL, for surfacing to callers/logs. */
    public enum Outcome { FIRST_SNAPSHOT, CHANGED, UNCHANGED, ERROR }

    @Transactional
    public Outcome check(UUID urlId) {
        TrackedUrl tracked = urls.findById(urlId)
                .orElseThrow(() -> new IllegalArgumentException("No tracked URL with id " + urlId));
        return check(tracked);
    }

    @Transactional
    public Outcome check(TrackedUrl tracked) {
        try {
            ScrapeResult result = scraper.scrape(tracked.getUrl());
            String content = result.text();
            String hash = sha256(content);

            Optional<Snapshot> previous = snapshots.findFirstByUrlIdOrderByScrapedAtDesc(tracked.getId());

            // First-ever scrape: store baseline, no update record.
            if (previous.isEmpty()) {
                snapshots.save(new Snapshot(tracked.getId(), content, hash, result.charCount(), result.method()));
                markChecked(tracked, "ok", result.method(), null);
                log.info("First snapshot stored for {}", tracked.getUrl());
                return Outcome.FIRST_SNAPSHOT;
            }

            Snapshot prev = previous.get();

            // Fast path: identical content by hash -> no change, no new snapshot.
            if (prev.getContentHash().equals(hash)) {
                markChecked(tracked, "unchanged", result.method(), null);
                return Outcome.UNCHANGED;
            }

            // Content changed: compute diff, store new snapshot + update record.
            DiffService.DiffResult diff = diffService.diff(prev.getContent(), content);
            Snapshot newSnap = snapshots.save(
                    new Snapshot(tracked.getId(), content, hash, result.charCount(), result.method()));
            UpdateRecord saved = updates.save(new UpdateRecord(
                    tracked.getId(), newSnap.getId(), prev.getId(),
                    diff.unifiedDiff(), diff.addedLines(), diff.removedLines()));
            markChecked(tracked, "changed", result.method(), null);
            log.info("Change detected for {} (+{} -{})", tracked.getUrl(),
                    diff.addedLines(), diff.removedLines());
            // Best-effort audio: event fires after this transaction commits.
            eventPublisher.publishEvent(new UpdateDetectedEvent(saved.getId()));
            return Outcome.CHANGED;

        } catch (Exception ex) {
            log.warn("Check failed for {}: {}", tracked.getUrl(), ex.getMessage());
            markChecked(tracked, "error", tracked.getLastMethod(), truncate(ex.getMessage(), 1000));
            return Outcome.ERROR;
        }
    }

    @Transactional
    public void checkAll() {
        List<TrackedUrl> all = urls.findAll();
        log.info("Checking all {} tracked URLs", all.size());
        for (TrackedUrl t : all) {
            check(t);
        }
    }

    private void markChecked(TrackedUrl tracked, String status, String method, String error) {
        tracked.setLastCheckedAt(OffsetDateTime.now());
        tracked.setLastStatus(status);
        tracked.setLastMethod(method);
        tracked.setLastError(error);
        urls.save(tracked);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
