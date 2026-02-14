# SmartAdmin 微服務遷移實施檢查清單

**文檔類型**: 實施檢查清單
**目標讀者**: 開發團隊、DevOps 工程師、項目經理
**文檔版本**: 1.0.0
**創建日期**: 2026-02-03

---

## 📋 文檔目的

本文檔提供 **系統化的檢查清單**，確保實施過程不遺漏關鍵步驟：
1. ✅ Phase 1-4 每個階段的檢查清單
2. ✅ 每個任務的驗收標準
3. ✅ 回滾方案
4. ✅ 進度追蹤模板

---

## 📊 實施概覽

**總時間**: 8 weeks（方案 A：選擇性微服務化）

| Phase | 週期 | 關鍵產出 | 狀態 |
|-------|------|---------|------|
| Phase 1 | Week 1-2 | Nacos + Gateway 就緒 | ⏳ 待開始 |
| Phase 2 | Week 3-4 | Job Service 獨立部署 | ⏳ 待開始 |
| Phase 3 | Week 5-6 | Resource Service 獨立部署 | ⏳ 待開始 |
| Phase 4 | Week 7-8 | 測試與驗證 | ⏳ 待開始 |

---

## Phase 1: 基礎設施準備（Week 1-2）

**目標**: 建立 Spring Cloud 基礎設施，Gateway 代理模式運行

### 1.1 Nacos 部署

#### 任務清單

- [ ] **部署 Nacos 3 節點集群**
  - [ ] 節點 1 正常運行（端口 8848）
  - [ ] 節點 2 正常運行（端口 8849）
  - [ ] 節點 3 正常運行（端口 8850）
  - [ ] 集群狀態檢查通過
    ```bash
    curl http://localhost:8848/nacos/v1/core/cluster/nodes
    ```
  - [ ] 健康檢查通過
    ```bash
    curl http://localhost:8848/nacos/v1/console/health/liveness
    ```

- [ ] **配置 PostgreSQL 持久化**
  - [ ] 創建數據庫: `nacos_config`
    ```sql
    CREATE DATABASE nacos_config;
    ```
  - [ ] 執行初始化腳本
    ```bash
    psql -U postgres -d nacos_config -f nacos-mysql.sql
    ```
  - [ ] 驗證數據持久化
    - 重啟 Nacos 後配置仍存在
  - [ ] 配置自動備份（每日 2:00 AM）

- [ ] **配置 Nacos 命名空間**
  - [ ] 創建 `dev` 命名空間
  - [ ] 創建 `test` 命名空間
  - [ ] 創建 `prod` 命名空間

- [ ] **配置 Nacos 用戶權限（生產環境）**
  - [ ] 創建 `smartadmin` 服務帳號
  - [ ] 賦予命名空間權限
  - [ ] 測試鑑權是否生效

#### 驗收標準

- ✅ Nacos Console 可訪問: http://localhost:8848/nacos
- ✅ 3 節點集群狀態顯示為 UP
- ✅ 配置可持久化到 PostgreSQL
- ✅ 命名空間隔離生效

#### 回滾方案

- ❌ 如果 Nacos 部署失敗：
  - 刪除 Nacos 部署
    ```bash
    docker compose down nacos
    ```
  - 恢復本地配置文件（application.yml）
  - SmartAdmin 單體繼續運行（無影響）

#### 負責人與時間表

| 任務 | 負責人 | 開始日期 | 預計完成 | 實際完成 | 狀態 | 備註 |
|------|--------|---------|---------|---------|------|------|
| 部署 Nacos 集群 | DevOps | - | - | - | ⏳ 待開始 | - |
| PostgreSQL 持久化 | DBA | - | - | - | ⏳ 待開始 | - |
| 命名空間配置 | DevOps | - | - | - | ⏳ 待開始 | - |

---

### 1.2 API Gateway 部署

#### 任務清單

- [ ] **部署 Spring Cloud Gateway**
  - [ ] 創建 Gateway 模塊
  - [ ] 配置 Nacos 服務發現
    ```yaml
    spring:
      cloud:
        nacos:
          discovery:
            server-addr: localhost:8848
    ```
  - [ ] 配置路由規則（代理模式）
    ```yaml
    spring:
      cloud:
        gateway:
          routes:
            - id: smartadmin-monolith
              uri: http://localhost:1024
              predicates:
                - Path=/**
    ```
  - [ ] 啟動 Gateway（端口 8080）
  - [ ] 驗證路由轉發正常

- [ ] **整合 Sa-Token 認證**
  - [ ] 創建 SaTokenGatewayFilter
  - [ ] 實現 Token 驗證邏輯
  - [ ] 注入用戶上下文到 Header
    ```java
    mutatedRequest.header("X-User-Id", loginId.toString())
    ```
  - [ ] 測試認證流程

- [ ] **配置跨域（CORS）**
  - [ ] 添加 CorsConfiguration
    ```java
    config.addAllowedOrigin("http://localhost:8080");
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    ```
  - [ ] 測試跨域請求

#### 驗收標準

- ✅ Gateway 可訪問: http://localhost:8080
- ✅ 100% 流量轉發到單體應用
- ✅ 延遲增加 < 10ms
- ✅ 認證流程正常（登入、權限驗證）

#### 回滾方案

- ❌ 如果 Gateway 部署失敗：
  - 停止 Gateway
    ```bash
    docker compose down gateway
    ```
  - 前端直接訪問單體應用（http://localhost:1024）
  - SmartAdmin 單體繼續運行（無影響）

---

### 1.3 分散式追蹤配置

#### 任務清單

- [ ] **部署 Zipkin Server**
  - [ ] 啟動 Zipkin（端口 9411）
    ```bash
    docker compose up -d zipkin
    ```
  - [ ] 驗證 UI 可訪問: http://localhost:9411

- [ ] **配置 Micrometer Tracing**
  - [ ] 添加依賴（Gateway + 單體應用）
    ```kotlin
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    ```
  - [ ] 配置 Zipkin 端點
    ```yaml
    management:
      tracing:
        sampling:
          probability: 1.0  # 100% 採樣（開發環境）
      zipkin:
        tracing:
          endpoint: http://localhost:9411/api/v2/spans
    ```
  - [ ] 測試追蹤鏈路

#### 驗收標準

- ✅ Zipkin UI 顯示完整調用鏈路
- ✅ 每個請求都有 trace-id
- ✅ 延遲分析準確（P50/P95/P99）

---

### 1.4 配置中心遷移

#### 任務清單

- [ ] **遷移配置到 Nacos**
  - [ ] 創建 `smartadmin-dev.yaml` 配置（Nacos）
  - [ ] 遷移 `sa-base.yaml` 內容
  - [ ] 遷移 `sa-admin.yaml` 內容
  - [ ] 配置動態刷新
    ```yaml
    spring:
      cloud:
        nacos:
          config:
            server-addr: localhost:8848
            file-extension: yaml
            refresh-enabled: true
    ```
  - [ ] 測試配置變更實時生效

- [ ] **保留本地配置（雙運行模式）**
  - [ ] 本地 YAML 作為備份
  - [ ] Nacos 配置優先級更高
  - [ ] 確保回滾時可用

#### 驗收標準

- ✅ Nacos 配置變更 < 1 秒生效
- ✅ 本地 YAML 作為備份可用

---

## Phase 2: Job Service 拆分（Week 3-4）

**目標**: 首個微服務獨立部署，驗證微服務化可行性

### 2.1 創建 Job Service 模塊

#### 任務清單

- [ ] **創建 job-service 模塊**
  - [ ] 從 `sa-admin` 提取 Job 相關代碼
    - Controller: `JobController`
    - Service: `JobService`
    - Manager: `JobManager`
    - Dao: `JobDao`
    - Entity: `JobEntity`
  - [ ] 創建獨立模塊結構
    ```
    job-service/
    ├── src/main/java/.../job/
    │   ├── controller/
    │   ├── service/
    │   ├── manager/
    │   ├── dao/
    │   └── domain/
    └── build.gradle.kts
    ```
  - [ ] 配置獨立端口（9203）

- [ ] **配置服務註冊**
  - [ ] 添加 Nacos 依賴
  - [ ] 配置服務名稱: `job-service`
    ```yaml
    spring:
      application:
        name: job-service
      cloud:
        nacos:
          discovery:
            server-addr: localhost:8848
    ```
  - [ ] 啟動服務，驗證註冊成功

- [ ] **保留核心優勢**
  - [ ] Manager 層保留（@Transactional）
  - [ ] ArchUnit 測試保留
  - [ ] Vavr Option 保留
  - [ ] 運行 ArchUnit 測試驗證

#### 驗收標準

- ✅ Job Service 成功註冊到 Nacos
- ✅ ArchUnit 測試通過
- ✅ 所有 Job API 正常工作

---

### 2.2 數據庫 Schema 遷移

#### 任務清單

- [ ] **創建獨立 Schema**
  - [ ] 創建 `sa_job` Schema
    ```sql
    CREATE SCHEMA sa_job;
    ```
  - [ ] 複製 `t_job` 表結構和數據
    ```sql
    CREATE TABLE sa_job.t_job AS SELECT * FROM public.t_job;
    ```
  - [ ] 驗證數據完整性（行數一致）

- [ ] **配置數據源**
  - [ ] Job Service 連接到 `sa_job` Schema
    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:5432/smartadmin?currentSchema=sa_job
    ```
  - [ ] 單體應用繼續使用 `public` Schema

#### 驗收標準

- ✅ 數據遷移無丟失
- ✅ Job Service 讀寫正確 Schema

---

### 2.3 Gateway 路由配置

#### 任務清單

- [ ] **添加 Job Service 路由**
  - [ ] 配置路由規則
    ```yaml
    spring:
      cloud:
        gateway:
          routes:
            - id: job-service
              uri: lb://job-service
              predicates:
                - Path=/api/job/**
              filters:
                - StripPrefix=1
    ```
  - [ ] 測試路由轉發

- [ ] **灰度發布**
  - [ ] 10% 流量到 Job Service
  - [ ] 觀察 24 小時無錯誤
  - [ ] 50% 流量到 Job Service
  - [ ] 觀察 24 小時無錯誤
  - [ ] 100% 流量到 Job Service

#### 驗收標準

- ✅ Job API 回應時間 < 100ms (P95)
- ✅ 48 小時零錯誤
- ✅ 數據一致性驗證通過

---

## Phase 3: Resource Service 拆分（Week 5-6）

**目標**: 第二個微服務獨立部署

### 3.1 創建 Resource Service 模塊

#### 任務清單

- [ ] **提取 Resource 相關代碼**
  - [ ] File Service (文件上傳/下載)
  - [ ] Mail Service (郵件發送)
  - [ ] SMS Service (短信發送)
  - [ ] 創建 `resource-service` 模塊

- [ ] **配置服務註冊**
  - [ ] 服務名稱: `resource-service`
  - [ ] 端口: 9204
  - [ ] 註冊到 Nacos

#### 驗收標準

- ✅ Resource Service 成功註冊
- ✅ 所有 Resource API 正常工作

---

### 3.2 MinIO 集成（文件服務）

#### 任務清單

- [ ] **部署 MinIO**
  - [ ] 啟動 MinIO Server
    ```bash
    docker compose up -d minio
    ```
  - [ ] 創建 Bucket: `smartadmin-files`
  - [ ] 配置訪問密鑰

- [ ] **配置 File Service**
  - [ ] 集成 MinIO SDK
  - [ ] 實現文件上傳/下載 API
  - [ ] 測試文件存取

#### 驗收標準

- ✅ 文件上傳成功率 > 99.9%
- ✅ 文件下載延遲 < 100ms

---

## Phase 4: 測試與驗證（Week 7-8）

**目標**: 全面測試，確保生產就緒

### 4.1 功能測試

#### 任務清單

- [ ] **API 功能測試**
  - [ ] Job Service 所有 API 測試通過
  - [ ] Resource Service 所有 API 測試通過
  - [ ] Gateway 路由測試通過

- [ ] **認證授權測試**
  - [ ] 登入流程正常
  - [ ] Token 驗證正常
  - [ ] 權限控制正常

#### 驗收標準

- ✅ 所有 API 測試通過（200 個測試用例）
- ✅ 認證流程無問題

---

### 4.2 性能測試

#### 任務清單

- [ ] **壓力測試**
  - [ ] JMeter 測試計劃
  - [ ] 1000 RPS 持續 10 分鐘
  - [ ] P95 延遲 < 200ms
  - [ ] 錯誤率 < 0.1%

- [ ] **分散式追蹤驗證**
  - [ ] Zipkin 鏈路完整
  - [ ] 瓶頸分析報告

#### 驗收標準

- ✅ 性能指標達標
- ✅ 無性能瓶頸

---

### 4.3 監控告警配置

#### 任務清單

- [ ] **Prometheus + Grafana**
  - [ ] 導入儀表盤
  - [ ] 配置 P0/P1/P2 告警規則
  - [ ] 測試告警通知

- [ ] **日誌聚合**
  - [ ] ELK Stack 配置
  - [ ] 日誌查詢測試

#### 驗收標準

- ✅ 監控指標完整
- ✅ 告警通知正常

---

### 4.4 文檔與培訓

#### 任務清單

- [ ] **更新文檔**
  - [ ] 架構圖更新
  - [ ] API 文檔更新
  - [ ] 運維手冊更新

- [ ] **團隊培訓**
  - [ ] 開發團隊培訓（2 小時）
  - [ ] 運維團隊培訓（2 小時）

#### 驗收標準

- ✅ 文檔完整
- ✅ 團隊培訓完成

---

## 📋 進度追蹤模板

### 總體進度

| Phase | 狀態 | 完成度 | 開始日期 | 預計完成 | 實際完成 | 備註 |
|-------|------|--------|---------|---------|---------|------|
| Phase 1 | ⏳ 待開始 | 0% | - | Week 2 | - | - |
| Phase 2 | ⏳ 待開始 | 0% | - | Week 4 | - | - |
| Phase 3 | ⏳ 待開始 | 0% | - | Week 6 | - | - |
| Phase 4 | ⏳ 待開始 | 0% | - | Week 8 | - | - |

### 詳細任務追蹤

| 任務 ID | 任務名稱 | 負責人 | 優先級 | 狀態 | 開始日期 | 預計完成 | 實際完成 | 阻塞問題 |
|---------|---------|--------|--------|------|---------|---------|---------|---------|
| 1.1.1 | 部署 Nacos 集群 | DevOps | P0 | ⏳ 待開始 | - | - | - | - |
| 1.1.2 | PostgreSQL 持久化 | DBA | P0 | ⏳ 待開始 | - | - | - | - |
| 1.2.1 | 部署 API Gateway | DevOps | P0 | ⏳ 待開始 | - | - | - | - |
| 1.3.1 | 部署 Zipkin | DevOps | P1 | ⏳ 待開始 | - | - | - | - |
| 2.1.1 | 創建 Job Service 模塊 | 開發 | P0 | ⏳ 待開始 | - | - | - | - |
| ... | ... | ... | ... | ... | ... | ... | ... | ... |

---

## ⚠️ 風險與問題

### 風險登記表

| 風險 ID | 風險描述 | 影響 | 概率 | 緩解措施 | 負責人 | 狀態 |
|---------|---------|------|------|---------|--------|------|
| R-001 | Nacos 連接失敗 | 高 | 中 | 健康檢查腳本 + 備份配置 | DevOps | ⏳ 監控中 |
| R-002 | Gateway 路由錯誤 | 高 | 低 | 充分測試 + 灰度發布 | 開發 | ⏳ 監控中 |
| R-003 | 性能下降 > 100% | 中 | 中 | 性能測試 + 調優 | 開發 | ⏳ 監控中 |

### 問題追蹤表

| 問題 ID | 問題描述 | 影響 | 優先級 | 負責人 | 狀態 | 解決日期 |
|---------|---------|------|--------|--------|------|---------|
| I-001 | - | - | - | - | - | - |

---

## 📝 文檔維護

**文檔版本**: 1.0.0
**創建日期**: 2026-02-03
**最後更新**: 2026-02-03
**維護團隊**: SmartAdmin 架構組

**更新歷史**:
- v1.0.0 (2026-02-03): 初始版本，完整 4 個 Phase 檢查清單

---

**Happy Implementation! ✅**

使用本檢查清單確保實施過程不遺漏任何關鍵步驟！
