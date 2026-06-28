# Prometheus Scraper Service

Stateless content-extraction microservice. Given a URL, it returns the page's
main text content as JSON. **It is fully decoupled from the backend** — no
shared code, no database access — and is designed to be lifted into its own
repository without changes.

## Extraction chain

1. **trafilatura** on statically-fetched HTML (main-content auto-detection)
2. **Playwright** (headless Chromium) renders the page, then trafilatura runs
   again — for JS-rendered sites
3. **BeautifulSoup** plain-text extraction as a last resort

The first strategy that yields ≥ `SCRAPER_MIN_CONTENT_CHARS` characters wins.
The response reports which `method` succeeded.

## API

```
POST /scrape
  { "url": "https://example.com/article" }

200 -> { "url", "title", "text", "method", "char_count" }
422 -> reachable but no usable content extracted
500 -> unexpected failure

GET /health -> { "status": "ok" }
```

## Local setup (without Docker)

```bash
cd scraper
python -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt
playwright install chromium        # one-time: downloads headless Chromium
uvicorn app.main:app --reload --port 8000
```

Smoke test:

```bash
curl -X POST http://localhost:8000/scrape \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com"}'
```

## Docker

```bash
docker build -t prometheus-scraper .
docker run -p 8000:8000 prometheus-scraper
```

The Docker image is based on `mcr.microsoft.com/playwright/python`, which ships
Chromium and its OS dependencies preinstalled.

## Config

See `.env.example`. All values are optional; defaults are baked in.
