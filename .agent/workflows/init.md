---
trigger: on_demand
description: SmartAdmin 開發環境初始化設定
tags: [setup, environment, java-21, postgresql, vavr, docker]
required_rules:
  - rules/05-postgresql-basics.md
  - rules/08-vavr-fundamentals.md
  - rules/09-mybatis-plus-core.md
last_updated: 2025-01-12
---

# SmartAdmin 開發環境初始化

本指南將引導您設置 SmartAdmin (Java 21 + Spring Boot 3.5.4 + PostgreSQL) 開發環境。

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

### 方式一：使用 Docker Compose（推薦）

#### 1. 啟動 PostgreSQL + Redis
```bash
cd .agent/configs
docker-compose up -d postgres redis
```

#### 2. 驗證服務狀態
```bash
docker-compose ps
```

**期望輸出**:
```
smartadmin-postgres   postgres:16-alpine   Up   0.0.0.0:5432->5432/tcp
smartadmin-redis      redis:7-alpine       Up   0.0.0.0:6379->6379/tcp
```

#### 3. 查看 PostgreSQL 日誌
```bash
docker-compose logs postgres
```

**確認**: 看到 "database system is ready to accept connections"

#### 4. 連接數據庫
```bash
docker exec -it smartadmin-postgres psql -U smartadmin -d smart_admin_v3
```

**默認配置**:
- 數據庫: `smart_admin_v3`
- 用戶名: `smartadmin`
- 密碼: `SmartAdmin@2024`
- 端口: `5432`

#### 5. 導入 SQL 腳本
```bash
# 如果有現有 MySQL 數據需要遷移，請參考遷移指南
# 如果是新項目，PostgreSQL 會自動執行 .agent/configs/init-scripts/01-init.sql

# 手動導入（如需要）
docker exec -i smartadmin-postgres psql -U smartadmin -d smart_admin_v3 < path/to/schema.sql
```

---

### 方式二：本地 PostgreSQL 安裝

#### macOS
```bash
brew install postgresql@16
brew services start postgresql@16
```

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install postgresql-16
sudo systemctl start postgresql
```

#### 創建數據庫
```bash
sudo -u postgres psql
```
```sql
CREATE DATABASE smart_admin_v3;
CREATE USER smartadmin WITH ENCRYPTED PASSWORD 'SmartAdmin@2024';
GRANT ALL PRIVILEGES ON DATABASE smart_admin_v3 TO smartadmin;
```

---

## 四、應用配置

### 1. 更新數據庫配置

**文件**: `sa-base/src/main/resources/dev/sa-base.yaml`

```yaml
spring:
  datasource:
    # PostgreSQL 驅動
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/smart_admin_v3
    username: smartadmin
    password: SmartAdmin@2024

    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: SmartAdmin@2024  # 如果使用 Docker Compose

mybatis-plus:
  global-config:
    db-config:
      id-type: AUTO  # PostgreSQL SERIAL/BIGSERIAL
      logic-delete-field: deleted_at
      logic-delete-value: NOW()
      logic-not-delete-value: 'NULL'

  configuration:
    map-underscore-to-camel-case: true
```

### 2. 驗證依賴配置

**檢查 PostgreSQL 驅動**:
```bash
mvn dependency:tree | grep postgresql
```

**期望輸出**:
```
org.postgresql:postgresql:jar:42.7.5:runtime
```

**檢查 Vavr 依賴**:
```bash
mvn dependency:tree | grep vavr
```

**期望輸出**:
```
io.vavr:vavr:jar:0.10.4:compile
io.vavr:vavr-jackson:jar:0.10.3:compile
```

---

## 五、構建和運行

### 1. 清理並編譯項目
```bash
cd smart-admin-api-java21-springboot3
mvn clean compile
```

### 2. 運行測試（可選）
```bash
mvn test
```

### 3. 打包應用
```bash
# 跳過測試
mvn clean package -DskipTests

# 包含測試和質量檢查
mvn clean verify
```

### 4. 啟動應用
```bash
cd sa-admin
mvn spring-boot:run
```

**或直接運行 JAR**:
```bash
java -jar sa-admin/target/sa-admin-3.28.3.jar
```

### 5. 驗證應用
- **應用 URL**: http://localhost:1024
- **Swagger API 文檔**: http://localhost:1024/swagger-ui.html
- **默認端口**: 1024

**確認日誌**:
```
Application started successfully on port 1024
Database: PostgreSQL 16
```

---

## 六、代碼質量檢查

### 運行架構測試（ArchUnit）
```bash
mvn test -Dtest=ArchitectureTest
```

**驗證規則**:
- ✅ 分層架構約束（Controller → Service → Manager → Domain）
- ✅ 命名規範（Controller/Service 後綴）
- ✅ 禁止字段注入
- ✅ Vavr 使用規範（Service 層推薦 Option 代替 Optional）
- ✅ PostgreSQL 約束（禁止 MySQL 驅動）

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

## 八、開發工具推薦

### 1. IntelliJ IDEA 插件
- **Lombok**: 支持 @Data、@Builder 等注解
- **MyBatisX**: MyBatis Mapper 跳轉
- **SonarLint**: 實時代碼質量檢測
- **CheckStyle-IDEA**: Checkstyle 集成

### 2. 數據庫工具
- **pgAdmin 4**: PostgreSQL 圖形化管理工具
  - 如使用 Docker Compose，啟動方式:
    ```bash
    docker-compose --profile tools up -d pgadmin
    ```
  - 訪問: http://localhost:5050
  - 賬號: admin@smartadmin.local / SmartAdmin@2024

- **DBeaver**: 通用數據庫工具
- **DataGrip**: JetBrains 數據庫 IDE

### 3. API 測試工具
- **Postman**: API 測試
- **Swagger UI**: 內置 API 文檔（http://localhost:1024/swagger-ui.html）

---

## 九、常見問題

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
   - `.agent/dev-standards.md` - 開發標準總覽
   - `.agent/rules/08-reactive-programming.md` - Vavr 函數式編程規範
   - `.agent/rules/05-rdb-specifications.md` - PostgreSQL 數據庫規範
   - `.agent/rules/09-mybatis-plus.md` - MyBatis Plus 整合規範

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
- 技術棧說明: `.agent/dev-standards.md`
- Maven 依賴: `.agent/configs/maven-dependencies.md`
- Docker 環境: `.agent/configs/docker-compose.yml`
- 架構測試: `.agent/configs/ArchitectureTest.java`
