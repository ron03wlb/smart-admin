---
trigger: on_demand
description: SmartAdmin 開發環境初始化設定
tags: [setup, environment, java-21, postgresql, vavr, docker]
required_rules:
  - rules/05-postgresql-basics.md
  - rules/08-vavr-fundamentals.md
  - rules/09-mybatis-plus-core.md

execution_order:
  - step: verify_prerequisites
    description: 驗證前置條件
    commands:
      - java -version
      - mvn -version
      - docker --version
    expected_output: Java 21, Maven 3.8+, Docker installed
    validation: 所有命令返回正確版本

  - step: start_databases
    description: 啟動 PostgreSQL 和 Redis
    commands:
      - cd .agent/configs && docker-compose up -d postgres redis
    expected_output: 2 containers running
    validation: docker ps shows postgres and redis

  - step: init_database
    description: 初始化數據庫架構
    commands:
      - docker exec -i smartadmin-postgres psql -U smartadmin -d smart_admin_v3 < .agent/configs/init-scripts/01-init.sql
    expected_output: SQL executed successfully
    validation: 表結構創建完成

  - step: compile_project
    description: 編譯項目
    commands:
      - cd smart-admin-api-java21-springboot3 && mvn clean compile
    expected_output: BUILD SUCCESS
    validation: target/ 目錄已創建

  - step: run_arch_tests
    description: 運行架構測試
    commands:
      - mvn test -Dtest=ArchitectureTest
    expected_output: Tests run, 0 failures
    validation: 所有 ArchUnit 規則通過

last_updated: 2025-01-13
---

# SmartAdmin 開發環境初始化

本指南將引導您設置 SmartAdmin (Java 21 + Spring Boot 3.5.4 + PostgreSQL) 開發環境。

---

## 🤖 AI 執行指南

### 何時應用此 Workflow
- ✅ 用戶第一次使用項目
- ✅ 用戶說 "環境設置" / "初始化"
- ✅ 其他 workflow 執行前環境未就緒
- ✅ 開發環境損壞需要重置

### 執行檢查清單
自動化執行所有步驟，每步完成後確認：
- [ ] Java 21 已安裝並配置
- [ ] Maven 能識別 Java 21
- [ ] Docker 服務正常運行
- [ ] PostgreSQL 容器啟動成功
- [ ] Redis 容器啟動成功
- [ ] 數據庫連接正常
- [ ] 項目編譯成功
- [ ] ArchUnit 測試通過

### 執行流程決策樹
```
用戶要求: "初始化開發環境"
  ├─ 1️⃣ 檢查前置條件
  │   ├─ Java 21 installed?
  │   │   ├─ NO → 提示安裝指令
  │   │   └─ YES → 繼續
  │   ├─ Maven installed?
  │   │   ├─ NO → 提示安裝指令
  │   │   └─ YES → 繼續
  │   └─ Docker installed?
  │       ├─ NO → 提示安裝指令
  │       └─ YES → 繼續
  │
  ├─ 2️⃣ 啟動數據庫服務
  │   ├─ 執行: docker-compose up -d
  │   ├─ 等待: PostgreSQL 就緒（最多 30秒）
  │   └─ 驗證: docker ps 顯示 running
  │
  ├─ 3️⃣ 初始化數據庫
  │   ├─ 連接測試: psql -c "SELECT 1"
  │   ├─ 執行初始化腳本
  │   └─ 驗證: 表結構創建成功
  │
  ├─ 4️⃣ 編譯項目
  │   ├─ 執行: mvn clean compile
  │   ├─ 驗證依賴: Vavr, PostgreSQL Driver
  │   └─ 確認: BUILD SUCCESS
  │
  └─ 5️⃣ 運行架構測試
      ├─ 執行: mvn test -Dtest=ArchitectureTest
      ├─ 驗證: 分層架構正確
      ├─ 驗證: Vavr 依賴正確
      └─ 確認: 0 failures
```

### 錯誤處理
如果任何步驟失敗：
- 🔍 定位失敗步驟
- 📋 查看詳細日誌
- 📖 參考 [java-failure-recovery.md](./java-failure-recovery.md)
- 🔧 修復後重新執行

---

---

## 一、前置條件檢查

### 1. 驗證 Java 版本
```bash
java -version
```
**期望輸出**: Java 21 或更高版本

### 2. 檢查 Maven 安裝
```bash
mvn -version
```
**期望輸出**: Maven 3.8+ 且 Java version 21

### 3. 檢查 Docker（推薦）
```bash
docker --version
docker-compose --version
```
**用途**: 快速啟動 PostgreSQL + Redis 開發環境

---

## 二、項目結構

### 導航到 Java 21 版本
```bash
cd smart-admin-api-java21-springboot3
```

### 關鍵目錄
```
smart-admin-api-java21-springboot3/
├── sa-base/          # 共享基礎庫
│   ├── common/       # 核心 DTO、工具類
│   ├── config/       # Spring 配置
│   └── module/       # 可複用支持模組
└── sa-admin/         # 主應用
    ├── module/
    │   ├── business/ # 業務邏輯模組
    │   └── system/   # 系統模組
    └── src/main/resources/
        ├── application.yaml
        └── dev/sa-base.yaml
```

---

## 三、數據庫設置（PostgreSQL）

### 使用 Docker Compose（推薦）

```bash
# 啟動服務
cd .agent/configs
docker-compose up -d postgres redis

# 驗證
docker-compose ps  # 確認 2 個容器運行
docker-compose logs postgres  # 確認 "database system is ready"

# 連接測試
docker exec -it smartadmin-postgres psql -U smartadmin -d smart_admin_v3
```

**默認配置**: 數據庫 `smart_admin_v3`, 用戶 `smartadmin`, 密碼 `SmartAdmin@2024`, 端口 `5432`

---

### 本地 PostgreSQL 安裝（可選）

```bash
# macOS
brew install postgresql@16 && brew services start postgresql@16

# 創建數據庫
psql postgres -c "CREATE DATABASE smart_admin_v3;"
psql postgres -c "CREATE USER smartadmin WITH PASSWORD 'SmartAdmin@2024';"
psql postgres -c "GRANT ALL ON DATABASE smart_admin_v3 TO smartadmin;"
```

---

## 四、應用配置

### 數據庫配置（sa-base/src/main/resources/dev/sa-base.yaml）

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/smart_admin_v3
    username: smartadmin
    password: SmartAdmin@2024
  data:
    redis:
      host: 127.0.0.1
      port: 6379

mybatis-plus:
  global-config:
    db-config:
      id-type: AUTO  # PostgreSQL SERIAL
```

### 驗證依賴

```bash
mvn dependency:tree | grep -E "(postgresql|vavr)"
# 確認: postgresql:42.7.5, vavr:0.10.4
```

---

## 五、構建和運行

```bash
# 編譯
cd smart-admin-api-java21-springboot3
mvn clean compile

# 打包（跳過測試）
mvn clean package -DskipTests

# 啟動
cd sa-admin && mvn spring-boot:run
# 或: java -jar sa-admin/target/sa-admin-3.28.3.jar
```

**驗證**: http://localhost:1024, Swagger: http://localhost:1024/swagger-ui.html

---

## 六、代碼質量檢查

```bash
# 架構測試
mvn test -Dtest=ArchitectureTest

# 完整驗證（測試 + 覆蓋率）
mvn verify
```

### 運行代碼風格檢查
```bash
mvn checkstyle:check
```

### 生成測試覆蓋率報告
```bash
mvn clean test jacoco:report
```

**報告位置**: `target/site/jacoco/index.html`

**覆蓋率要求**: ≥ 80%

---

## 七、前端設置（可選）

### 1. 導航到前端目錄
```bash
cd ../../smart-admin-web
```

### 2. 安裝依賴
```bash
npm install
```

### 3. 啟動開發服務器
```bash
npm run dev
```

**訪問**: http://localhost:5173

---


### Q1: 端口 1024 已被佔用
**解決方案**: 修改 `sa-admin/src/main/resources/dev/application.yaml`
```yaml
server:
  port: 8080  # 改為其他端口
```

### Q2: PostgreSQL 連接失敗
**檢查步驟**:
```bash
# 1. 確認 PostgreSQL 運行狀態
docker-compose ps postgres

# 2. 查看 PostgreSQL 日誌
docker-compose logs postgres

# 3. 測試連接
docker exec -it smartadmin-postgres psql -U smartadmin -d smart_admin_v3

# 4. 驗證配置文件
grep -A 5 "datasource:" sa-base/src/main/resources/dev/sa-base.yaml
```

### Q3: Redis 連接失敗
**解決方案**:
- Redis 是可選的，基礎開發可禁用
- 啟動 Redis:
  ```bash
  docker-compose up -d redis
  ```
- 或在配置中禁用 Redis

### Q4: 依賴下載緩慢
**解決方案**: 配置 Maven 鏡像（阿里雲）

**文件**: `~/.m2/settings.xml`
```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### Q5: ArchUnit 測試失敗
**常見原因**:
- Service 層使用了 `java.util.Optional`（應使用 `io.vavr.control.Option`）
- 字段注入（應使用構造函數注入）
- 引入了 MySQL 驅動（應使用 PostgreSQL）

**解決**: 查看測試報告，按規範修復代碼

### Q6: JSONB TypeHandler 不生效
**確認步驟**:
1. Entity 類添加 `autoResultMap = true`
   ```java
   @TableName(value = "t_order", autoResultMap = true)
   ```
2. 字段添加 TypeHandler
   ```java
   @TableField(typeHandler = JsonbTypeHandler.class)
   private Map<String, Object> metadata;
   ```

---

## 十、下一步

初始化成功後：

1. **閱讀開發規範**
   - `.agent/README.md` - 開發規範與導航總覽
   - `.agent/rules/08-vavr-fundamentals.md` - Vavr 函數式編程規範
   - `.agent/rules/05-postgresql-advanced.md` - PostgreSQL 數據庫規範
   - `.agent/rules/09-mybatis-plus-core.md` - MyBatis Plus 整合規範

2. **探索代碼庫**
   - `sa-base/module/support/` - 可複用支持模組
   - `sa-admin/module/business/` - 業務邏輯模組
   - `.agent/configs/ArchitectureTest.java` - 架構測試規則

3. **開發實踐**
   - 使用 Vavr Option 替代 null 檢查
   - 使用 Vavr Try 替代 try-catch
   - 使用 LambdaQueryWrapper 構建類型安全查詢
   - PostgreSQL JSONB 字段處理擴展數據
   - 運行 ArchUnit 測試確保架構一致性

4. **持續集成**
   - 配置 CI/CD 流程（參考 `.agent/workflows/java-ci-cd-pipeline.md`）
   - 集成 SonarQube 代碼質量分析
   - 設置 JaCoCo 測試覆蓋率門檻

---

## 十一、快速啟動腳本

### 一鍵啟動開發環境
```bash
#!/bin/bash
# 文件: start-dev.sh

echo "🚀 Starting SmartAdmin Development Environment..."

# 1. 啟動數據庫和緩存
cd .agent/configs
docker-compose up -d postgres redis
echo "✅ PostgreSQL and Redis started"

# 2. 等待 PostgreSQL 就緒
echo "⏳ Waiting for PostgreSQL..."
sleep 5

# 3. 編譯項目
cd ../../smart-admin-api-java21-springboot3
mvn clean compile -DskipTests
echo "✅ Project compiled"

# 4. 啟動應用
cd sa-admin
mvn spring-boot:run
```

**運行**:
```bash
chmod +x start-dev.sh
./start-dev.sh
```

---

## 十二、停止環境

```bash
# 停止應用（Ctrl+C）

# 停止 Docker 服務
cd .agent/configs
docker-compose down

# 停止並刪除數據卷（慎用）
docker-compose down -v
```

---

**初始化完成！** 🎉

參考文檔：
- 開發規範與導航: `.agent/README.md`
- Maven 依賴: `.agent/configs/maven-dependencies.md`
- Docker 環境: `.agent/configs/docker-compose.yml`
- 架構測試: `.agent/configs/ArchitectureTest.java`
