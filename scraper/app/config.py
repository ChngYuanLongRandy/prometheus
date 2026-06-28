"""Runtime configuration for the scraper service.

The scraper is fully stateless: it holds no database connection and no
shared code with the backend. Everything it needs comes from env vars.
"""
import os


# Below this character count, an extraction result is treated as
# "insufficient" and the next strategy in the chain is attempted.
MIN_CONTENT_CHARS = int(os.getenv("SCRAPER_MIN_CONTENT_CHARS", "200"))

# Timeout (seconds) for the initial static HTTP fetch.
HTTP_TIMEOUT = float(os.getenv("SCRAPER_HTTP_TIMEOUT", "20"))

# Timeout (milliseconds) for Playwright page navigation.
PLAYWRIGHT_TIMEOUT_MS = int(os.getenv("SCRAPER_PLAYWRIGHT_TIMEOUT_MS", "30000"))

# Whether the Playwright fallback is enabled. Disable in environments where
# Chromium is not installed to fail fast instead of erroring per request.
PLAYWRIGHT_ENABLED = os.getenv("SCRAPER_PLAYWRIGHT_ENABLED", "true").lower() == "true"

USER_AGENT = os.getenv(
    "SCRAPER_USER_AGENT",
    "Mozilla/5.0 (compatible; PrometheusWatcher/0.1; +https://github.com/prometheus)",
)
