package com.prometheus.watcher.repo;

import java.util.List;
import java.util.UUID;

import com.prometheus.watcher.model.UpdateRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpdateRepository extends JpaRepository<UpdateRecord, UUID> {
    /** Newest changes first, for the update feed. */
    List<UpdateRecord> findAllByOrderByDetectedAtDesc();
}
