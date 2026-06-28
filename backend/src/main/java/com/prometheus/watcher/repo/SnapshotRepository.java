package com.prometheus.watcher.repo;

import java.util.Optional;
import java.util.UUID;

import com.prometheus.watcher.model.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotRepository extends JpaRepository<Snapshot, UUID> {
    /** Most recent snapshot for a URL, used as the diff baseline. */
    Optional<Snapshot> findFirstByUrlIdOrderByScrapedAtDesc(UUID urlId);
}
