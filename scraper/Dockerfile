# Playwright's official Python image ships with Chromium + all OS deps
# preinstalled, which keeps this Dockerfile small and reliable.
FROM mcr.microsoft.com/playwright/python:v1.49.1-noble

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Browsers are already present in this base image, but re-running install is
# a no-op safeguard if the base image version drifts from the pinned client.
RUN playwright install chromium

COPY app ./app

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
