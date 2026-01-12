---
trigger: always_on
description: PostgreSQL 建表與索引規範
tags: [postgresql, database, schema, indexes]
positioning: ideal
last_updated: 2025-01-12
---

# PostgreSQL 建表與索引規範

**TL;DR**: PostgreSQL 建表必須包含審計字段（created_at/updated_at/deleted_at），使用 BIGSERIAL 主鍵、TIMESTAMPTZ 時間戳、JSONB 存儲非結構化數據。索引選擇 B-Tree（默認）、GIN（JSONB/數組）、BRIN（時序數據）、部分索引（活躍數據）。

**定位說明**: 本規範是 PostgreSQL 開發的基礎，適用於所有數據庫設計場景。高級特性（JSONB、CTE、窗口函數）見 `05-postgresql-advanced.md`，MyBatis 整合見 `05-postgresql-mybatis-integration.md`。

---

## 【強制】建表規範

### 1. 必備字段

每張表必須包含以下字段：

```sql
CREATE TABLE t_user (
    id BIGSERIAL PRIMARY KEY,  -- PostgreSQL 自增主鍵

    -- 業務字段 --
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,

    -- 審計字段 --
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,  -- 軟刪除（NULL 表示未刪除）
    created_by BIGINT,
    updated_by BIGINT,

    CONSTRAINT uk_user_email UNIQUE (email)
);

-- 自動更新 updated_at 觸發器
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_user_modtime
    BEFORE UPDATE ON t_user
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();
```

**字段說明**:
- `id`: 使用 `BIGSERIAL`（等價於 `BIGINT` + `AUTO_INCREMENT`）
- `created_at/updated_at`: 使用 `TIMESTAMPTZ`（帶時區時間戳）
- `deleted_at`: 軟刪除字段，NULL 表示未刪除

### 2. 命名規範

| 類型     | 規範              | 正例                      | 反例                  |
| -------- | ----------------- | ------------------------- | --------------------- |
| 表名     | 小寫+下劃線，單數 | `t_user`, `t_order_item`  | `Users`, `orderItems` |
| 字段名   | 小寫+下劃線       | `user_name`, `created_at` | `userName`            |
| 布爾字段 | is_xxx            | `is_deleted`, `is_active` | `deleted`, `active`   |
| 索引命名 | 見索引規範        | `idx_user_email`          | `index1`              |

### 3. 類型選擇（PostgreSQL 專用）

```sql
-- ✅ 正確 - 使用 PostgreSQL 優勢類型
CREATE TABLE t_product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    price NUMERIC(10,2) NOT NULL,          -- 精確數值（金額）
    status SMALLINT DEFAULT 0,             -- 狀態碼（2字節）
    description TEXT,                      -- 不限長度文本
    tags TEXT[],                           -- 數組類型
    metadata JSONB,                        -- JSON 存儲
    ip_address INET,                       -- IP 地址
    created_at TIMESTAMPTZ DEFAULT NOW(),  -- 帶時區時間戳
    valid_period DATERANGE                 -- 日期範圍
);
```

**類型映射建議**:
- 整數: `SMALLINT` (2字節) / `INTEGER` (4字節) / `BIGINT` (8字節)
- 精確數值: `NUMERIC(precision, scale)`
- 文本: `VARCHAR(n)` / `TEXT`（無長度限制）
- 時間: `TIMESTAMPTZ`（帶時區） / `DATE` / `TIME`
- 布爾: `BOOLEAN`
- JSON: `JSONB`（推薦，支持索引） / `JSON`
- 數組: `type[]` 如 `TEXT[]`, `INTEGER[]`

---

## 【強制】索引規範

### 1. 索引命名

- 主鍵: `pk_tablename` 或 默認（系統生成）
- 唯一索引: `uk_tablename_columnname`
- 普通索引: `idx_tablename_columnname`
- 外鍵索引: `fk_tablename_columnname`

### 2. PostgreSQL 索引類型

#### 2.1 B-Tree（默認，大多數場景）

```sql
-- 適用於: 等值查詢、範圍查詢、排序
CREATE INDEX idx_user_email ON t_user(email);
CREATE INDEX idx_order_created ON t_order(created_at);

-- 聯合索引
CREATE INDEX idx_order_user_status ON t_order(user_id, status, created_at);
```

#### 2.2 GIN（JSONB、數組、全文搜索）

```sql
-- JSONB 索引
CREATE INDEX idx_order_items ON t_order USING GIN(items);
CREATE INDEX idx_metadata ON t_order USING GIN(metadata jsonb_path_ops);

-- 數組索引
CREATE INDEX idx_article_tags ON t_article USING GIN(tags);

-- 全文搜索索引
CREATE INDEX idx_article_search ON t_article USING GIN(search_vector);
```

#### 2.3 GiST（地理位置、範圍類型）

```sql
-- 地理位置索引（需要 PostGIS 擴展）
CREATE INDEX idx_store_location ON t_store USING GIST(location);

-- 範圍類型索引
CREATE INDEX idx_event_period ON t_event USING GIST(valid_period);
```

#### 2.4 BRIN（大表且數據有序）

```sql
-- 適用於: 時間序列數據、日誌表
CREATE INDEX idx_log_created ON t_log USING BRIN(created_at);
```

#### 2.5 部分索引（僅索引活躍數據）

```sql
-- 僅索引未刪除的數據
CREATE INDEX idx_active_users ON t_user(email)
WHERE deleted_at IS NULL AND status = 'ACTIVE';

-- 僅索引最近一年的訂單
CREATE INDEX idx_recent_orders ON t_order(user_id, created_at)
WHERE created_at > NOW() - INTERVAL '1 year';
```

#### 2.6 表達式索引

```sql
-- 小寫郵箱索引
CREATE INDEX idx_user_lower_email ON t_user(LOWER(email));

-- JSON 字段提取索引
CREATE INDEX idx_order_user_id ON t_order((metadata->>'user_id'));
```

### 3. 聯合索引設計

```sql
-- 查詢: WHERE status=? AND type=? ORDER BY created_at
-- ✅ 正確順序：等值查詢在前，範圍/排序在後
CREATE INDEX idx_order_status_type_created
ON t_order(status, type, created_at);
```

**最左前綴原則**:

```sql
-- 索引: idx_a_b_c (a, b, c)

-- ✅ 可以使用索引
WHERE a = 1
WHERE a = 1 AND b = 2
WHERE a = 1 AND b = 2 AND c = 3

-- ❌ 無法使用索引
WHERE b = 2           -- 缺少 a
WHERE b = 2 AND c = 3 -- 缺少 a
WHERE a = 1 AND c = 3 -- 跳過 b，只能用 a
```

---

## 檢查清單

**建表規範**:
- ✅ 使用 BIGSERIAL 主鍵
- ✅ 時間字段使用 TIMESTAMPTZ
- ✅ 添加 created_at/updated_at 觸發器
- ✅ 表名、字段名小寫+下劃線
- ✅ 金額使用 NUMERIC 類型
- ✅ 軟刪除使用 deleted_at (TIMESTAMPTZ)

**索引優化**:
- ✅ 選擇正確的索引類型（B-Tree/GIN/BRIN）
- ✅ 部分索引減少索引大小
- ✅ 聯合索引遵循最左前綴原則
- ✅ JSONB/數組使用 GIN 索引
- ✅ 時序數據使用 BRIN 索引
- ✅ 活躍數據使用部分索引

---

## 相關規範

- **高級特性**: [rules/05-postgresql-advanced.md](./05-postgresql-advanced.md) - JSONB、數組、CTE、窗口函數
- **MyBatis 整合**: [rules/05-postgresql-mybatis-integration.md](./05-postgresql-mybatis-integration.md) - SQL 優化、遷移指南
- **MyBatis Plus**: [rules/09-mybatis-plus-core.md](./09-mybatis-plus-core.md) - 核心配置
- **Vavr 整合**: [rules/08-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md) - 函數式查詢
