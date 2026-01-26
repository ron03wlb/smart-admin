FROM python:3.11-slim

LABEL maintainer="SmartAdmin AI Team"
LABEL description="Telegram Webhook Service for SmartAdmin AI"

WORKDIR /app

# 安裝依賴
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 複製應用代碼
COPY app.py .

# 健康檢查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD python -c "import requests; requests.get('http://localhost:8080/health')"

# 非 root 用戶運行
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

# 使用 Gunicorn 生產服務器
CMD ["gunicorn", "--bind", "0.0.0.0:8080", "--workers", "2", "--timeout", "60", "app:app"]
