package com.prometheus.watcher.web;

import java.util.Optional;
import java.util.UUID;

import com.prometheus.watcher.model.AudioClip;
import com.prometheus.watcher.model.TrackedUrl;
import com.prometheus.watcher.model.UpdateRecord;
import com.prometheus.watcher.repo.AudioClipRepository;
import com.prometheus.watcher.repo.TrackedUrlRepository;
import com.prometheus.watcher.repo.UpdateRepository;
import com.prometheus.watcher.service.AudioClipService;
import com.prometheus.watcher.service.S3AudioService;
import com.prometheus.watcher.web.dto.UpdateView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AudioController {

    private final UpdateRepository updates;
    private final TrackedUrlRepository urls;
    private final AudioClipRepository audioClips;
    private final AudioClipService audioService;
    private final S3AudioService s3;

    public AudioController(UpdateRepository updates, TrackedUrlRepository urls,
                           AudioClipRepository audioClips, AudioClipService audioService,
                           S3AudioService s3) {
        this.updates = updates;
        this.urls = urls;
        this.audioClips = audioClips;
        this.audioService = audioService;
        this.s3 = s3;
    }

    /**
     * POST /updates/{id}/generate-audio
     * Manually (re)generate the audio clip for an update. Returns the UpdateView
     * with audioUrl populated if synthesis succeeded, null if it failed (best-effort).
     */
    @PostMapping("/updates/{id}/generate-audio")
    public ResponseEntity<UpdateView> generateAudio(@PathVariable UUID id) {
        UpdateRecord update = updates.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such update"));
        TrackedUrl url = urls.findById(update.getUrlId()).orElse(null);

        Optional<AudioClip> clip = audioService.generateForUpdate(update, url);

        String audioUrl = clip.map(c -> s3.presignedUrl(c.getS3Key())).orElse(null);
        String resolvedUrl = url != null ? url.getUrl() : "(deleted)";
        String label = url != null ? url.getLabel() : null;

        return ResponseEntity.ok(UpdateView.of(update, resolvedUrl, label, audioUrl));
    }
}
