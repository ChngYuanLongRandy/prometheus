"""Content extraction chain.

Strategy order (per A1 spec):
  1. trafilatura on the statically-fetched HTML  (main-content auto-detection)
  2. Playwright-rendered HTML re-run through trafilatura  (JS-rendered pages)
  3. BeautifulSoup plain-text extraction            (last resort)

Each strategy reports the method it used so the backend can record provenance.
The service is stateless — this module performs no I/O beyond the target URL.
"""
from __future__ import annotations

import logging
from dataclasses import dataclass

import httpx
import trafilatura
from bs4 import BeautifulSoup

from . import config

logger = logging.getLogger("scraper.extract")


@dataclass
class ExtractionResult:
    url: str
    text: str
    method: str  # "trafilatura" | "playwright+trafilatura" | "beautifulsoup"
    title: str | None
    char_count: int


class ExtractionError(Exception):
    """Raised when no strategy could retrieve usable content."""


def _sufficient(text: str | None) -> bool:
    return bool(text) and len(text.strip()) >= config.MIN_CONTENT_CHARS


def _fetch_static(url: str) -> str:
    """Fetch raw HTML over plain HTTP."""
    headers = {"User-Agent": config.USER_AGENT}
    with httpx.Client(
        timeout=config.HTTP_TIMEOUT, follow_redirects=True, headers=headers
    ) as client:
        resp = client.get(url)
        resp.raise_for_status()
        return resp.text


def _fetch_rendered(url: str) -> str:
    """Render the page with headless Chromium and return its HTML."""
    # Imported lazily so the service can boot even if Playwright/Chromium
    # is not installed, as long as the fallback is disabled.
    from playwright.sync_api import sync_playwright

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        try:
            page = browser.new_page(user_agent=config.USER_AGENT)
            page.goto(url, wait_until="networkidle", timeout=config.PLAYWRIGHT_TIMEOUT_MS)
            return page.content()
        finally:
            browser.close()


def _trafilatura_extract(html: str, url: str) -> str | None:
    return trafilatura.extract(
        html,
        url=url,
        include_comments=False,
        include_tables=True,
        favor_recall=True,
    )


def _title_of(html: str) -> str | None:
    soup = BeautifulSoup(html, "lxml")
    if soup.title and soup.title.string:
        return soup.title.string.strip()
    return None


def _beautifulsoup_extract(html: str) -> str:
    soup = BeautifulSoup(html, "lxml")
    for tag in soup(["script", "style", "noscript", "template"]):
        tag.decompose()
    text = soup.get_text(separator="\n")
    # Collapse runs of blank lines produced by stripped markup.
    lines = [line.strip() for line in text.splitlines()]
    return "\n".join(line for line in lines if line)


def extract_content(url: str) -> ExtractionResult:
    """Run the extraction chain and return the first sufficient result."""
    last_html: str | None = None

    # --- Strategy 1: static HTML -> trafilatura ---------------------------
    try:
        last_html = _fetch_static(url)
        text = _trafilatura_extract(last_html, url)
        if _sufficient(text):
            return ExtractionResult(
                url=url,
                text=text.strip(),  # type: ignore[union-attr]
                method="trafilatura",
                title=_title_of(last_html),
                char_count=len(text.strip()),  # type: ignore[union-attr]
            )
        logger.info("trafilatura insufficient for %s, trying Playwright", url)
    except Exception as exc:  # network error, bad status, etc.
        logger.warning("static fetch failed for %s: %s", url, exc)

    # --- Strategy 2: Playwright-rendered HTML -> trafilatura --------------
    if config.PLAYWRIGHT_ENABLED:
        try:
            rendered = _fetch_rendered(url)
            last_html = rendered
            text = _trafilatura_extract(rendered, url)
            if _sufficient(text):
                return ExtractionResult(
                    url=url,
                    text=text.strip(),  # type: ignore[union-attr]
                    method="playwright+trafilatura",
                    title=_title_of(rendered),
                    char_count=len(text.strip()),  # type: ignore[union-attr]
                )
            logger.info("Playwright+trafilatura insufficient for %s, trying BeautifulSoup", url)
        except Exception as exc:
            logger.warning("Playwright render failed for %s: %s", url, exc)
    else:
        logger.info("Playwright fallback disabled; skipping render step")

    # --- Strategy 3: BeautifulSoup plain text (last resort) --------------
    if last_html is not None:
        text = _beautifulsoup_extract(last_html)
        if _sufficient(text):
            return ExtractionResult(
                url=url,
                text=text.strip(),
                method="beautifulsoup",
                title=_title_of(last_html),
                char_count=len(text.strip()),
            )

    raise ExtractionError(
        f"No strategy produced at least {config.MIN_CONTENT_CHARS} characters of content"
    )
