package com.prometheus.watcher.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.prometheus.watcher.model.AudioClip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioClipRepository extends JpaRepository<AudioClip, UUID> {
    Optional<AudioClip> findByUpdateId(UUID updateId);
    List<AudioClip> findByCreatedAtBefore(OffsetDateTime cutoff);
}
