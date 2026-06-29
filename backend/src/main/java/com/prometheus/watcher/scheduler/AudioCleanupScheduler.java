package com.prometheus.watcher.scheduler;

import java.time.OffsetDateTime;
import java.util.List;

import com.prometheus.watcher.model.AudioClip;
import com.prometheus.watcher.repo.AudioClipRepository;
import com.prometheus.watcher.service.S3AudioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Deletes S3 objects and DB rows for audio clips older than N days (default 30). */
@Component
public class AudioCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AudioCleanupScheduler.class);

    private final AudioClipRepository audioClips;
    private final S3AudioService s3;

    @Value("${prometheus.audio.cleanup-after-days:30}")
    private int cleanupAfterDays;

    public AudioCleanupScheduler(AudioClipRepository audioClips, S3AudioService s3) {
        this.audioClips = audioClips;
        this.s3 = s3;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "${prometheus.watcher.zone:UTC}")
    @Transactional
    public void cleanupOldAudio() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(cleanupAfterDays);
        List<AudioClip> old = audioClips.findByCreatedAtBefore(cutoff);
        if (old.isEmpty()) return;

        log.info("Audio cleanup: removing {} clips older than {} days", old.size(), cleanupAfterDays);
        for (AudioClip clip : old) {
            try {
                s3.delete(clip.getS3Key());
                audioClips.delete(clip);
            } catch (Exception e) {
                log.warn("Failed to remove audio clip {} ({}): {}",
                        clip.getId(), clip.getS3Key(), e.getMessage());
            }
        }
    }
}
