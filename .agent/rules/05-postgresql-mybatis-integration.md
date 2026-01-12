---
trigger: always_on
description: PostgreSQL + MyBatis Plus：SQL 優化、MySQL 遷移、性能調優
tags: [postgresql, mybatis-plus, migration, performance, optimization]
positioning: migration
prerequisites: [rules/05-postgresql-basics.md, rules/09-mybatis-plus-core.md]
related_rules: [rules/08-vavr-mybatis-integration.md, rules/09-mybatis-plus-postgresql.md]
last_updated: 2025-01-12
---

# PostgreSQL + MyBatis Plus 整合

**TL;DR**: MySQL 遷移至 PostgreSQL 需注意類型映射（DATETIME→TIMESTAMPTZ、JSON→JSONB）、語法差異（ON DUPLICATE KEY→ON CONFLICT）、主鍵策略（AUTO_INCREMENT→BIGSERIAL）。SQL 優化禁止 SELECT *、左模糊查詢，分頁使用主鍵範圍而非 OFFSET。性能調優定期 VACUUM ANALYZE，連接池配置 `(core_count * 2) + spindle_count`。

**前置條件**:
- [05-postgresql-basics.md](./05-postgresql-basics.md) - PostgreSQL 基礎規範
- [09-mybatis-plus-core.md](./09-mybatis-plus-core.md) - MyBatis Plus 核心配置

---

## 【強制】SQL 語句優化

### 1. 禁止 SELECT *

```java
// ✅ 正確
@Select("SELECT id, name, email, status FROM t_user WHERE id = #{id}")
User selectById(Long id);

// ❌ 錯誤
@Select("SELECT * FROM t_user WHERE id = #{id}")
User selectById(Long id);
```

**原因**:
- 浪費網絡帶寬（傳輸不需要的字段）
- 無法利用覆蓋索引優化
- 表結構變更時影響應用穩定性

### 2. 分頁優化

```sql
-- ❌ 低效：OFFSET 掃描大量數據
SELECT * FROM t_order
LIMIT 20 OFFSET 100000;

-- ✅ 高效：使用主鍵範圍
SELECT * FROM t_order
WHERE id > 100000
ORDER BY id
LIMIT 20;

-- ✅ 高效：延遲關聯（複雜條件）
SELECT a.* FROM t_order a
INNER JOIN (
    SELECT id FROM t_order
    WHERE status = 'PAID'
    ORDER BY created_at DESC
    LIMIT 20 OFFSET 100000
) b ON a.id = b.id;
```

**性能對比**:
- `OFFSET 100000`: 掃描 100020 行，丟棄 100000 行
- `WHERE id > 100000`: 直接定位，掃描 20 行
- 延遲關聯: 子查詢僅選 id（索引覆蓋），外層查詢僅 20 次

### 3. 禁止左模糊查詢

```sql
-- ❌ 無法使用索引
WHERE email LIKE '%@gmail.com';

-- ✅ 可以使用索引
WHERE email LIKE 'john%';

-- ✅ 全文搜索（需要時）
WHERE search_vector @@ to_tsquery('gmail');
```

### 4. JOIN 優化

```sql
-- ✅ 小表驅動大表
SELECT o.id, o.amount, u.name
FROM t_order o
INNER JOIN t_user u ON o.user_id = u.id
WHERE u.status = 'ACTIVE';  -- 先過濾用戶

-- 確保 JOIN 字段有索引
CREATE INDEX idx_order_user_id ON t_order(user_id);
CREATE INDEX idx_user_id ON t_user(id);  -- 主鍵自動有索引
```

### 5. IN vs EXISTS

```sql
-- ✅ 小集合用 IN
SELECT * FROM t_order
WHERE user_id IN (1, 2, 3);

-- ✅ 子查詢用 EXISTS
SELECT * FROM t_order o
WHERE EXISTS (
    SELECT 1 FROM t_user u
    WHERE u.id = o.user_id
      AND u.vip = TRUE
);
```

---

## 【強制】數據遷移（MySQL → PostgreSQL）

### 1. 類型映射

| MySQL           | PostgreSQL     | 說明                    |
| --------------- | -------------- | ----------------------- |
| TINYINT         | SMALLINT       | 1字節 → 2字節           |
| INT             | INTEGER        | 相同                    |
| BIGINT          | BIGINT         | 相同                    |
| BIGINT UNSIGNED | BIGINT         | 去掉 UNSIGNED           |
| DATETIME        | TIMESTAMPTZ    | 推薦帶時區              |
| TIMESTAMP       | TIMESTAMPTZ    | 推薦帶時區              |
| VARCHAR(n)      | VARCHAR(n)     | 相同                    |
| TEXT            | TEXT           | 相同                    |
| DECIMAL(m,n)    | NUMERIC(m,n)   | 推薦 NUMERIC            |
| JSON            | JSONB          | 推薦 JSONB（支持索引）  |
| ENUM            | VARCHAR + CHECK| 或使用 ENUM 類型        |

### 2. SQL 語法差異

#### 2.1 UPSERT（插入或更新）

```sql
-- MySQL
INSERT INTO t_user VALUES (1, 'John')
ON DUPLICATE KEY UPDATE name='John';

-- PostgreSQL
INSERT INTO t_user VALUES (1, 'John')
ON CONFLICT (id) DO UPDATE SET name='John';
```

#### 2.2 自增主鍵

```sql
-- MySQL
id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY

-- PostgreSQL
id BIGSERIAL PRIMARY KEY
-- 或
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
```

#### 2.3 字符串拼接

```sql
-- MySQL
CONCAT(first_name, ' ', last_name)

-- PostgreSQL
first_name || ' ' || last_name
-- 或
CONCAT(first_name, ' ', last_name)  -- 也支持
```

#### 2.4 LIMIT 語法

```sql
-- MySQL 和 PostgreSQL 相同
SELECT * FROM t_user LIMIT 10 OFFSET 20;
```

#### 2.5 日期函數

```sql
-- MySQL
NOW()
CURDATE()
DATE_ADD(NOW(), INTERVAL 1 DAY)

-- PostgreSQL
NOW()
CURRENT_DATE
NOW() + INTERVAL '1 day'
```

### 3. MyBatis Plus 配置調整

```yaml
# application.yml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/smart_admin_v3?useSSL=false&serverTimezone=Asia/Shanghai
    username: smartadmin
    password: SmartAdmin@2024
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

mybatis-plus:
  configuration:
    # PostgreSQL 標識符使用雙引號
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      # PostgreSQL 主鍵策略
      id-type: AUTO
      # 邏輯刪除字段
      logic-delete-field: deletedAt
      logic-delete-value: 'NOW()'
      logic-not-delete-value: 'NULL'
```

### 4. 依賴更新

```xml
<!-- 移除 MySQL 驅動 -->
<!-- <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency> -->

<!-- 添加 PostgreSQL 驅動 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.5</version>
</dependency>

<!-- P6Spy 支持 PostgreSQL -->
<dependency>
    <groupId>p6spy</groupId>
    <artifactId>p6spy</artifactId>
    <version>3.9.1</version>
</dependency>
```

### 5. Entity 調整

```java
// MySQL Entity
@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;  // MySQL: DATETIME
}

// PostgreSQL Entity
@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;  // PostgreSQL: TIMESTAMPTZ
}
```

---

## 【推薦】性能優化

### 1. EXPLAIN ANALYZE

```sql
-- 查看執行計劃
EXPLAIN ANALYZE
SELECT * FROM t_user
WHERE email = 'test@example.com'
  AND status = 'ACTIVE';

-- 輸出示例：
-- Seq Scan on t_user  (cost=0.00..15.25 rows=1 width=100)
--   Filter: ((email = 'test@example.com') AND (status = 'ACTIVE'))
--   Rows Removed by Filter: 99
-- Planning Time: 0.123 ms
-- Execution Time: 0.456 ms
```

**關注指標**:
- `Seq Scan`: 全表掃描（慢）→ 需要添加索引
- `Index Scan`: 索引掃描（快）
- `Bitmap Index Scan`: 位圖索引掃描（中）
- `rows`: 估計行數 vs 實際行數
- `cost`: 估計成本
- `Execution Time`: 實際執行時間

### 2. VACUUM 和 ANALYZE

```sql
-- 清理死元組，更新統計信息
VACUUM ANALYZE t_user;

-- 僅更新統計信息
ANALYZE t_user;

-- 完全清理（鎖表，謹慎使用）
VACUUM FULL t_user;

-- 自動配置
ALTER TABLE t_user SET (
    autovacuum_enabled = true,
    autovacuum_vacuum_scale_factor = 0.1,
    autovacuum_analyze_scale_factor = 0.05
);
```

**何時執行**:
- 大量數據更新/刪除後
- 查詢性能下降時
- 定期維護（每週/每月）

### 3. 連接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20       # 最大連接數
      minimum-idle: 5             # 最小空閒連接
      connection-timeout: 30000   # 連接超時（ms）
      idle-timeout: 600000        # 空閒超時（10分鐘）
      max-lifetime: 1800000       # 最大生命週期（30分鐘）
      connection-test-query: SELECT 1
```

**連接數計算公式**:
```
connections = ((core_count * 2) + effective_spindle_count)
```

例如: 4核 CPU + 1個磁盤 = 9個連接

**注意事項**:
- PostgreSQL 默認最大連接數 100
- 過多連接消耗內存（每個連接 ~10MB）
- 建議使用連接池而非頻繁創建連接

### 4. 慢查詢日誌

```sql
-- postgresql.conf
log_min_duration_statement = 1000  -- 記錄超過1秒的查詢
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d '
log_statement = 'all'  -- 記錄所有語句（生產環境用 'none'）
```

**查詢慢查詢日誌**:
```sql
-- 使用 pg_stat_statements 擴展
CREATE EXTENSION pg_stat_statements;

-- 查詢最慢的 10 個查詢
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

### 5. 索引維護

```sql
-- 查找缺失索引（外鍵無索引）
SELECT
    tc.table_name,
    kcu.column_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND NOT EXISTS (
      SELECT 1 FROM pg_indexes
      WHERE tablename = tc.table_name
        AND indexdef LIKE '%' || kcu.column_name || '%'
  );

-- 查找未使用的索引
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexrelname NOT LIKE '%_pkey'
ORDER BY tablename, indexname;
```

---

## 檢查清單

**SQL 優化**:
- ✅ 禁止 SELECT *
- ✅ 分頁使用主鍵範圍或延遲關聯
- ✅ 禁止左模糊查詢
- ✅ JOIN 字段有索引
- ✅ 小集合用 IN，子查詢用 EXISTS

**MySQL 遷移**:
- ✅ 類型映射正確（DATETIME→TIMESTAMPTZ）
- ✅ 主鍵使用 BIGSERIAL
- ✅ UPSERT 使用 ON CONFLICT
- ✅ 依賴更新為 PostgreSQL 驅動
- ✅ Entity 時間字段使用 OffsetDateTime

**性能優化**:
- ✅ 使用 EXPLAIN ANALYZE 分析慢查詢
- ✅ 定期 VACUUM ANALYZE
- ✅ 連接池配置合理
- ✅ 開啟慢查詢日誌
- ✅ 刪除未使用的索引

---

## 相關規範

- **基礎規範**: [rules/05-postgresql-basics.md](./05-postgresql-basics.md) - 建表與索引
- **高級特性**: [rules/05-postgresql-advanced.md](./05-postgresql-advanced.md) - JSONB、CTE、窗口函數
- **MyBatis Plus 核心**: [rules/09-mybatis-plus-core.md](./09-mybatis-plus-core.md) - 基礎配置
- **MyBatis Plus PostgreSQL**: [rules/09-mybatis-plus-postgresql.md](./09-mybatis-plus-postgresql.md) - PostgreSQL 專用配置
- **Vavr 整合**: [rules/08-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md) - 函數式查詢
