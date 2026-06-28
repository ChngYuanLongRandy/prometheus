package com.prometheus.watcher.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateUrlRequest(
        @NotBlank
        @Pattern(regexp = "^https?://.+", message = "url must start with http:// or https://")
        String url,
        String label
) {
}
