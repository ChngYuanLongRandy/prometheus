package com.prometheus.watcher.web;

import java.util.List;
import java.util.UUID;

import com.prometheus.watcher.model.TrackedUrl;
import com.prometheus.watcher.repo.TrackedUrlRepository;
import com.prometheus.watcher.service.WatcherService;
import com.prometheus.watcher.web.dto.CheckResult;
import com.prometheus.watcher.web.dto.CreateUrlRequest;
import com.prometheus.watcher.web.dto.UrlView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
public class UrlController {

    private final TrackedUrlRepository urls;
    private final WatcherService watcher;

    public UrlController(TrackedUrlRepository urls, WatcherService watcher) {
        this.urls = urls;
        this.watcher = watcher;
    }

    /** GET /urls — list all tracked URLs, newest first. */
    @GetMapping("/urls")
    public List<UrlView> list() {
        return urls.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(UrlView::of)
                .toList();
    }

    /** POST /urls — add a URL to track. */
    @PostMapping("/urls")
    public ResponseEntity<UrlView> add(@Valid @RequestBody CreateUrlRequest req) {
        String url = req.url().trim();
        if (urls.existsByUrl(url)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "URL already tracked");
        }
        TrackedUrl saved = urls.save(new TrackedUrl(url, blankToNull(req.label())));
        return ResponseEntity.status(HttpStatus.CREATED).body(UrlView.of(saved));
    }

    /** DELETE /urls/{id} — stop tracking a URL (cascades snapshots + updates). */
    @DeleteMapping("/urls/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        if (!urls.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such tracked URL");
        }
        urls.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** POST /urls/{id}/check — re-scrape one URL now. */
    @PostMapping("/urls/{id}/check")
    public CheckResult check(@PathVariable UUID id) {
        if (!urls.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such tracked URL");
        }
        WatcherService.Outcome outcome = watcher.check(id);
        UrlView view = urls.findById(id).map(UrlView::of).orElseThrow();
        return new CheckResult(outcome.name(), view);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
