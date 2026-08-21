FROM python:3.14-slim

WORKDIR /app

# System libs Pillow needs at runtime.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libjpeg62-turbo zlib1g \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10002 --create-home --home-dir /home/dressd dressd

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY --chown=10002:10002 app ./app

# Image decoding on attacker-supplied bytes has no business running as root.
USER 10002

EXPOSE 8000

# Uses the stdlib rather than adding curl to the image.
HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=5 \
    CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://localhost:8000/health', timeout=2).status == 200 else 1)"

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
