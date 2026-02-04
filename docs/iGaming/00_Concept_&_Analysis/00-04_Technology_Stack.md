# 技術選型標準 (Technology Stack)

> **版本**: 1.0.0
> **最後更新**: 2026-01-27
> **目的**: 定義 IGaming 平台的統一技術棧與架構標準

---

## 📋 目錄

- [技術選型原則](#技術選型原則)
- [核心技術棧總覽](#核心技術棧總覽)
- [後端技術棧](#後端技術棧)
- [前端技術棧](#前端技術棧)
- [數據存儲技術](#數據存儲技術)
- [基礎設施技術](#基礎設施技術)
- [安全與合規技術](#安全與合規技術)
- [第三方集成](#第三方集成)
- [版本要求與生命週期](#版本要求與生命週期)

---

## 🎯 技術選型原則

### 1. 選型決策標準

**必須滿足**：
- ✅ **性能要求**：支持高並發（10K+ TPS）、低延遲（<100ms P95）
- ✅ **可擴展性**：水平擴展能力、無狀態設計
- ✅ **可靠性**：99.95%+ 可用性、災難恢復能力
- ✅ **安全性**：符合 PCI DSS、GDPR、SOC 2 標準
- ✅ **社區支持**：活躍社區、長期維護、充足文檔
- ✅ **人才可得性**：主流技術、招聘容易、學習曲線合理

### 2. 評估維度權重

| 維度 | 權重 | 說明 |
|------|------|------|
| 性能 | 30% | 吞吐量、延遲、資源效率 |
| 成熟度 | 25% | 生產驗證、穩定性、案例數 |
| 成本 | 15% | 授權費、運維成本、人力成本 |
| 生態 | 15% | 社區、工具鏈、集成能力 |
| 安全 | 10% | 漏洞記錄、合規性、審計 |
| 可維護性 | 5% | 代碼質量、調試工具、監控 |

### 3. 選型決策流程

```text
1. 需求分析 → 2. 候選技術調研 → 3. POC驗證 → 4. 評分決策 → 5. 架構評審 → 6. 試點上線
```

---

## 🏗️ 核心技術棧總覽

### 技術架構全景圖

```text
┌────────────────────────────────────────────────────────┐
│                   Frontend Layer                        │
│   Web: React 18 + TypeScript + Vite                   │
│   Mobile: React Native 0.73 / Flutter 3.16            │
│   CMS: Next.js 14 (SSR/ISR)                           │
└────────────┬──────────────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────────────┐
│                   API Gateway                           │
│   Kong Gateway 3.5 + Rate Limiting + WAF              │
│   NGINX Plus (Backup)                                  │
└────────────┬──────────────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────────────┐
│                  Backend Services                       │
│   Java 21 (Spring Boot 3.2) - 核心業務                │
│   Node.js 20 (Fastify 4) - 實時服務                   │
│   Go 1.22 - 高性能組件（風控引擎、支付網關）          │
│   Python 3.12 (FastAPI) - ML風控模型                  │
└────────────┬──────────────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────────────┐
│                  Data & Message Layer                   │
│   PostgreSQL 16 (主庫) + Citus (分片)                 │
│   Redis 7.2 (緩存 + 會話 + 限流)                      │
│   Apache Kafka 3.6 (事件流)                           │
│   Elasticsearch 8.11 (審計日誌 + 搜索)                │
│   ClickHouse 23.12 (OLAP分析)                         │
└────────────┬──────────────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────────────┐
│              Infrastructure & Operations                │
│   Kubernetes 1.29 + Helm 3.13                         │
│   Docker 25.0                                          │
│   Terraform 1.7 (IaC)                                  │
│   ArgoCD 2.9 (GitOps)                                  │
│   Prometheus + Grafana (監控)                          │
│   Datadog / New Relic (APM)                           │
└───────────────────────────────────────────────────────┘
```

---

## ⚙️ 後端技術棧

### 1. 主力開發語言

#### Java 21 (LTS) + Spring Boot 3.2 ⭐ 推薦

**使用場景**：
- ✅ 核心業務服務（玩家、錢包、交易、活動）
- ✅ 複雜業務邏輯（流水計算、對賬系統）
- ✅ 高一致性要求的服務

**技術棧**：
```yaml
語言: Java 21 (LTS until 2029)
框架: Spring Boot 3.2.x
  - Spring Security 6.2 (認證授權)
  - Spring Data JPA 3.2 (ORM)
  - Spring Cloud 2023.0.x (微服務)
  - Spring Kafka 3.1 (事件驅動)

依賴注入: Spring Framework 6.1
ORM: Hibernate 6.4 + QueryDSL 5.1
API文檔: SpringDoc OpenAPI 2.3
序列化: Jackson 2.16 + Protobuf 3.25
驗證: Jakarta Validation 3.0
定時任務: Spring Scheduler + Quartz 2.3
分佈式鎖: Redisson 3.26
```

**選型理由**：
- ✅ 生態成熟、社區活躍、人才充足
- ✅ 強類型系統、編譯期檢查、重構友好
- ✅ 豐富的企業級特性（事務、安全、緩存）
- ✅ Spring Boot自動配置、開箱即用
- ✅ Java 21 性能提升（Virtual Threads、G1GC優化）

**版本要求**：
- Java: ≥ 21 (推薦 21 LTS)
- Spring Boot: ≥ 3.2.0
- Spring Cloud: ≥ 2023.0.0

---

#### Node.js 20 LTS + Fastify 4 ⭐ 推薦

**使用場景**：
- ✅ 實時通信服務（WebSocket、SSE）
- ✅ 輕量級API服務（遊戲大廳、前端BFF）
- ✅ 快速原型開發

**技術棧**：
```yaml
運行時: Node.js 20 LTS (until 2026-04)
框架: Fastify 4.25 (高性能HTTP)
  - @fastify/websocket (WebSocket支持)
  - @fastify/jwt (JWT認證)
  - @fastify/cors (CORS)
  - @fastify/helmet (安全頭)

ORM: Prisma 5.8 (推薦) / TypeORM 0.3
緩存: ioredis 5.3
消息隊列: kafkajs 2.2
驗證: zod 3.22 / joi 17.12
測試: Vitest 1.2 + Supertest 6.3
```

**選型理由**：
- ✅ 單線程非阻塞I/O，適合高並發
- ✅ Fastify性能優於Express（3-5倍）
- ✅ TypeScript支持良好
- ✅ npm生態豐富、開發效率高
- ✅ 前後端語言統一、人才共享

**性能基準**：
- Throughput: ~50K req/s（單核心）
- Latency: <10ms P99（簡單查詢）

---

#### Go 1.22 ⭐ 推薦

**使用場景**：
- ✅ **高性能組件**：支付網關、風控引擎
- ✅ **低延遲服務**：限流、熔斷、負載均衡
- ✅ **系統工具**：數據同步、對賬腳本

**技術棧**：
```yaml
語言: Go 1.22
框架: Gin 1.9 / Fiber 2.52
ORM: GORM 1.25 + sqlx 1.3
緩存: go-redis 9.4
消息隊列: confluent-kafka-go 2.3
配置: Viper 1.18
日誌: zap 1.26 + lumberjack 2.2
測試: testify 1.8
```

**選型理由**：
- ✅ 編譯型語言、原生並發（goroutine）
- ✅ 內存佔用低（<Java/Node.js）
- ✅ 快速啟動（<1秒）
- ✅ 簡潔語法、易於維護
- ✅ 靜態類型、編譯期檢查

**性能優勢**：
- 啟動時間：~500ms（vs Java 5-10s）
- 內存佔用：~50MB（vs Java 200MB+）
- 並發性能：百萬級 goroutine

---

#### Python 3.12 + FastAPI ⭐ 專用場景

**使用場景**：
- ✅ **ML風控模型**：欺詐檢測、異常流水識別
- ✅ **數據分析**：BI報表、數據挖掘
- ✅ **自動化腳本**：數據遷移、批次處理

**技術棧**：
```yaml
語言: Python 3.12
Web框架: FastAPI 0.109
ML框架:
  - scikit-learn 1.4 (傳統ML)
  - XGBoost 2.0 / LightGBM 4.3 (GBDT)
  - TensorFlow 2.15 / PyTorch 2.2 (深度學習)
數據處理: pandas 2.2 + NumPy 1.26
ORM: SQLAlchemy 2.0
緩存: redis-py 5.0
```

**選型理由**：
- ✅ ML/AI生態最成熟
- ✅ 數據科學工具豐富
- ✅ 原型開發快速
- ❌ 性能不如 Java/Go（僅用於專用場景）

---

### 2. 微服務架構

#### Spring Cloud 2023.0.x

**核心組件**：
```yaml
服務註冊: Consul 1.17 (推薦) / Eureka 2.0
配置中心: Spring Cloud Config + Consul KV
服務網關: Spring Cloud Gateway 4.1
負載均衡: Spring Cloud LoadBalancer
熔斷限流: Resilience4j 2.2
鏈路追蹤: Micrometer Tracing + Zipkin 2.24
```

**vs Kubernetes Service Mesh**：
| 維度 | Spring Cloud | Istio Service Mesh |
|------|-------------|-------------------|
| 語言綁定 | Java Only | 語言無關 ⭐ |
| 性能開銷 | 低 ⭐ | 中（Sidecar代理）|
| 學習曲線 | 陡峭 | 更陡峭 |
| 可觀測性 | 良好 | 優秀 ⭐ |
| 社區 | Java社區 | CNCF ⭐ |

**推薦策略**：
- **Spring Cloud**：純Java微服務、團隊熟悉Spring
- **Istio**：多語言服務、雲原生架構、未來趨勢 ⭐

---

### 3. API設計標準

#### RESTful API ⭐ 主流

**規範**：
- **HTTP方法**：GET (查詢)、POST (創建)、PUT (替換)、PATCH (部分更新)、DELETE (刪除)
- **路徑命名**：`/api/v1/{resource}`、`/api/v1/{resource}/{id}`
- **版本策略**：URL版本（`/v1/`, `/v2/`）
- **響應格式**：JSON（統一封裝）

**統一響應格式**：
```json
{
  "code": 1000,
  "message": "Success",
  "data": { ... },
  "timestamp": "2026-01-27T10:00:00Z",
  "trace_id": "abc-123-def"
}
```

**詳細規範**：參見 [12-05 API設計標準](../07_Technical_Infrastructure_NEW/07-03-01_Design_Principles.md)

---

#### GraphQL（可選）

**使用場景**：
- 前端BFF（Backend for Frontend）
- 複雜關聯查詢（減少over-fetching）
- 移動端API（減少請求次數）

**技術選型**：
- Java: Spring GraphQL 1.2
- Node.js: Apollo Server 4.x

**推薦策略**：
- **內部API**：RESTful ⭐（標準化、緩存友好）
- **移動端API**：GraphQL（靈活性高）

---

## 🎨 前端技術棧

### 1. Web前端

#### React 18 + TypeScript + Vite ⭐ 推薦

**技術棧**：
```yaml
語言: TypeScript 5.3
框架: React 18.2
構建工具: Vite 5.0 (推薦) / Webpack 5.90
狀態管理: Zustand 4.4 (推薦) / Redux Toolkit 2.0
路由: React Router 6.21
UI庫:
  - Ant Design 5.12 (管理後台) ⭐
  - Material-UI 5.15 (玩家前台)
  - Tailwind CSS 3.4 (自定義UI)
表單: React Hook Form 7.49 + Zod 3.22
請求: TanStack Query 5.17 (React Query) + Axios 1.6
SSR框架: Next.js 14.1 (SEO友好)
測試: Vitest 1.2 + React Testing Library 14.1
```

**選型理由**：
- ✅ 生態最成熟、社區最活躍
- ✅ Virtual DOM性能優化
- ✅ Hooks簡化狀態邏輯
- ✅ TypeScript靜態檢查
- ✅ Vite開發體驗極佳（HMR <50ms）

---

#### Vue 3（備選）

**使用場景**：
- 團隊熟悉Vue
- 輕量級管理後台
- 快速原型開發

**技術棧**：
```yaml
框架: Vue 3.4 + Composition API
構建: Vite 5.0
狀態管理: Pinia 2.1
路由: Vue Router 4.2
UI庫: Element Plus 2.5 / Ant Design Vue 4.1
```

---

### 2. 移動端

#### React Native 0.73 ⭐ 推薦

**技術棧**：
```yaml
框架: React Native 0.73
語言: TypeScript 5.3
導航: React Navigation 6.1
狀態: Zustand 4.4 / Redux Toolkit 2.0
UI庫: React Native Paper 5.12 / NativeBase 3.4
熱更新: CodePush (Microsoft)
構建: EAS Build (Expo)
```

**選型理由**：
- ✅ 代碼複用率高（與Web共享邏輯）
- ✅ 熱更新能力（繞過應用商店審核）
- ✅ Facebook/Meta長期支持
- ✅ 第三方庫豐富
- ❌ 性能略遜原生（複雜動畫場景）

---

#### Flutter 3.16（備選）

**使用場景**：
- 高性能UI要求
- 複雜動畫效果
- 獨立移動端團隊

**技術棧**：
```yaml
框架: Flutter 3.16
語言: Dart 3.2
狀態管理: Riverpod 2.4 / Bloc 8.1
```

**選型理由**：
- ✅ 原生性能（編譯為機器碼）
- ✅ UI一致性（自繪引擎）
- ✅ Google官方支持
- ❌ 包體積較大（10MB+）
- ❌ Dart生態不如JavaScript

**推薦策略**：
- **React Native**：團隊已有React經驗、快速迭代 ⭐
- **Flutter**：追求極致性能、獨立移動端團隊

---

### 3. CMS管理後台

#### Next.js 14 + React 18 ⭐ 推薦

**技術棧**：
```yaml
框架: Next.js 14.1 (App Router)
渲染策略:
  - SSR (Server-Side Rendering) - SEO關鍵頁
  - ISR (Incremental Static Regeneration) - 內容頁
  - CSR (Client-Side Rendering) - 管理後台
後端: Next.js API Routes / tRPC 10.45
認證: NextAuth.js 4.24
部署: Vercel / Self-hosted
```

**選型理由**：
- ✅ SEO友好（SSR/ISR）
- ✅ 性能優秀（自動優化）
- ✅ 開發體驗好（文件路由）
- ✅ Vercel託管簡單

---

## 💾 數據存儲技術

### 1. 關係型數據庫

#### PostgreSQL 16 + Citus ⭐ 推薦

**使用場景**：
- ✅ **主數據庫**：玩家、錢包、交易、遊戲
- ✅ **OLTP業務**：高並發讀寫、事務一致性
- ✅ **水平擴展**：Citus分片（單表>1億行）

**技術棧**：
```yaml
數據庫: PostgreSQL 16.1
分片擴展: Citus 12.1 (分佈式PostgreSQL)
連接池: PgBouncer 1.21
備份: pgBackRest 2.49 + WAL-G 3.0
監控: pg_stat_statements + Prometheus Exporter
高可用: Patroni 3.2 + etcd 3.5
```

**選型理由**：
- ✅ ACID事務完整性（金融級）
- ✅ JSON/JSONB支持（靈活數據結構）
- ✅ 豐富索引類型（B-Tree、GIN、BRIN）
- ✅ 窗口函數、CTE（複雜分析查詢）
- ✅ Citus分片擴展（TB級數據）
- ✅ 開源免費、社區活躍

**版本要求**：
- PostgreSQL: ≥ 16.0
- Citus: ≥ 12.0

**vs MySQL 8.0**：
| 維度 | PostgreSQL | MySQL |
|------|-----------|-------|
| 事務隔離 | 4級完整支持 ⭐ | Repeatable Read默認 |
| JSON支持 | JSONB高效 ⭐ | JSON原生 |
| 窗口函數 | 完整 ⭐ | 完整 |
| 分片方案 | Citus ⭐ | Vitess / ShardingSphere |
| 複製延遲 | 低 ⭐ | 低 |
| 生態 | Java/Python/Go | PHP/Java |

**推薦**：PostgreSQL（企業級特性更豐富）⭐

---

### 2. 緩存層

#### Redis 7.2 ⭐ 推薦

**使用場景**：
- ✅ 熱點數據緩存（玩家會話、錢包餘額）
- ✅ 分佈式鎖（Redlock算法）
- ✅ 限流（Token Bucket、滑動窗口）
- ✅ 會話存儲（Session）
- ✅ 消息隊列（Stream）
- ✅ 排行榜（Sorted Set）

**技術棧**：
```yaml
緩存: Redis 7.2 (單線程優化)
高可用: Redis Sentinel 7.2 / Redis Cluster
持久化: AOF (always) + RDB (每小時)
客戶端:
  - Java: Redisson 3.26
  - Node.js: ioredis 5.3
  - Go: go-redis 9.4
監控: RedisInsight + Prometheus Exporter
```

**配置建議**：
```
# redis.conf
maxmemory: 80% 系統內存
maxmemory-policy: allkeys-lru
appendonly: yes
appendfsync: everysec
```

**選型理由**：
- ✅ 單線程無鎖、性能極高（10萬+QPS）
- ✅ 豐富數據結構（String/Hash/List/Set/ZSet/Stream）
- ✅ 生態成熟、客戶端齊全
- ✅ 持久化可靠（AOF+RDB）

---

### 3. 消息隊列

#### Apache Kafka 3.6 ⭐ 推薦

**使用場景**：
- ✅ **事件驅動架構**：交易事件、遊戲事件
- ✅ **數據管道**：CDC（Change Data Capture）
- ✅ **審計日誌**：不可篡改的日誌流
- ✅ **實時分析**：流式數據處理

**技術棧**：
```yaml
消息隊列: Apache Kafka 3.6
ZooKeeper替代: KRaft模式 (推薦)
Schema註冊: Confluent Schema Registry 7.5
流處理: Kafka Streams 3.6 / Apache Flink 1.18
監控: Kafka Exporter + Grafana
管理工具: Conduktor / Kafka UI
```

**vs RabbitMQ / AWS SQS**：
| 維度 | Kafka | RabbitMQ | AWS SQS |
|------|-------|----------|---------|
| 吞吐量 | 極高(百萬/s) ⭐ | 中(萬/s) | 高 |
| 延遲 | <10ms ⭐ | <5ms ⭐ | ~100ms |
| 持久化 | 磁盤日誌 ⭐ | 可選 | 自動 ⭐ |
| 順序保證 | 分區順序 ⭐ | 隊列順序 | FIFO隊列 |
| 回溯消費 | 支持 ⭐ | 不支持 | 不支持 |
| 運維複雜度 | 高 | 中 | 低 ⭐ |

**推薦**：Kafka（高吞吐、持久化、事件溯源）⭐

---

### 4. 搜索引擎

#### Elasticsearch 8.11 ⭐ 推薦

**使用場景**：
- ✅ **審計日誌搜索**（09-02）
- ✅ **全文檢索**（遊戲名稱、玩家搜索）
- ✅ **日誌分析**（ELK Stack）

**技術棧**：
```yaml
搜索引擎: Elasticsearch 8.11
日誌收集: Logstash 8.11 / Filebeat 8.11
可視化: Kibana 8.11
客戶端: Official REST Client
```

**vs OpenSearch**：
- Elasticsearch: 商業化更好、功能更全 ⭐
- OpenSearch: 開源友好、AWS支持

---

### 5. OLAP分析

#### ClickHouse 23.12 ⭐ 推薦

**使用場景**：
- ✅ **BI報表**：玩家行為分析、營收報表
- ✅ **實時指標**：DAU/MAU、GGR/NGR
- ✅ **數據倉庫**：ODS→DWD→DWS→ADS

**技術棧**：
```yaml
OLAP引擎: ClickHouse 23.12
數據同步:
  - CDC: Debezium + Kafka Connect
  - ETL: Apache Airflow 2.8
可視化: Superset 3.0 / Metabase 0.48
```

**vs StarRocks / Apache Druid**：
| 維度 | ClickHouse | StarRocks | Druid |
|------|-----------|-----------|-------|
| 查詢性能 | 極快 ⭐ | 極快 ⭐ | 快 |
| 寫入性能 | 高 ⭐ | 中 | 高 |
| SQL兼容 | 高 ⭐ | 高 ⭐ | 中 |
| 學習曲線 | 中 | 中 | 陡峭 |
| 社區 | 大 ⭐ | 中 | 小 |

**推薦**：ClickHouse（性能卓越、SQL友好）⭐

---

## 🏗️ 基礎設施技術

### 1. 容器化與編排

#### Kubernetes 1.29 + Docker ⭐ 推薦

**技術棧**：
```yaml
容器運行時: Docker 25.0 / containerd 1.7
容器編排: Kubernetes 1.29
包管理: Helm 3.13
服務網格: Istio 1.20 (可選)
Ingress: NGINX Ingress Controller 1.9
存儲: Rook Ceph 1.13 / Longhorn 1.5
```

**vs 虛擬機 / Serverless**：
| 維度 | Kubernetes | 虛擬機 | Serverless |
|------|-----------|-------|-----------|
| 資源利用率 | 高 ⭐ | 低 | 極高 ⭐ |
| 啟動速度 | 快(<10s) ⭐ | 慢(分鐘) | 極快(<1s) ⭐ |
| 成本 | 中 | 高 | 中 ⭐ |
| 運維複雜度 | 高 | 中 ⭐ | 低 ⭐ |
| 狀態管理 | 複雜 | 簡單 ⭐ | 無狀態 ⭐ |

**推薦**：Kubernetes（雲原生標準）⭐

---

### 2. CI/CD

#### GitLab CI + ArgoCD ⭐ 推薦

**技術棧**：
```yaml
代碼倉庫: GitLab 16.8 / GitHub Enterprise
CI/CD: GitLab CI 16.8
GitOps: ArgoCD 2.9
容器倉庫: Harbor 2.10 (自託管) / Docker Hub
鏡像掃描: Trivy 0.48
```

**vs Jenkins / GitHub Actions**：
| 維度 | GitLab CI | GitHub Actions | Jenkins |
|------|-----------|----------------|---------|
| 配置即代碼 | ✅ ⭐ | ✅ ⭐ | Plugin |
| K8s集成 | 原生 ⭐ | 第三方 | Plugin |
| 成本 | 開源免費 ⭐ | 付費 | 開源免費 ⭐ |
| 學習曲線 | 中 | 低 ⭐ | 陡峭 |

---

### 3. 監控與告警

#### Prometheus + Grafana ⭐ 推薦

**技術棧**：
```yaml
指標收集: Prometheus 2.49
時序存儲: VictoriaMetrics 1.96 (長期存儲)
可視化: Grafana 10.3
告警: Alertmanager 0.26
鏈路追蹤: Jaeger 1.53 / Tempo 2.3
日誌: Loki 2.9 (輕量) / Elasticsearch 8.11 (重度)
APM: Datadog / New Relic (商業) / SkyWalking 9.7 (開源)
```

---

### 4. 基礎設施即代碼 (IaC)

#### Terraform 1.7 ⭐ 推薦

**技術棧**：
```yaml
IaC工具: Terraform 1.7
配置管理: Ansible 9.1 (補充)
雲供應商:
  - AWS: 完整支持 ⭐
  - GCP: 完整支持 ⭐
  - Azure: 完整支持 ⭐
  - Alibaba Cloud: 完整支持
狀態後端: Terraform Cloud / S3 + DynamoDB
```

---

## 🔐 安全與合規技術

### 1. 加密技術

**數據加密標準**：
```yaml
傳輸加密: TLS 1.3
對稱加密: AES-256-GCM
非對稱加密: RSA-4096 / ECDSA P-256
密鑰管理: AWS KMS / Azure Key Vault / HashiCorp Vault
密碼哈希: Argon2id (m=65536, t=3, p=4)
盲索引: HMAC-SHA256
```

**詳細規範**：參見 [09-03 數據安全標準](../09_System_Security/09-03_Data_Security_Standard.md)

---

### 2. 身份認證與授權

**技術選型**：
```yaml
認證協議: OAuth 2.0 + OpenID Connect (OIDC)
JWT簽名: RS256 (RSA-SHA256)
MFA: TOTP (Time-based OTP) - RFC 6238
SSO: Keycloak 23.0 / Auth0 (商業)
RBAC: 自研 (參見 09-01 Admin RBAC)
```

---

### 3. 合規工具

```yaml
漏洞掃描: Trivy 0.48 + SonarQube 10.3
SAST: SonarQube 10.3 Community
DAST: OWASP ZAP 2.14
依賴檢查: Snyk / Dependabot
滲透測試: Burp Suite Professional
合規審計: Vanta (SOC 2) / Drata (多合規)
```

---

## 🔌 第三方集成

### 1. 支付服務商 (PSP)

**推薦集成**：
- **Nuvei** (iGaming專業)
- **Paysafe** (高風險商戶友好)
- **Adyen** (全球化)
- **Stripe** (開發者友好)

**集成標準**：參見 [02-02 支付網關集成](../02_Finance_Center/02-02_Payment_Gateway_Integration.md)

---

### 2. 遊戲供應商 (GP)

**聚合平台**：
- **SOFTSWISS Game Aggregator** (15K+ 遊戲)
- **Hub88** (100+ 供應商)
- **Groove Gaming** (快速集成)

**集成標準**：參見 [03-01 遊戲集成標準](../03_Game_Center/03-01_Game_Integration_Standard.md)

---

### 3. KYC/AML服務

**推薦供應商**：
- **Sumsub** (全球覆蓋)
- **iDenfy** (快速驗證)
- **Onfido** (AI驗證)
- **Persona** (靈活配置)

---

## 📅 版本要求與生命週期

### 最低版本要求

| 技術 | 最低版本 | 推薦版本 | LTS截止 |
|------|---------|---------|---------|
| **後端** |
| Java | 17 | 21 ⭐ | 2029-09 |
| Spring Boot | 3.0.0 | 3.2.x ⭐ | - |
| Node.js | 18 | 20 LTS ⭐ | 2026-04 |
| Go | 1.21 | 1.22 ⭐ | - |
| Python | 3.10 | 3.12 ⭐ | 2028-10 |
| **前端** |
| React | 18.0 | 18.2 ⭐ | - |
| TypeScript | 5.0 | 5.3 ⭐ | - |
| Next.js | 13 | 14 ⭐ | - |
| **數據庫** |
| PostgreSQL | 14 | 16 ⭐ | 2028-11 |
| Redis | 7.0 | 7.2 ⭐ | - |
| Kafka | 3.0 | 3.6 ⭐ | - |
| Elasticsearch | 8.0 | 8.11 ⭐ | - |
| ClickHouse | 23.3 | 23.12 ⭐ | - |
| **基礎設施** |
| Kubernetes | 1.27 | 1.29 ⭐ | 2024-12 |
| Docker | 24.0 | 25.0 ⭐ | - |

### 升級策略

**主版本升級**：
- 評估期：1個月（POC測試）
- 灰度期：2個月（生產驗證）
- 全量期：1個月（全量上線）

**次版本升級**：
- 季度更新（3個月）
- 安全補丁立即應用

---

## 📚 相關文檔

### 技術參考
- [00-00 文檔導航地圖](./00-00_Document_Map.md) - 全局導航
- [12-05 API設計標準](../07_Technical_Infrastructure_NEW/07-03-01_Design_Principles.md) - API規範
- [12-03 網關架構](../07_Technical_Infrastructure_NEW/07-02-01_Gateway_Core.md) - 網關設計
- [12-01 部署架構](../07_Technical_Infrastructure_NEW/07-01_Deployment.md) - CI/CD

### 安全參考
- [09-03 數據安全標準](../09_System_Security/09-03_Data_Security_Standard.md) - 加密規範
- [09-01 管理後台RBAC](../05_Platform_Governance_NEW/05-02_RBAC_Permissions.md) - 權限設計

---

**文檔版本**: 1.0.0
**維護團隊**: Architecture Team & Platform Team
**下次審閱**: 2026-04-27（每季度審閱）
