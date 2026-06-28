package com.prometheus.watcher.scheduler;

import com.prometheus.watcher.service.WatcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily automatic re-scrape of all tracked URLs. Cron is configurable via
 * the WATCHER_CRON env var (mapped to prometheus.watcher.cron); defaults to
 * 08:00 every day.
 */
@Component
public class WatcherScheduler {

    private static final Logger log = LoggerFactory.getLogger(WatcherScheduler.class);

    private final WatcherService watcher;

    public WatcherScheduler(WatcherService watcher) {
        this.watcher = watcher;
    }

    @Scheduled(cron = "${prometheus.watcher.cron:0 0 8 * * *}", zone = "${prometheus.watcher.zone:UTC}")
    public void scheduledCheckAll() {
        log.info("Scheduled daily check-all starting");
        watcher.checkAll();
        log.info("Scheduled daily check-all finished");
    }
}
