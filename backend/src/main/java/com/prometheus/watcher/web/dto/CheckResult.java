package com.prometheus.watcher.web.dto;

/** Returned by manual check triggers. */
public record CheckResult(String outcome, UrlView url) {
}
