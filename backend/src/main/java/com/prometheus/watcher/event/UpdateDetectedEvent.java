package com.prometheus.watcher.event;

import java.util.UUID;

/** Published inside the check() transaction when a content change is recorded. */
public record UpdateDetectedEvent(UUID updateId) {}
