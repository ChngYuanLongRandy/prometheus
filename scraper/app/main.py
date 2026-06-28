"""FastAPI entrypoint for the Prometheus scraper microservice.

Contract with the backend (HTTP only, no shared code/DB):
  POST /scrape   { "url": "https://..." }  -> extracted content
  GET  /health                              -> liveness probe

This service is intentionally extractable into its own repository: it depends
on nothing from the backend and owns no persistent state.
"""
from __future__ import annotations

import logging

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field, HttpUrl

from .extract import ExtractionError, extract_content

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")

app = FastAPI(title="Prometheus Scraper", version="0.1.0")


class ScrapeRequest(BaseModel):
    url: HttpUrl


class ScrapeResponse(BaseModel):
    url: str
    title: str | None = None
    text: str
    method: str = Field(description="Extraction strategy that succeeded")
    char_count: int


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/scrape", response_model=ScrapeResponse)
def scrape(req: ScrapeRequest) -> ScrapeResponse:
    url = str(req.url)
    try:
        result = extract_content(url)
    except ExtractionError as exc:
        # 422: reachable but no usable content extracted.
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except Exception as exc:  # unexpected failure
        raise HTTPException(status_code=500, detail=f"scrape failed: {exc}") from exc

    return ScrapeResponse(
        url=result.url,
        title=result.title,
        text=result.text,
        method=result.method,
        char_count=result.char_count,
    )
