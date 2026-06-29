package com.prometheus.watcher.service;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.prometheus.watcher.model.AudioClip;
import com.prometheus.watcher.model.TrackedUrl;
import com.prometheus.watcher.model.UpdateRecord;
import com.prometheus.watcher.repo.AudioClipRepository;
import com.prometheus.watcher.repo.TrackedUrlRepository;
import com.prometheus.watcher.repo.UpdateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates audio generation: compose speech text from a diff → Polly → S3 → persist.
 * All public methods are best-effort: failures are logged and return Optional.empty(),
 * never propagated to callers.
 */
@Service
public class AudioClipService {

    private static final Logger log = LoggerFactory.getLogger(AudioClipService.class);

    private final PollyTtsService polly;
    private final S3AudioService s3;
    private final AudioClipRepository audioClips;
    private final UpdateRepository updates;
    private final TrackedUrlRepository urls;

    public AudioClipService(PollyTtsService polly, S3AudioService s3,
                            AudioClipRepository audioClips, UpdateRepository updates,
                            TrackedUrlRepository urls) {
        this.polly = polly;
        this.s3 = s3;
        this.audioClips = audioClips;
        this.updates = updates;
        this.urls = urls;
    }

    /** Called by the post-commit event listener; looks up entities from DB. */
    @Transactional
    public Optional<AudioClip> generateForUpdate(UUID updateId) {
        UpdateRecord update = updates.findById(updateId).orElse(null);
        if (update == null) {
            log.warn("Audio generation skipped: update {} not found", updateId);
            return Optional.empty();
        }
        TrackedUrl url = urls.findById(update.getUrlId()).orElse(null);
        return generateForUpdate(update, url);
    }

    /** Called directly by the manual generate-audio endpoint (entities already loaded). */
    @Transactional
    public Optional<AudioClip> generateForUpdate(UpdateRecord update, TrackedUrl trackedUrl) {
        try {
            // Replace existing clip for this update if present.
            audioClips.findByUpdateId(update.getId()).ifPresent(existing -> {
                try { s3.delete(existing.getS3Key()); } catch (Exception ex) {
                    log.warn("Could not delete old S3 key {}: {}", existing.getS3Key(), ex.getMessage());
                }
                audioClips.delete(existing);
                audioClips.flush();
            });

            String speechText = composeSpeechText(update, trackedUrl);
            byte[] mp3 = polly.synthesize(speechText);
            String key = "audio/" + update.getId() + ".mp3";
            s3.upload(key, mp3);

            AudioClip clip = audioClips.save(new AudioClip(update.getId(), key, speechText.length()));
            log.info("Audio clip created for update {} ({} chars, {} bytes MP3)",
                    update.getId(), speechText.length(), mp3.length);
            return Optional.of(clip);

        } catch (Exception e) {
            log.warn("TTS generation failed for update {} (best-effort): {}",
                    update.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    private String composeSpeechText(UpdateRecord update, TrackedUrl url) {
        String source = url != null
                ? (url.getLabel() != null ? url.getLabel() : url.getUrl())
                : "a tracked page";

        // Extract added lines from the unified diff, strip the leading '+'.
        String addedContent = Arrays.stream(update.getDiff().split("\n"))
                .filter(l -> l.startsWith("+") && !l.startsWith("+++"))
                .map(l -> l.substring(1).trim())
                .filter(l -> !l.isEmpty())
                .collect(Collectors.joining(" "));

        StringBuilder sb = new StringBuilder();
        sb.append("Change detected at ").append(source).append(". ");
        sb.append("Added ").append(update.getAddedLines())
          .append(" lines, removed ").append(update.getRemovedLines()).append(" lines. ");
        if (!addedContent.isEmpty()) {
            sb.append("New content: ").append(addedContent);
        }
        return sb.toString();
    }
}
