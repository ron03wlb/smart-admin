FROM python:3.11-slim

LABEL maintainer="SmartAdmin AI Team"
LABEL description="CrewAI Agent Runner for SmartAdmin"

WORKDIR /app

# 安裝系統依賴（Java、Git、Gradle）
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-21-jdk \
    gradle \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 設置 Java 環境變量
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# 安裝 Python 依賴
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 複製 CrewAI 代碼
COPY crews/ ./crews/
COPY tools/ ./tools/
COPY monitoring/ ./monitoring/

# 複製入口腳本
COPY run_analyzer_crew.py .
COPY run_developer_crew.py .
COPY run_qa_crew.py .
COPY run_meta_crew.py .

# 創建工作區目錄
RUN mkdir -p /workspace

# 健康檢查（檢查 Python 和 Java）
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD python --version && java --version

# 非 root 用戶運行
RUN useradd -m -u 1000 agentuser && \
    chown -R agentuser:agentuser /app /workspace
USER agentuser

# 默認執行 Analyzer Crew
CMD ["python", "run_analyzer_crew.py"]
