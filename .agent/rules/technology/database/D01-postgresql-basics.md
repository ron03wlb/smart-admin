---
trigger: always_on
description: PostgreSQL Table Creation and Index Rules
tags: [postgresql, database, schema, indexes]
positioning: ideal
last_updated: 2025-01-12
---

# PostgreSQL Table Creation and Index Rules

**TL;DR**: PostgreSQL table creation must include audit fields (created_at/updated_at/deleted_at), use BIGSERIAL primary key, TIMESTAMPTZ timestamp, JSONB for unstructured data. Index selection: B-Tree (default), GIN (JSONB/array), BRIN (time-series data), partial index (active data).

**Positioning**: This specification is the foundation for PostgreSQL development, applicable to all database design scenarios. Advanced features (JSONB, CTE, Window Functions) see `05-postgresql-advanced.md`, MyBatis integration see `05-postgresql-mybatis.md`.

---

## 【Mandatory】Table Creation Rules

### 1. Required Fields

Every table must include the following fields:

```sql
CREATE TABLE t_user (
    id BIGSERIAL PRIMARY KEY,  -- PostgreSQL auto-increment primary key

    -- Business fields --
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,

    -- Audit fields --
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,  -- Soft delete (NULL means not deleted)
    created_by BIGINT,
    updated_by BIGINT,

    CONSTRAINT uk_user_email UNIQUE (email)
);

-- Auto-update updated_at trigger
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

**Field Description**:
- `id`: Use `BIGSERIAL` (equivalent to `BIGINT` + `AUTO_INCREMENT`)
- `created_at/updated_at`: Use `TIMESTAMPTZ` (timestamp with timezone)
- `deleted_at`: Soft delete field, NULL means not deleted

### 2. Naming Convention

| Type          | Convention             | Correct                   | Incorrect             |
| ------------- | ---------------------- | ------------------------- | --------------------- |
| Table Name    | lowercase+underscore, singular | `t_user`, `t_order_item`  | `Users`, `orderItems` |
| Field Name    | lowercase+underscore   | `user_name`, `created_at` | `userName`            |
| Boolean Field | is_xxx                 | `is_deleted`, `is_active` | `deleted`, `active`   |
| Index Naming  | See index rules        | `idx_user_email`          | `index1`              |

### 3. Type Selection (PostgreSQL-Specific)

```sql
-- ✅ Correct - Use PostgreSQL advantage types
CREATE TABLE t_product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    price NUMERIC(10,2) NOT NULL,          -- Exact numeric (amount)
    status SMALLINT DEFAULT 0,             -- Status code (2 bytes)
    description TEXT,                      -- Unlimited text
    tags TEXT[],                           -- Array type
    metadata JSONB,                        -- JSON storage
    ip_address INET,                       -- IP address
    created_at TIMESTAMPTZ DEFAULT NOW(),  -- Timestamp with timezone
    valid_period DATERANGE                 -- Date range
);
```

**Type Mapping Recommendations**:
- Integer: `SMALLINT` (2 bytes) / `INTEGER` (4 bytes) / `BIGINT` (8 bytes)
- Exact Numeric: `NUMERIC(precision, scale)`
- Text: `VARCHAR(n)` / `TEXT` (no length limit)
- Time: `TIMESTAMPTZ` (with timezone) / `DATE` / `TIME`
- Boolean: `BOOLEAN`
- JSON: `JSONB` (recommended, supports indexing) / `JSON`
- Array: `type[]` such as `TEXT[]`, `INTEGER[]`

---

## 【Mandatory】Index Rules

### 1. Index Naming

- Primary Key: `pk_tablename` or default (system generated)
- Unique Index: `uk_tablename_columnname`
- Regular Index: `idx_tablename_columnname`
- Foreign Key Index: `fk_tablename_columnname`

### 2. PostgreSQL Index Types

#### 2.1 B-Tree (Default, Most Scenarios)

```sql
-- Suitable for: Equality queries, range queries, sorting
CREATE INDEX idx_user_email ON t_user(email);
CREATE INDEX idx_order_created ON t_order(created_at);

-- Composite index
CREATE INDEX idx_order_user_status ON t_order(user_id, status, created_at);
```

#### 2.2 GIN (JSONB, Array, Full-Text Search)

```sql
-- JSONB index
CREATE INDEX idx_order_items ON t_order USING GIN(items);
CREATE INDEX idx_metadata ON t_order USING GIN(metadata jsonb_path_ops);

-- Array index
CREATE INDEX idx_article_tags ON t_article USING GIN(tags);

-- Full-text search index
CREATE INDEX idx_article_search ON t_article USING GIN(search_vector);
```

#### 2.3 GiST (Geolocation, Range Types)

```sql
-- Geolocation index (requires PostGIS extension)
CREATE INDEX idx_store_location ON t_store USING GIST(location);

-- Range type index
CREATE INDEX idx_event_period ON t_event USING GIST(valid_period);
```

#### 2.4 BRIN (Large Tables with Ordered Data)

```sql
-- Suitable for: Time-series data, log tables
CREATE INDEX idx_log_created ON t_log USING BRIN(created_at);
```

#### 2.5 Partial Index (Index Active Data Only)

```sql
-- Index only non-deleted data
CREATE INDEX idx_active_users ON t_user(email)
WHERE deleted_at IS NULL AND status = 'ACTIVE';

-- Index only recent year orders
CREATE INDEX idx_recent_orders ON t_order(user_id, created_at)
WHERE created_at > NOW() - INTERVAL '1 year';
```

#### 2.6 Expression Index

```sql
-- Lowercase email index
CREATE INDEX idx_user_lower_email ON t_user(LOWER(email));

-- JSON field extraction index
CREATE INDEX idx_order_user_id ON t_order((metadata->>'user_id'));
```

### 3. Composite Index Design

```sql
-- Query: WHERE status=? AND type=? ORDER BY created_at
-- ✅ Correct order: Equality query first, range/sort last
CREATE INDEX idx_order_status_type_created
ON t_order(status, type, created_at);
```

**Leftmost Prefix Principle**:

```sql
-- Index: idx_a_b_c (a, b, c)

-- ✅ Can use index
WHERE a = 1
WHERE a = 1 AND b = 2
WHERE a = 1 AND b = 2 AND c = 3

-- ❌ Cannot use index
WHERE b = 2           -- Missing a
WHERE b = 2 AND c = 3 -- Missing a
WHERE a = 1 AND c = 3 -- Skip b, only uses a
```

---

## Checklist

**Table Creation Rules**:
- ✅ Use BIGSERIAL primary key
- ✅ Time fields use TIMESTAMPTZ
- ✅ Add created_at/updated_at trigger
- ✅ Table name, field name lowercase+underscore
- ✅ Amount uses NUMERIC type
- ✅ Soft delete uses deleted_at (TIMESTAMPTZ)

**Index Optimization**:
- ✅ Select correct index type (B-Tree/GIN/BRIN)
- ✅ Partial index reduces index size
- ✅ Composite index follows leftmost prefix principle
- ✅ JSONB/array uses GIN index
- ✅ Time-series data uses BRIN index
- ✅ Active data uses partial index

---

## Related Rules

- **Advanced Features**: [technology/database/D02-postgresql-advanced.md](./D02-postgresql-advanced.md) - JSONB, Array, CTE, Window Functions
- **MyBatis Integration**: [technology/database/D03-postgresql-mybatis.md](./D03-postgresql-mybatis.md) - Complete PostgreSQL + MyBatis Plus Integration
- **MyBatis Plus**: [technology/database/09-mybatis-plus-core.md](./09-mybatis-plus-core.md) - Core Configuration
- **Vavr Integration**: [technology/functional/P03-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md) - Functional Query
