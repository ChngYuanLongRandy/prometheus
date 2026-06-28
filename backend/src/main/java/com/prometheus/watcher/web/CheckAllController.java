package com.prometheus.watcher.web;

import java.util.Map;

import com.prometheus.watcher.repo.TrackedUrlRepository;
import com.prometheus.watcher.service.WatcherService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckAllController {

    private final WatcherService watcher;
    private final TrackedUrlRepository urls;

    public CheckAllController(WatcherService watcher, TrackedUrlRepository urls) {
        this.watcher = watcher;
        this.urls = urls;
    }

    /** POST /check-all — re-scrape every tracked URL now. */
    @PostMapping("/check-all")
    public Map<String, Object> checkAll() {
        long count = urls.count();
        watcher.checkAll();
        return Map.of("checked", count, "status", "done");
    }
}
