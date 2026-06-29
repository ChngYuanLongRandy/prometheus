package com.prometheus.watcher.event;

import com.prometheus.watcher.service.AudioClipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Triggers audio generation AFTER the update-detection transaction commits.
 * Running after commit avoids FK issues and ensures the update row is visible
 * to the AudioClipService's own transaction.
 */
@Component
public class AudioGenerationListener {

    private static final Logger log = LoggerFactory.getLogger(AudioGenerationListener.class);

    private final AudioClipService audioService;

    public AudioGenerationListener(AudioClipService audioService) {
        this.audioService = audioService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUpdateDetected(UpdateDetectedEvent event) {
        log.debug("Post-commit: generating audio for update {}", event.updateId());
        audioService.generateForUpdate(event.updateId());
    }
}
