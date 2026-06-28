package com.prometheus.watcher.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.prometheus.watcher.model.TrackedUrl;
import com.prometheus.watcher.repo.TrackedUrlRepository;
import com.prometheus.watcher.repo.UpdateRepository;
import com.prometheus.watcher.web.dto.UpdateView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UpdateController {

    private final UpdateRepository updates;
    private final TrackedUrlRepository urls;

    public UpdateController(UpdateRepository updates, TrackedUrlRepository urls) {
        this.updates = updates;
        this.urls = urls;
    }

    /** GET /updates — detected changes, newest first. */
    @GetMapping("/updates")
    public List<UpdateView> list() {
        // Resolve URL/label per update without an N+1 query.
        Map<UUID, TrackedUrl> byId = urls.findAll().stream()
                .collect(Collectors.toMap(TrackedUrl::getId, Function.identity()));

        return updates.findAllByOrderByDetectedAtDesc().stream()
                .map(u -> {
                    TrackedUrl t = byId.get(u.getUrlId());
                    String url = t != null ? t.getUrl() : "(deleted)";
                    String label = t != null ? t.getLabel() : null;
                    return UpdateView.of(u, url, label);
                })
                .toList();
    }
}
