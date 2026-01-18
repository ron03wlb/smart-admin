# Smart Admin 21 - Docker 開發環境

本目錄包含 Smart Admin 21 項目的 Docker 開發環境配置。

## 📦 服務列表

| 服務       | 容器名稱      | 端口 | 說明                      |
| ---------- | ------------- | ---- | ------------------------- |
| PostgreSQL | sa21-postgres | 5432 | 數據庫（可切換 Supabase） |
| Redis      | sa21-redis    | 6379 | 緩存服務                  |
| Kafka      | sa21-kafka    | 9094 | 消息隊列（KRaft 無 ZK）   |
| Nginx      | sa21-nginx    | 80   | 反向代理                  |
| Frontend   | sa21-web      | 3000 | 前端應用（可選）          |

## 🚀 快速開始

### 1. 啟動基礎服務

```bash
# 進入 docker 目錄
cd docker

# 啟動所有基礎服務（不含前端）
docker compose up -d

# 查看服務狀態
docker compose ps

# 查看日誌
docker compose logs -f
```

### 2. 啟動包含前端

```bash
# 啟動所有服務（包含前端）
docker compose --profile frontend up -d
```

### 3. 停止服務

```bash
# 停止所有服務
docker compose down

# 停止並刪除數據卷（⚠️ 會丟失數據）
docker compose down -v
```

## 🔧 配置說明

### 環境變量

編輯 `.env` 文件修改配置：

```bash
# 複製範例配置
cp .env.example .env

# 編輯配置
vim .env
```

### 切換到 Supabase

1. 編輯 `.env` 文件：

```bash
DB_MODE=supabase
SUPABASE_HOST=your-project.supabase.co
SUPABASE_PASSWORD=your-password
```

2. 修改應用配置 `sa-base.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${SUPABASE_HOST}:5432/postgres
    username: postgres
    password: ${SUPABASE_PASSWORD}
```

3. 停止本地 PostgreSQL（可選）：

```bash
docker compose stop postgres
```

## 📁 目錄結構

```
docker/
├── docker-compose.yml          # 主配置
├── docker-compose.override.yml # 開發環境覆蓋
├── .env                        # 環境變量
├── .env.example                # 環境變量範例
├── postgres/
│   └── init.sql                # 數據庫初始化
├── redis/
│   └── redis.conf              # Redis 配置
├── nginx/
│   ├── nginx.conf              # Nginx 主配置
│   └── conf.d/
│       └── default.conf        # 站點配置
└── README.md                   # 本文件
```

## 🔍 常用命令

```bash
# 查看服務狀態
docker compose ps

# 查看特定服務日誌
docker compose logs -f postgres
docker compose logs -f kafka

# 進入容器
docker exec -it sa21-postgres psql -U smartadmin -d smart_admin
docker exec -it sa21-redis redis-cli

# 重啟單個服務
docker compose restart redis

# 重建服務
docker compose up -d --build frontend
```

## 🔧 故障排除

### PostgreSQL 連接失敗

```bash
# 檢查容器狀態
docker compose ps postgres

# 查看日誌
docker compose logs postgres

# 嘗試連接
docker exec -it sa21-postgres psql -U smartadmin -d smart_admin
```

### Kafka 無法啟動

```bash
# 查看 Kafka 日誌（KRaft 模式無需 Zookeeper）
docker compose logs kafka

# 重啟 Kafka
docker compose restart kafka

# 進入 Kafka 容器測試
docker exec -it sa21-kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

### 端口衝突

```bash
# 檢查端口占用
lsof -i :5432
lsof -i :6379
lsof -i :9092

# 修改 .env 中的端口配置
POSTGRES_PORT=5433
REDIS_PORT=6380
```

## 📊 資源使用

開發環境預估資源使用：

| 服務       | 記憶體     | CPU |
| ---------- | ---------- | --- |
| PostgreSQL | ~200MB     | 低  |
| Redis      | ~50MB      | 低  |
| Kafka      | ~256MB     | 中  |
| Nginx      | ~10MB      | 低  |
| **總計**   | **~520MB** | -   |

## 🔗 相關連結

- [Docker Compose 文檔](https://docs.docker.com/compose/)
- [PostgreSQL Docker](https://hub.docker.com/_/postgres)
- [Confluent Kafka](https://hub.docker.com/r/confluentinc/cp-kafka)
