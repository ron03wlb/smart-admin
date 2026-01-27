# SmartAdmin Auto-Coding - Telegram Webhook Dockerfile
# Version: 1.0.0

# Base image: Python 3.11 Slim
FROM python:3.11-slim

# Maintainer information
LABEL maintainer="SmartAdmin Auto-Coding System"
LABEL version="1.0.0"

# Working directory
WORKDIR /app

# Copy dependency file
COPY requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app.py .

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD python -c "import requests; requests.get('http://localhost:8080/health', timeout=3)" || exit 1

# Start command
CMD ["python3", "app.py"]
