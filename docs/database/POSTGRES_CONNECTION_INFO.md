# SmartAdmin-21 PostgreSQL 連線資訊

**更新日期**：2026-03-28
**環境**：本地開發環境（Docker）
**版本**：PostgreSQL 16 Alpine

---

## 📊 Docker 容器資訊

### 容器狀態

| 屬性 | 值 |
|------|-----|
| **容器名稱** | `sa21-postgres` |
| **映像檔** | `postgres:16-alpine` |
| **狀態** | ✅ Up 5 weeks (healthy) |
| **端口映射** | `0.0.0.0:5432->5432/tcp`<br/>`[::]:5432->5432/tcp` |
| **健康檢查** | ✅ Healthy |

### Docker 命令

```bash
# 檢查容器狀態
docker ps --filter "name=sa21-postgres"

# 進入容器
docker exec -it sa21-postgres bash

# 使用 psql 連線（容器內）
docker exec -it sa21-postgres psql -U postgres -d smart_admin

# 檢查資料庫列表
docker exec sa21-postgres psql -U postgres -c "\l"
```

---

## 🔐 連線憑證

### 主應用程式連線（application.yaml）

**來源檔案**：`smart-admin-api-java21-springboot3/smartadmin-app/src/main/resources/dev/application.yaml`

```yaml
# 數據源配置（PostgreSQL）
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smart_admin
    username: smartadmin
    password: SmartAdmin@2024
    driver-class-name: org.postgresql.Driver
```

**詳細資訊**：

| 屬性 | 值 |
|------|-----|
| **主機** | `localhost` |
| **端口** | `5432` |
| **資料庫名稱** | `smart_admin` |
| **使用者名稱** | `smartadmin` |
| **密碼** | `SmartAdmin@2024` |
| **驅動程式** | `org.postgresql.Driver` |

### Flyway 遷移連線（DDL 權限）

```yaml
# Flyway 資料庫版本管理
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: "0"
    baseline-description: "Existing SmartAdmin schema"
    validate-on-migrate: false
    out-of-order: true
    table: flyway_schema_history
    user: smartadmin
    password: SmartAdmin@2024
```

**說明**：
- ✅ Flyway 使用與應用程式相同的 `smartadmin` 使用者（具備 DDL 權限）
- ✅ `baseline-on-migrate: true` - 適用於現有資料庫
- ⚠️ `validate-on-migrate: false` - 暫時禁用（修復 V24 RLS policy 冪等性問題）

### LiteFlow 規則存儲連線

```yaml
# LiteFlow SQL 規則存儲配置
liteflow:
  rule-source: sql
  rule-source-ext-data-map:
    url: jdbc:postgresql://localhost:5432/smart_admin
    driverClassName: org.postgresql.Driver
    username: postgres
    password: postgres
    applicationName: smartadmin
```

**詳細資訊**：

| 屬性 | 值 |
|------|-----|
| **主機** | `localhost` |
| **端口** | `5432` |
| **資料庫名稱** | `smart_admin` |
| **使用者名稱** | `postgres` ⚠️ |
| **密碼** | `postgres` ⚠️ |
| **應用名稱** | `smartadmin` |

**⚠️ 注意**：LiteFlow 使用 `postgres` 超級使用者（用於存取 LiteFlow 規則表）

---

## 🔧 HikariCP 連接池配置

```yaml
# HikariCP 連接池配置
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000       # 30 秒
      idle-timeout: 600000            # 10 分鐘
      max-lifetime: 1800000           # 30 分鐘
```

**參數說明**：

| 參數 | 值 | 說明 |
|------|-----|------|
| `minimum-idle` | 5 | 最小空閒連接數 |
| `maximum-pool-size` | 20 | 最大連接池大小 |
| `connection-timeout` | 30000ms (30秒) | 連接超時時間 |
| `idle-timeout` | 600000ms (10分鐘) | 空閒連接超時 |
| `max-lifetime` | 1800000ms (30分鐘) | 連接最大生命週期 |

**效能建議**：
- ✅ **開發環境**：當前配置適合（pool-size: 20）
- 📈 **生產環境**：建議 `maximum-pool-size: 50-100`（根據負載調整）

---

## 🗄️ 資料庫結構資訊

### 核心表（SmartAdmin）

```bash
# 檢查所有表
docker exec sa21-postgres psql -U smartadmin -d smart_admin -c "\dt"

# 檢查 Flyway 遷移歷史
docker exec sa21-postgres psql -U smartadmin -d smart_admin -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;"

# 檢查 LiteFlow 規則表
docker exec sa21-postgres psql -U smartadmin -d smart_admin -c "SELECT * FROM t_liteflow_chain;"
```

### LiteFlow 規則表映射

| 表名 | 用途 | 欄位映射 |
|------|------|---------|
| `t_liteflow_chain` | Chain 規則存儲 | `chain_code` → Chain Name<br/>`chain_data` → EL Data<br/>`status` → Enable Field |
| `t_liteflow_script` | Script 規則存儲 | `script_code` → Script ID<br/>`script_name` → Script Name<br/>`script_data` → Script Data<br/>`script_type` → Script Language |

---

## 🔌 JDBC URL 格式

### 標準 JDBC URL

```
jdbc:postgresql://localhost:5432/smart_admin
```

### 帶參數的 JDBC URL（生產環境建議）

```
jdbc:postgresql://localhost:5432/smart_admin?applicationName=smartadmin&ssl=false&stringtype=unspecified
```

**參數說明**：
- `applicationName`: 應用識別（用於 PostgreSQL 日誌追蹤）
- `ssl=false`: 本地開發環境禁用 SSL
- `stringtype=unspecified`: 解決 MyBatis JSONB 類型轉換問題

---

## 🛠️ 常用操作命令

### 資料庫連線測試

```bash
# 測試 smartadmin 使用者連線
docker exec sa21-postgres psql -U smartadmin -d smart_admin -c "SELECT version();"

# 測試 postgres 超級使用者連線
docker exec sa21-postgres psql -U postgres -d smart_admin -c "SELECT current_user, current_database();"
```

### 資料庫狀態檢查

```bash
# 檢查活動連接數
docker exec sa21-postgres psql -U postgres -d smart_admin -c "SELECT count(*) FROM pg_stat_activity WHERE datname = 'smart_admin';"

# 檢查資料庫大小
docker exec sa21-postgres psql -U postgres -d smart_admin -c "SELECT pg_size_pretty(pg_database_size('smart_admin'));"

# 檢查慢查詢（>1秒）
docker exec sa21-postgres psql -U postgres -d smart_admin -c "SELECT query, calls, total_time, mean_time FROM pg_stat_statements WHERE mean_time > 1000 ORDER BY mean_time DESC LIMIT 10;"
```

### 備份與還原

```bash
# 備份資料庫
docker exec sa21-postgres pg_dump -U smartadmin -d smart_admin -F c -f /tmp/smart_admin_backup.dump
docker cp sa21-postgres:/tmp/smart_admin_backup.dump ./backups/

# 還原資料庫
docker cp ./backups/smart_admin_backup.dump sa21-postgres:/tmp/
docker exec sa21-postgres pg_restore -U smartadmin -d smart_admin -c /tmp/smart_admin_backup.dump
```

---

## 🔒 安全配置

### Row-Level Security (RLS)

```yaml
# 多租戶配置
tenant:
  enabled: false  # ⚠️ 當前禁用（開發環境）
  default-timezone: UTC
  base-domain: example.com
  rls:
    enabled: false  # ⚠️ 啟用後需切換到 smartadmin_app 資料來源
```

**說明**：
- 🔒 RLS（Row-Level Security）用於多租戶資料隔離
- ⚠️ 當前開發環境禁用 RLS
- 📋 啟用 RLS 需配合 `smartadmin_app` 資料來源（Flyway 使用 `postgres` 超級使用者）

### 密碼雜湊（Argon2id）

```yaml
# Argon2id 密碼雜湊配置（iGaming 合規：OWASP 2024）
smart:
  security:
    argon2:
      salt-length: 16
      hash-length: 32
      parallelism: 4
      memory: 65536       # 64 MB
      iterations: 3
```

### 欄位加密（AES-256-GCM）

```yaml
# Field encryption configuration (AES-256-GCM for TOTP secrets)
smart:
  field-encrypt:
    enabled: true
    # Data Encryption Key (256-bit AES, Base64-encoded)
    dek: OQcGYSHEPPzSpI7aQvffTZPRoVSa6NA2YfdflWZewr4=
    # Blind Index Key (HMAC-SHA256, Base64-encoded)
    blind-index-key: rBLUDiID+A8fMOM+s/FnkQdLxpeTkuJ5gA8Mm2rGJQ4=
```

**⚠️ 重要提醒**：
- 🔑 DEK 和 Blind Index Key 僅供開發環境使用
- 🚫 **生產環境絕對不可使用相同密鑰**
- 🔄 定期輪換加密密鑰（建議每季度）
- 🔒 密鑰應存儲在外部密鑰管理服務（如 AWS KMS、HashiCorp Vault）

---

## 📊 監控與日誌

### MyBatis 日誌配置

```yaml
# Logging 配置（診斷 TypeHandler 註冊問題）
logging:
  level:
    root: INFO
    org.apache.ibatis: DEBUG
    org.mybatis.spring: DEBUG
    net.lab1024.sa.common.mybatis.typehandler: DEBUG
```

### Tomcat Access Log

```yaml
# Tomcat access log 配置
server:
  tomcat:
    basedir: ${project.log-directory}/tomcat-logs
    accesslog:
      enabled: true
      max-days: 7
      pattern: "%t %{X-Forwarded-For}i %a %r %s (%D ms) %I (%B byte)"
```

**日誌位置**：`${user.home}/logs/smart_admin_v4/smartadmin-app/dev/tomcat-logs`

---

## 🔗 相關工具與擴展

### pgAdmin 4 連線配置

如果使用 pgAdmin 4 GUI 工具：

| 欄位 | 值 |
|------|-----|
| **Name** | SmartAdmin-21 Dev |
| **Host** | localhost |
| **Port** | 5432 |
| **Maintenance database** | smart_admin |
| **Username** | smartadmin |
| **Password** | SmartAdmin@2024 |

### IntelliJ IDEA Database Tool

1. **Database Tool** → **+** → **Data Source** → **PostgreSQL**
2. 填入連線資訊：
   - Host: `localhost`
   - Port: `5432`
   - Database: `smart_admin`
   - User: `smartadmin`
   - Password: `SmartAdmin@2024`
3. **Test Connection** → 成功後 **Apply**

### DBeaver 連線配置

1. **Database** → **New Database Connection** → **PostgreSQL**
2. 填入連線資訊（同上）
3. **Driver properties** → 添加：
   - `applicationName`: `smartadmin-dbeaver`

---

## 🚨 常見問題排查

### 問題 1：連線被拒絕（Connection refused）

```bash
# 檢查容器是否運行
docker ps --filter "name=sa21-postgres"

# 如果未運行，啟動容器
docker start sa21-postgres

# 檢查端口是否被佔用
lsof -i :5432
```

### 問題 2：認證失敗（Authentication failed）

```bash
# 檢查 smartadmin 使用者是否存在
docker exec sa21-postgres psql -U postgres -c "\du"

# 重設 smartadmin 密碼
docker exec sa21-postgres psql -U postgres -c "ALTER USER smartadmin WITH PASSWORD 'SmartAdmin@2024';"
```

### 問題 3：資料庫不存在（Database does not exist）

```bash
# 檢查資料庫列表
docker exec sa21-postgres psql -U postgres -c "\l"

# 如果不存在，建立資料庫
docker exec sa21-postgres psql -U postgres -c "CREATE DATABASE smart_admin OWNER smartadmin;"
```

### 問題 4：連接池耗盡（Connection pool exhausted）

```bash
# 檢查活動連接數
docker exec sa21-postgres psql -U postgres -d smart_admin -c "
SELECT count(*), state
FROM pg_stat_activity
WHERE datname = 'smart_admin'
GROUP BY state;
"

# 終止空閒連接
docker exec sa21-postgres psql -U postgres -d smart_admin -c "
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'smart_admin'
  AND state = 'idle'
  AND state_change < current_timestamp - INTERVAL '10 minutes';
"
```

---

## 📚 參考文件

### SmartAdmin 相關

- **Application Config**: [application.yaml](../../smart-admin-api-java21-springboot3/smartadmin-app/src/main/resources/dev/application.yaml)
- **Flyway Migrations**: [db/migration/](../../smart-admin-api-java21-springboot3/smartadmin-app/src/main/resources/db/migration/)
- **MyBatis Mappers**: [mapper/](../../smart-admin-api-java21-springboot3/smartadmin-modules/)

### PostgreSQL 文檔

- **PostgreSQL 16 Documentation**: https://www.postgresql.org/docs/16/
- **HikariCP Configuration**: https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby
- **Flyway Documentation**: https://flywaydb.org/documentation/

### LiteFlow 文檔

- **LiteFlow SQL 規則存儲**: https://liteflow.cc/pages/v2.12.X/08.%E8%A7%84%E5%88%99%E6%96%87%E4%BB%B6/04.html

---

## ✅ 快速驗證清單

執行以下命令確認連線正常：

```bash
# 1. 檢查容器狀態
docker ps --filter "name=sa21-postgres"
# ✅ 預期：容器正在運行且健康

# 2. 測試 smartadmin 使用者連線
docker exec sa21-postgres psql -U smartadmin -d smart_admin -c "SELECT current_user, current_database(), version();"
# ✅ 預期：顯示當前使用者、資料庫名稱、PostgreSQL 版本

# 3. 檢查連接池配置
cd smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:bootRun
# ✅ 預期：應用啟動成功，無連接錯誤

# 4. 測試 API 連線
curl http://localhost:1024/actuator/health
# ✅ 預期：{"status":"UP"}
```

---

**文檔版本**: v1.0.0
**最後更新**: 2026-03-28
**維護者**: SmartAdmin Database Team
**環境**: 本地開發環境（Docker）
