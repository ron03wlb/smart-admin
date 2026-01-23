---
trigger: always_on
description: PostgreSQL Advanced Features - JSONB, Array, CTE, Window Function
tags: [postgresql, jsonb, array, cte, window-function]
positioning: ideal
prerequisites:
  - rules/05-postgresql-basics.md
last_updated: 2025-01-12
---

# PostgreSQL Advanced Features

**TL;DR**: Master PostgreSQL-specific features: JSONB for dynamic data storage (supports GIN index queries), Array Type (tag system), CTE improves complex query readability, Window Functions for ranking/cumulative statistics, Full-Text Search (Chinese requires jieba plugin).

**Prerequisites**: Read [05-postgresql-basics.md](./05-postgresql-basics.md) for basic table creation and index rules.

---

## 【Mandatory】JSONB Fields

### 1. Use Cases

- Flexible extension fields (avoid frequent column addition)
- Dynamic form data
- Audit Log (record change details)
- Configuration storage

### 2. Table Creation Example

```sql
CREATE TABLE t_order (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,

    -- JSONB fields
    items JSONB NOT NULL,       -- Order item details
    metadata JSONB,             -- Extension fields

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- JSONB Index (GIN - Generalized Inverted Index)
CREATE INDEX idx_order_items ON t_order USING GIN(items);
CREATE INDEX idx_order_metadata ON t_order USING GIN(metadata jsonb_path_ops);
```

### 3. Query Operations

```sql
-- Query orders containing specific product
SELECT * FROM t_order
WHERE items @> '[{"product_id": 123}]'::jsonb;

-- Extract JSONB fields
SELECT
    id,
    items->>'total_amount' as total,
    metadata->'shipping_address'->>'city' as city
FROM t_order;

-- Update JSONB fields
UPDATE t_order
SET metadata = jsonb_set(
    metadata,
    '{shipping_address, city}',
    '"Hangzhou"'
)
WHERE id = 1;

-- Check if key exists
SELECT * FROM t_order
WHERE metadata ? 'vip_discount';
```

### 4. MyBatis Plus Integration

```java
// Entity definition
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

// TypeHandler implementation (serialize JSONB)
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

// Mapper query
default List<Order> findByProductId(Long productId) {
    return selectList(new LambdaQueryWrapper<Order>()
        .apply("items @> '[{\"product_id\": {0}}]'::jsonb", productId));
}
```

---

## 【Mandatory】Array Type

### 1. Use Cases

- Tag system (tags)
- Permission list (roles)
- Multi-select fields

### 2. Table Creation and Query

```sql
CREATE TABLE t_article (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,

    -- Array fields
    tags TEXT[],            -- Tag array
    category_ids INTEGER[], -- Category ID array

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Array index (GIN)
CREATE INDEX idx_article_tags ON t_article USING GIN(tags);

-- Insert array
INSERT INTO t_article (title, content, tags)
VALUES ('Sample Article', 'Content', ARRAY['Java', 'PostgreSQL', 'Spring']);

-- Query containing specific tags (any match)
SELECT * FROM t_article
WHERE tags && ARRAY['Java', 'Python'];

-- Query containing all tags
SELECT * FROM t_article
WHERE tags @> ARRAY['Java', 'Spring'];

-- Append to array
UPDATE t_article
SET tags = array_append(tags, 'NewTag')
WHERE id = 1;
```

### 3. MyBatis Plus Integration

```java
// Entity definition
@Data
@TableName("t_article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;

    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] tags;
}

// TypeHandler implementation
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

// Mapper query
default List<Article> findByTag(String tag) {
    return selectList(new LambdaQueryWrapper<Article>()
        .apply("tags && ARRAY[{0}]::TEXT[]", tag));
}
```

---

## 【Mandatory】CTE (Common Table Expression)

### 1. Basic CTE

```sql
-- Improve readability and maintainability
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

### 2. Recursive CTE (Organization Tree)

```sql
-- Recursive query for organization tree
WITH RECURSIVE org_tree AS (
    -- Initial query (root node)
    SELECT
        id,
        name,
        parent_id,
        1 as level,
        ARRAY[id] as path
    FROM t_department
    WHERE parent_id IS NULL

    UNION ALL

    -- Recursive part
    SELECT
        d.id,
        d.name,
        d.parent_id,
        ot.level + 1,
        ot.path || d.id
    FROM t_department d
    JOIN org_tree ot ON d.parent_id = ot.id
    WHERE NOT d.id = ANY(ot.path)  -- Prevent cycles
)
SELECT
    id,
    REPEAT('  ', level - 1) || name as name,
    level,
    path
FROM org_tree
ORDER BY path;
```

### 3. MyBatis Usage

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

## 【Mandatory】Window Functions

### 1. Ranking Functions

```sql
-- Salary ranking within each department
SELECT
    name,
    department_id,
    salary,
    RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) as rank,
    DENSE_RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) as dense_rank,
    ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC) as row_num
FROM t_employee;

-- Differences:
-- RANK: 1, 2, 2, 4 (skip number when tied)
-- DENSE_RANK: 1, 2, 2, 3 (no skip when tied)
-- ROW_NUMBER: 1, 2, 3, 4 (unique sequence)
```

### 2. Aggregate Functions

```sql
-- Department average salary vs personal salary
SELECT
    name,
    department_id,
    salary,
    AVG(salary) OVER (PARTITION BY department_id) as dept_avg_salary,
    salary - AVG(salary) OVER (PARTITION BY department_id) as diff_from_avg
FROM t_employee;

-- Cumulative statistics
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

### 3. LAG/LEAD Functions

```sql
-- Compare with previous/next value
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

## 【Recommended】Full-Text Search

### 1. Basic Usage

```sql
-- Add full-text search column
ALTER TABLE t_article
ADD COLUMN search_vector TSVECTOR;

-- Update search vector (Chinese use zhparser or jieba)
UPDATE t_article
SET search_vector = to_tsvector('english', title || ' ' || content);

-- Create GIN index
CREATE INDEX idx_article_search ON t_article USING GIN(search_vector);

-- Full-text search query
SELECT
    id,
    title,
    ts_rank(search_vector, query) AS rank
FROM t_article,
     to_tsquery('english', 'PostgreSQL & performance') AS query
WHERE search_vector @@ query
ORDER BY rank DESC;
```

### 2. Auto-Update Trigger

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

## Checklist

**JSONB Fields**:
- ✅ Use JSONB instead of multiple columns for dynamic fields
- ✅ Add GIN index to support queries
- ✅ Use JacksonTypeHandler for serialization
- ✅ Use @> operator for containment queries

**Array Type**:
- ✅ Use TEXT[] for tag system
- ✅ Add GIN index to support queries
- ✅ Use StringArrayTypeHandler
- ✅ Use && or @> operators for queries

**CTE and Window Functions**:
- ✅ Use CTE to improve readability for complex queries
- ✅ Use RECURSIVE CTE for recursive queries
- ✅ Use Window Functions for statistical analysis
- ✅ Use RANK/DENSE_RANK/ROW_NUMBER for ranking

---

## Related Rules

- **Basic Rules**: [rules/05-postgresql-basics.md](./05-postgresql-basics.md) - Table Creation and Indexing
- **MyBatis Integration**: [rules/05-postgresql-mybatis-integration.md](./05-postgresql-mybatis-integration.md) - SQL Optimization, Migration
- **MyBatis Plus**: [rules/09-mybatis-plus-postgresql.md](./09-mybatis-plus-postgresql.md) - PostgreSQL Configuration
