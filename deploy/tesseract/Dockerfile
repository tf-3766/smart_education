FROM ubuntu:24.04

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
       ca-certificates \
       tesseract-ocr \
       tesseract-ocr-chi-sim \
       tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /data

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD tesseract --version >/dev/null 2>&1 \
      && tesseract --list-langs 2>/dev/null | grep -q chi_sim \
      || exit 1

CMD ["sleep", "infinity"]