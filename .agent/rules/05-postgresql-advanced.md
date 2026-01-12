---
trigger: always_on
description: PostgreSQL 高級特性：JSONB、數組、CTE、窗口函數
tags: [postgresql, jsonb, array, cte, window-function]
positioning: ideal
prerequisites: [rules/05-postgresql-basics.md]
last_updated: 2025-01-12
---

# PostgreSQL 高級特性

**TL;DR**: 掌握 PostgreSQL 特有功能：JSONB 存儲動態數據（支持 GIN 索引查詢）、數組類型（標籤系統）、CTE 提升複雜查詢可讀性、窗口函數實現排名/累計統計、全文搜索（中文需 jieba 插件）。

**前置條件**: 閱讀 [05-postgresql-basics.md](./05-postgresql-basics.md) 了解基礎建表與索引規範。

---

## 【強制】JSONB 字段

### 1. 使用場景

- 靈活的擴展字段（避免頻繁加字段）
- 動態表單數據
- 審計日誌（記錄變更詳情）
- 配置存儲

### 2. 建表示例

```sql
CREATE TABLE t_order (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,

    -- JSONB 字段
    items JSONB NOT NULL,       -- 訂單項詳情
    metadata JSONB,             -- 擴展字段

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- JSONB 索引 (GIN - Generalized Inverted Index)
CREATE INDEX idx_order_items ON t_order USING GIN(items);
CREATE INDEX idx_order_metadata ON t_order USING GIN(metadata jsonb_path_ops);
```

### 3. 查詢操作

```sql
-- 查詢包含特定商品的訂單
SELECT * FROM t_order
WHERE items @> '[{"product_id": 123}]'::jsonb;

-- 提取 JSONB 字段
SELECT
    id,
    items->>'total_amount' as total,
    metadata->'shipping_address'->>'city' as city
FROM t_order;

-- 更新 JSONB 字段
UPDATE t_order
SET metadata = jsonb_set(
    metadata,
    '{shipping_address, city}',
    '"杭州"'
)
WHERE id = 1;

-- 檢查鍵是否存在
SELECT * FROM t_order
WHERE metadata ? 'vip_discount';
```

### 4. MyBatis Plus 整合

```java
// Entity 定義
@Data
@TableName("t_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<OrderItem> items;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
}

// TypeHandler 實現（序列化 JSONB）
public class JacksonTypeHandler extends BaseTypeHandler<Object> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Class<?> type;

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(MAPPER.writeValueAsString(parameter));
        ps.setObject(i, jsonObject);
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return fromJson(rs.getString(columnName));
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return fromJson(rs.getString(columnIndex));
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return fromJson(cs.getString(columnIndex));
    }

    private Object fromJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) return null;
        return MAPPER.readValue(json, type);
    }
}

// Mapper 查詢
default List<Order> findByProductId(Long productId) {
    return selectList(new LambdaQueryWrapper<Order>()
        .apply("items @> '[{\"product_id\": {0}}]'::jsonb", productId));
}
```

---

## 【強制】數組類型

### 1. 使用場景

- 標籤系統（tags）
- 權限列表（roles）
- 多選字段

### 2. 建表與查詢

```sql
CREATE TABLE t_article (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,

    -- 數組字段
    tags TEXT[],            -- 標籤數組
    category_ids INTEGER[], -- 分類 ID 數組

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 數組索引 (GIN)
CREATE INDEX idx_article_tags ON t_article USING GIN(tags);

-- 插入數組
INSERT INTO t_article (title, content, tags)
VALUES ('示例文章', '內容', ARRAY['Java', 'PostgreSQL', 'Spring']);

-- 查詢包含特定標籤（任一匹配）
SELECT * FROM t_article
WHERE tags && ARRAY['Java', 'Python'];

-- 查詢包含所有標籤
SELECT * FROM t_article
WHERE tags @> ARRAY['Java', 'Spring'];

-- 數組追加
UPDATE t_article
SET tags = array_append(tags, 'NewTag')
WHERE id = 1;
```

### 3. MyBatis Plus 整合

```java
// Entity 定義
@Data
@TableName("t_article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;

    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] tags;
}

// TypeHandler 實現
public class StringArrayTypeHandler extends BaseTypeHandler<String[]> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String[] parameter, JdbcType jdbcType) throws SQLException {
        ps.setArray(i, ps.getConnection().createArrayOf("TEXT", parameter));
    }

    @Override
    public String[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array array = rs.getArray(columnName);
        return array != null ? (String[]) array.getArray() : null;
    }

    @Override
    public String[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array array = rs.getArray(columnIndex);
        return array != null ? (String[]) array.getArray() : null;
    }

    @Override
    public String[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array array = cs.getArray(columnIndex);
        return array != null ? (String[]) array.getArray() : null;
    }
}

// Mapper 查詢
default List<Article> findByTag(String tag) {
    return selectList(new LambdaQueryWrapper<Article>()
        .apply("tags && ARRAY[{0}]::TEXT[]", tag));
}
```

---

## 【強制】CTE (公用表表達式)

### 1. 基本 CTE

```sql
-- 提升可讀性和可維護性
WITH active_users AS (
    SELECT id, name, department_id
    FROM t_user
    WHERE status = 'ACTIVE' AND deleted_at IS NULL
),
department_stats AS (
    SELECT
        department_id,
        COUNT(*) as user_count,
        AVG(age) as avg_age
    FROM active_users
    GROUP BY department_id
)
SELECT
    d.name,
    COALESCE(ds.user_count, 0) as user_count,
    COALESCE(ds.avg_age, 0) as avg_age
FROM t_department d
LEFT JOIN department_stats ds ON d.id = ds.department_id;
```

### 2. 遞歸 CTE（組織樹）

```sql
-- 遞歸查詢組織樹
WITH RECURSIVE org_tree AS (
    -- 初始查詢（根節點）
    SELECT
        id,
        name,
        parent_id,
        1 as level,
        ARRAY[id] as path
    FROM t_department
    WHERE parent_id IS NULL

    UNION ALL

    -- 遞歸部分
    SELECT
        d.id,
        d.name,
        d.parent_id,
        ot.level + 1,
        ot.path || d.id
    FROM t_department d
    JOIN org_tree ot ON d.parent_id = ot.id
    WHERE NOT d.id = ANY(ot.path)  -- 防止循環
)
SELECT
    id,
    REPEAT('  ', level - 1) || name as name,
    level,
    path
FROM org_tree
ORDER BY path;
```

### 3. MyBatis 中使用

```xml
<select id="getDepartmentStats" resultType="DepartmentStatVO">
    WITH active_users AS (
        SELECT id, department_id FROM t_user WHERE deleted_at IS NULL
    )
    SELECT d.id, d.name, COUNT(u.id) as user_count
    FROM t_department d
    LEFT JOIN active_users u ON d.id = u.department_id
    GROUP BY d.id, d.name
</select>
```

---

## 【強制】窗口函數

### 1. 排名函數

```sql
-- 每個部門的薪資排名
SELECT
    name,
    department_id,
    salary,
    RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) as rank,
    DENSE_RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) as dense_rank,
    ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC) as row_num
FROM t_employee;

-- 差異：
-- RANK: 1, 2, 2, 4（有並列時跳號）
-- DENSE_RANK: 1, 2, 2, 3（有並列不跳號）
-- ROW_NUMBER: 1, 2, 3, 4（唯一序號）
```

### 2. 聚合函數

```sql
-- 部門平均薪資 vs 個人薪資
SELECT
    name,
    department_id,
    salary,
    AVG(salary) OVER (PARTITION BY department_id) as dept_avg_salary,
    salary - AVG(salary) OVER (PARTITION BY department_id) as diff_from_avg
FROM t_employee;

-- 累計統計
SELECT
    order_date,
    amount,
    SUM(amount) OVER (
        ORDER BY order_date
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) as running_total
FROM t_order
ORDER BY order_date;
```

### 3. LAG/LEAD 函數

```sql
-- 對比上一次/下一次的值
SELECT
    order_date,
    amount,
    LAG(amount, 1) OVER (ORDER BY order_date) as prev_amount,
    LEAD(amount, 1) OVER (ORDER BY order_date) as next_amount,
    amount - LAG(amount, 1) OVER (ORDER BY order_date) as change
FROM t_order
ORDER BY order_date;
```

---

## 【推薦】全文搜索

### 1. 基本使用

```sql
-- 添加全文搜索列
ALTER TABLE t_article
ADD COLUMN search_vector TSVECTOR;

-- 更新搜索向量（中文使用 zhparser 或 jieba）
UPDATE t_article
SET search_vector = to_tsvector('english', title || ' ' || content);

-- 創建 GIN 索引
CREATE INDEX idx_article_search ON t_article USING GIN(search_vector);

-- 全文搜索查詢
SELECT
    id,
    title,
    ts_rank(search_vector, query) AS rank
FROM t_article,
     to_tsquery('english', 'PostgreSQL & performance') AS query
WHERE search_vector @@ query
ORDER BY rank DESC;
```

### 2. 自動更新觸發器

```sql
CREATE OR REPLACE FUNCTION article_search_trigger()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('english',
        COALESCE(NEW.title, '') || ' ' ||
        COALESCE(NEW.content, '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tsvector_update
BEFORE INSERT OR UPDATE ON t_article
FOR EACH ROW
EXECUTE FUNCTION article_search_trigger();
```

---

## 檢查清單

**JSONB 字段**:
- ✅ 動態字段使用 JSONB 而非多列
- ✅ 添加 GIN 索引支持查詢
- ✅ 使用 JacksonTypeHandler 序列化
- ✅ 使用 @> 操作符包含查詢

**數組類型**:
- ✅ 標籤系統使用 TEXT[]
- ✅ 添加 GIN 索引支持查詢
- ✅ 使用 StringArrayTypeHandler
- ✅ 使用 && 或 @> 操作符查詢

**CTE 與窗口函數**:
- ✅ 複雜查詢使用 CTE 提升可讀性
- ✅ 遞歸查詢使用 RECURSIVE CTE
- ✅ 統計分析使用窗口函數
- ✅ 排名使用 RANK/DENSE_RANK/ROW_NUMBER

---

## 相關規範

- **基礎規範**: [rules/05-postgresql-basics.md](./05-postgresql-basics.md) - 建表與索引
- **MyBatis 整合**: [rules/05-postgresql-mybatis-integration.md](./05-postgresql-mybatis-integration.md) - SQL 優化、遷移
- **MyBatis Plus**: [rules/09-mybatis-plus-postgresql.md](./09-mybatis-plus-postgresql.md) - PostgreSQL 配置
