package com.prometheus.watcher.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.prometheus.watcher.model.AudioClip;
import com.prometheus.watcher.model.TrackedUrl;
import com.prometheus.watcher.repo.AudioClipRepository;
import com.prometheus.watcher.repo.TrackedUrlRepository;
import com.prometheus.watcher.repo.UpdateRepository;
import com.prometheus.watcher.service.S3AudioService;
import com.prometheus.watcher.web.dto.UpdateView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UpdateController {

    private final UpdateRepository updates;
    private final TrackedUrlRepository urls;
    private final AudioClipRepository audioClips;
    private final S3AudioService s3;

    public UpdateController(UpdateRepository updates, TrackedUrlRepository urls,
                            AudioClipRepository audioClips, S3AudioService s3) {
        this.updates = updates;
        this.urls = urls;
        this.audioClips = audioClips;
        this.s3 = s3;
    }

    /** GET /updates — detected changes newest first, with pre-signed audio URLs where available. */
    @GetMapping("/updates")
    public List<UpdateView> list() {
        Map<UUID, TrackedUrl> urlById = urls.findAll().stream()
                .collect(Collectors.toMap(TrackedUrl::getId, Function.identity()));

        Map<UUID, AudioClip> clipByUpdateId = audioClips.findAll().stream()
                .collect(Collectors.toMap(AudioClip::getUpdateId, Function.identity()));

        return updates.findAllByOrderByDetectedAtDesc().stream()
                .map(u -> {
                    TrackedUrl t = urlById.get(u.getUrlId());
                    String url = t != null ? t.getUrl() : "(deleted)";
                    String label = t != null ? t.getLabel() : null;
                    AudioClip clip = clipByUpdateId.get(u.getId());
                    String audioUrl = clip != null ? s3.presignedUrl(clip.getS3Key()) : null;
                    return UpdateView.of(u, url, label, audioUrl);
                })
                .toList();
    }
}
