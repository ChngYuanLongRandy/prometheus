package com.prometheus.watcher.repo;

import java.util.Optional;
import java.util.UUID;

import com.prometheus.watcher.model.TrackedUrl;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedUrlRepository extends JpaRepository<TrackedUrl, UUID> {
    Optional<TrackedUrl> findByUrl(String url);

    boolean existsByUrl(String url);
}
