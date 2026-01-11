---
trigger: always_on
---

# PostgreSQL 数据库规范

基于 PostgreSQL 16 的数据库设计、查询优化和最佳实践规范，
涵盖 PostgreSQL 特有功能（JSONB、数组、CTE、窗口函数）及与 MyBatis Plus 的整合。

---

## 【强制】建表规范

### 1. 必备字段

每张表必须包含以下字段：

```sql
CREATE TABLE t_user (
    id BIGSERIAL PRIMARY KEY,  -- PostgreSQL 自增主键

    -- 业务字段 --
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,

    -- 审计字段 --
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,  -- 软删除（NULL 表示未删除）
    created_by BIGINT,
    updated_by BIGINT,

    CONSTRAINT uk_user_email UNIQUE (email)
);

-- 自动更新 updated_at 触发器
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

**字段说明**:
- `id`: 使用 `BIGSERIAL`（等价于 `BIGINT` + `AUTO_INCREMENT`）
- `created_at/updated_at`: 使用 `TIMESTAMPTZ`（带时区时间戳）
- `deleted_at`: 软删除字段，NULL 表示未删除

### 2. 命名规范

| 类型     | 规范              | 正例                      | 反例                  |
| -------- | ----------------- | ------------------------- | --------------------- |
| 表名     | 小写+下划线，单数 | `t_user`, `t_order_item`  | `Users`, `orderItems` |
| 字段名   | 小写+下划线       | `user_name`, `created_at` | `userName`            |
| 布尔字段 | is_xxx            | `is_deleted`, `is_active` | `deleted`, `active`   |
| 索引命名 | 见索引规范        | `idx_user_email`          | `index1`              |

### 3. 类型选择（PostgreSQL 专用）

```sql
-- ✅ 正确 - 使用 PostgreSQL 优势类型
CREATE TABLE t_product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    price NUMERIC(10,2) NOT NULL,          -- 精确数值（金额）
    status SMALLINT DEFAULT 0,             -- 状态码（2字节）
    description TEXT,                      -- 不限长度文本
    tags TEXT[],                           -- 数组类型
    metadata JSONB,                        -- JSON 存储
    ip_address INET,                       -- IP 地址
    created_at TIMESTAMPTZ DEFAULT NOW(),  -- 带时区时间戳
    valid_period DATERANGE                 -- 日期范围
);

-- ❌ 错误 - 不适用
price FLOAT                  -- 精度丢失
status INTEGER               -- 浪费空间
created_at TIMESTAMP         -- 无时区信息（推荐 TIMESTAMPTZ）
```

**类型映射建议**:
- 整数: `SMALLINT` (2字节) / `INTEGER` (4字节) / `BIGINT` (8字节)
- 精确数值: `NUMERIC(precision, scale)`
- 文本: `VARCHAR(n)` / `TEXT`（无长度限制）
- 时间: `TIMESTAMPTZ`（带时区） / `DATE` / `TIME`
- 布尔: `BOOLEAN`
- JSON: `JSONB`（推荐，支持索引） / `JSON`
- 数组: `type[]` 如 `TEXT[]`, `INTEGER[]`

---

## 【强制】PostgreSQL 特有功能

### 1. JSONB 字段

#### 1.1 使用场景

- 灵活的扩展字段（避免频繁加字段）
- 动态表单数据
- 审计日志（记录变更详情）
- 配置存储

#### 1.2 建表示例

```sql
CREATE TABLE t_order (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,

    -- JSONB 字段
    items JSONB NOT NULL,       -- 订单项详情
    metadata JSONB,             -- 扩展字段

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- JSONB 索引 (GIN - Generalized Inverted Index)
CREATE INDEX idx_order_items ON t_order USING GIN(items);
CREATE INDEX idx_order_metadata ON t_order USING GIN(metadata jsonb_path_ops);
```

#### 1.3 查询操作

```sql
-- 查询包含特定商品的订单
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

-- 删除 JSONB 键
UPDATE t_order
SET metadata = metadata - 'temp_field'
WHERE id = 1;

-- 检查键是否存在
SELECT * FROM t_order
WHERE metadata ? 'vip_discount';
```

#### 1.4 MyBatis Plus 整合

```java
// Entity 定义
@Data
@TableName("t_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    // JSONB 映射为对象，使用 TypeHandler 转换
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<OrderItem> items;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    private OffsetDateTime createdAt;
}

// TypeHandler 实现
public class JacksonTypeHandler extends BaseTypeHandler<Object> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Class<?> type;

    public JacksonTypeHandler(Class<?> type) {
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    Object parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        try {
            jsonObject.setValue(MAPPER.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON 序列化失败", e);
        }
        ps.setObject(i, jsonObject);
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        String json = rs.getString(columnName);
        return fromJson(json);
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        String json = rs.getString(columnIndex);
        return fromJson(json);
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        String json = cs.getString(columnIndex);
        return fromJson(json);
    }

    private Object fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }
}

// Mapper 查询
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    // JSONB 包含查询
    default List<Order> findByProductId(Long productId) {
        return selectList(
            new LambdaQueryWrapper<Order>()
                .apply("items @> '[{\"product_id\": {0}}]'::jsonb", productId)
        );
    }
}
```

### 2. 数组类型

#### 2.1 使用场景

- 标签系统（tags）
- 权限列表（roles）
- 多选字段

#### 2.2 建表示例

```sql
CREATE TABLE t_article (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,

    -- 数组字段
    tags TEXT[],            -- 标签数组
    category_ids INTEGER[], -- 分类 ID 数组

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 数组索引 (GIN)
CREATE INDEX idx_article_tags ON t_article USING GIN(tags);
```

#### 2.3 查询操作

```sql
-- 插入数组
INSERT INTO t_article (title, content, tags)
VALUES ('示例文章', '内容', ARRAY['Java', 'PostgreSQL', 'Spring']);

-- 查询包含特定标签（任一匹配）
SELECT * FROM t_article
WHERE tags && ARRAY['Java', 'Python'];

-- 查询包含所有标签
SELECT * FROM t_article
WHERE tags @> ARRAY['Java', 'Spring'];

-- 查询特定位置元素
SELECT * FROM t_article
WHERE tags[1] = 'Java';

-- 数组追加
UPDATE t_article
SET tags = array_append(tags, 'NewTag')
WHERE id = 1;

-- 数组移除
UPDATE t_article
SET tags = array_remove(tags, 'OldTag')
WHERE id = 1;

-- 数组长度
SELECT id, array_length(tags, 1) as tag_count
FROM t_article;
```

#### 2.4 MyBatis Plus 整合

```java
// Entity 定义
@Data
@TableName("t_article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] tags;

    @TableField(typeHandler = IntegerArrayTypeHandler.class)
    private Integer[] categoryIds;
}

// TypeHandler 实现
public class StringArrayTypeHandler extends BaseTypeHandler<String[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    String[] parameter, JdbcType jdbcType)
            throws SQLException {
        Connection conn = ps.getConnection();
        Array array = conn.createArrayOf("TEXT", parameter);
        ps.setArray(i, array);
    }

    @Override
    public String[] getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        Array array = rs.getArray(columnName);
        return array != null ? (String[]) array.getArray() : null;
    }

    @Override
    public String[] getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        Array array = rs.getArray(columnIndex);
        return array != null ? (String[]) array.getArray() : null;
    }

    @Override
    public String[] getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        Array array = cs.getArray(columnIndex);
        return array != null ? (String[]) array.getArray() : null;
    }
}

// Mapper 查询
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    // 查询包含特定标签
    default List<Article> findByTag(String tag) {
        return selectList(
            new LambdaQueryWrapper<Article>()
                .apply("tags && ARRAY[{0}]::TEXT[]", tag)
        );
    }
}
```

### 3. CTE (公用表表达式)

#### 3.1 基本 CTE

```sql
-- 提升可读性和可维护性
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

#### 3.2 递归 CTE（组织树）

```sql
-- 递归查询组织树
WITH RECURSIVE org_tree AS (
    -- 初始查询（根节点）
    SELECT
        id,
        name,
        parent_id,
        1 as level,
        ARRAY[id] as path
    FROM t_department
    WHERE parent_id IS NULL

    UNION ALL

    -- 递归部分
    SELECT
        d.id,
        d.name,
        d.parent_id,
        ot.level + 1,
        ot.path || d.id
    FROM t_department d
    JOIN org_tree ot ON d.parent_id = ot.id
    WHERE NOT d.id = ANY(ot.path)  -- 防止循环
)
SELECT
    id,
    REPEAT('  ', level - 1) || name as name,
    level,
    path
FROM org_tree
ORDER BY path;
```

#### 3.3 MyBatis 中使用

```xml
<!-- Mapper XML -->
<select id="getDepartmentStats" resultType="DepartmentStatVO">
    WITH active_users AS (
        SELECT id, name, department_id
        FROM t_user
        WHERE status = 'ACTIVE' AND deleted_at IS NULL
    ),
    department_stats AS (
        SELECT
            department_id,
            COUNT(*) as user_count
        FROM active_users
        GROUP BY department_id
    )
    SELECT
        d.id,
        d.name,
        COALESCE(ds.user_count, 0) as user_count
    FROM t_department d
    LEFT JOIN department_stats ds ON d.id = ds.department_id
</select>
```

### 4. 窗口函数

#### 4.1 排名函数

```sql
-- 每个部门的薪资排名
SELECT
    name,
    department_id,
    salary,
    RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) as rank,
    DENSE_RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) as dense_rank,
    ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC) as row_num
FROM t_employee;

-- 差异：
-- RANK: 1, 2, 2, 4（有并列时跳号）
-- DENSE_RANK: 1, 2, 2, 3（有并列不跳号）
-- ROW_NUMBER: 1, 2, 3, 4（唯一序号）
```

#### 4.2 聚合函数

```sql
-- 部门平均薪资 vs 个人薪资
SELECT
    name,
    department_id,
    salary,
    AVG(salary) OVER (PARTITION BY department_id) as dept_avg_salary,
    salary - AVG(salary) OVER (PARTITION BY department_id) as diff_from_avg
FROM t_employee;

-- 累计统计
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

#### 4.3 LAG/LEAD 函数

```sql
-- 对比上一次/下一次的值
SELECT
    order_date,
    amount,
    LAG(amount, 1) OVER (ORDER BY order_date) as prev_amount,
    LEAD(amount, 1) OVER (ORDER BY order_date) as next_amount,
    amount - LAG(amount, 1) OVER (ORDER BY order_date) as change
FROM t_order
ORDER BY order_date;
```

### 5. 全文搜索

#### 5.1 基本使用

```sql
-- 添加全文搜索列
ALTER TABLE t_article
ADD COLUMN search_vector TSVECTOR;

-- 更新搜索向量（中文使用 zhparser 或 jieba）
UPDATE t_article
SET search_vector = to_tsvector('english', title || ' ' || content);

-- 创建 GIN 索引
CREATE INDEX idx_article_search ON t_article USING GIN(search_vector);

-- 全文搜索查询
SELECT
    id,
    title,
    ts_rank(search_vector, query) AS rank
FROM t_article,
     to_tsquery('english', 'PostgreSQL & performance') AS query
WHERE search_vector @@ query
ORDER BY rank DESC;
```

#### 5.2 自动更新触发器

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

## 【强制】索引规范

### 1. 索引命名

- 主键: `pk_tablename` 或 默认（系统生成）
- 唯一索引: `uk_tablename_columnname`
- 普通索引: `idx_tablename_columnname`
- 外键索引: `fk_tablename_columnname`

### 2. PostgreSQL 索引类型

#### 2.1 B-Tree（默认，大多数场景）

```sql
-- 适用于: 等值查询、范围查询、排序
CREATE INDEX idx_user_email ON t_user(email);
CREATE INDEX idx_order_created ON t_order(created_at);

-- 联合索引
CREATE INDEX idx_order_user_status ON t_order(user_id, status, created_at);
```

#### 2.2 GIN（JSONB、数组、全文搜索）

```sql
-- JSONB 索引
CREATE INDEX idx_order_items ON t_order USING GIN(items);
CREATE INDEX idx_metadata ON t_order USING GIN(metadata jsonb_path_ops);

-- 数组索引
CREATE INDEX idx_article_tags ON t_article USING GIN(tags);

-- 全文搜索索引
CREATE INDEX idx_article_search ON t_article USING GIN(search_vector);
```

#### 2.3 GiST（地理位置、范围类型）

```sql
-- 地理位置索引（需要 PostGIS 扩展）
CREATE INDEX idx_store_location ON t_store USING GIST(location);

-- 范围类型索引
CREATE INDEX idx_event_period ON t_event USING GIST(valid_period);
```

#### 2.4 BRIN（大表且数据有序）

```sql
-- 适用于: 时间序列数据、日志表
CREATE INDEX idx_log_created ON t_log USING BRIN(created_at);
```

#### 2.5 部分索引（仅索引活跃数据）

```sql
-- 仅索引未删除的数据
CREATE INDEX idx_active_users ON t_user(email)
WHERE deleted_at IS NULL AND status = 'ACTIVE';

-- 仅索引最近一年的订单
CREATE INDEX idx_recent_orders ON t_order(user_id, created_at)
WHERE created_at > NOW() - INTERVAL '1 year';
```

#### 2.6 表达式索引

```sql
-- 小写邮箱索引
CREATE INDEX idx_user_lower_email ON t_user(LOWER(email));

-- JSON 字段提取索引
CREATE INDEX idx_order_user_id ON t_order((metadata->>'user_id'));
```

### 3. 联合索引设计

```sql
-- 查询: WHERE status=? AND type=? ORDER BY created_at
-- ✅ 正确顺序：等值查询在前，范围/排序在后
CREATE INDEX idx_order_status_type_created
ON t_order(status, type, created_at);

-- ❌ 错误：排序字段在中间，破坏有序性
CREATE INDEX idx_order_status_created_type
ON t_order(status, created_at, type);
```

**最左前缀原则**:

```sql
-- 索引: idx_a_b_c (a, b, c)

-- ✅ 可以使用索引
WHERE a = 1
WHERE a = 1 AND b = 2
WHERE a = 1 AND b = 2 AND c = 3

-- ❌ 无法使用索引
WHERE b = 2           -- 缺少 a
WHERE b = 2 AND c = 3 -- 缺少 a
WHERE a = 1 AND c = 3 -- 跳过 b，只能用 a
```

---

## 【强制】SQL 语句

### 1. 禁止 SELECT *

```java
// ✅ 正确
@Select("SELECT id, name, email, status FROM t_user WHERE id = #{id}")
User selectById(Long id);

// ❌ 错误
@Select("SELECT * FROM t_user WHERE id = #{id}")
User selectById(Long id);
```

### 2. 分页优化

```sql
-- ❌ 低效：OFFSET 扫描大量数据
SELECT * FROM t_order
LIMIT 20 OFFSET 100000;

-- ✅ 高效：使用主键范围
SELECT * FROM t_order
WHERE id > 100000
ORDER BY id
LIMIT 20;

-- ✅ 高效：延迟关联（复杂条件）
SELECT a.* FROM t_order a
INNER JOIN (
    SELECT id FROM t_order
    WHERE status = 'PAID'
    ORDER BY created_at DESC
    LIMIT 20 OFFSET 100000
) b ON a.id = b.id;
```

### 3. 禁止左模糊查询

```sql
-- ❌ 无法使用索引
WHERE email LIKE '%@gmail.com';

-- ✅ 可以使用索引
WHERE email LIKE 'john%';

-- ✅ 全文搜索（需要时）
WHERE search_vector @@ to_tsquery('gmail');
```

### 4. JOIN 优化

```sql
-- ✅ 小表驱动大表
SELECT o.id, o.amount, u.name
FROM t_order o
INNER JOIN t_user u ON o.user_id = u.id
WHERE u.status = 'ACTIVE';  -- 先过滤用户

-- 确保 JOIN 字段有索引
CREATE INDEX idx_order_user_id ON t_order(user_id);
CREATE INDEX idx_user_id ON t_user(id);  -- 主键自动有索引
```

### 5. IN vs EXISTS

```sql
-- ✅ 小集合用 IN
SELECT * FROM t_order
WHERE user_id IN (1, 2, 3);

-- ✅ 子查询用 EXISTS
SELECT * FROM t_order o
WHERE EXISTS (
    SELECT 1 FROM t_user u
    WHERE u.id = o.user_id
      AND u.vip = TRUE
);
```

---

## 【强制】数据迁移（MySQL → PostgreSQL）

### 1. 类型映射

| MySQL           | PostgreSQL     | 说明                    |
| --------------- | -------------- | ----------------------- |
| TINYINT         | SMALLINT       | 1字节 → 2字节           |
| INT             | INTEGER        | 相同                    |
| BIGINT          | BIGINT         | 相同                    |
| BIGINT UNSIGNED | BIGINT         | 去掉 UNSIGNED           |
| DATETIME        | TIMESTAMPTZ    | 推荐带时区              |
| TIMESTAMP       | TIMESTAMPTZ    | 推荐带时区              |
| VARCHAR(n)      | VARCHAR(n)     | 相同                    |
| TEXT            | TEXT           | 相同                    |
| DECIMAL(m,n)    | NUMERIC(m,n)   | 推荐 NUMERIC            |
| JSON            | JSONB          | 推荐 JSONB（支持索引）  |
| ENUM            | VARCHAR + CHECK| 或使用 ENUM 类型        |

### 2. SQL 语法差异

```sql
-- MySQL
SELECT * FROM t_user LIMIT 10 OFFSET 20;
INSERT INTO t_user VALUES (1, 'John')
ON DUPLICATE KEY UPDATE name='John';

-- PostgreSQL
SELECT * FROM t_user LIMIT 10 OFFSET 20;  -- 相同
INSERT INTO t_user VALUES (1, 'John')
ON CONFLICT (id) DO UPDATE SET name='John';  -- 不同语法
```

### 3. 自增主键

```sql
-- MySQL
id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY

-- PostgreSQL
id BIGSERIAL PRIMARY KEY
-- 或
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
```

### 4. MyBatis Plus 配置调整

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
    # PostgreSQL 标识符使用双引号
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      # PostgreSQL 主键策略
      id-type: AUTO
      # 逻辑删除字段
      logic-delete-field: deletedAt
      logic-delete-value: 'NOW()'
      logic-not-delete-value: 'NULL'
```

**依赖更新**:

```xml
<!-- 移除 MySQL 驱动 -->
<!-- <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency> -->

<!-- 添加 PostgreSQL 驱动 -->
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

---

## 【推荐】性能优化

### 1. EXPLAIN ANALYZE

```sql
-- 查看执行计划
EXPLAIN ANALYZE
SELECT * FROM t_user
WHERE email = 'test@example.com'
  AND status = 'ACTIVE';

-- 输出示例：
-- Seq Scan on t_user  (cost=0.00..15.25 rows=1 width=100)
--   Filter: ((email = 'test@example.com') AND (status = 'ACTIVE'))
--   Rows Removed by Filter: 99
-- Planning Time: 0.123 ms
-- Execution Time: 0.456 ms
```

**关注指标**:
- `Seq Scan`: 全表扫描（慢）→ 需要添加索引
- `Index Scan`: 索引扫描（快）
- `Bitmap Index Scan`: 位图索引扫描（中）
- `rows`: 估计行数 vs 实际行数
- `cost`: 估计成本
- `Execution Time`: 实际执行时间

### 2. VACUUM 和 ANALYZE

```sql
-- 清理死元组，更新统计信息
VACUUM ANALYZE t_user;

-- 仅更新统计信息
ANALYZE t_user;

-- 完全清理（锁表，谨慎使用）
VACUUM FULL t_user;

-- 自动配置
ALTER TABLE t_user SET (
    autovacuum_enabled = true,
    autovacuum_vacuum_scale_factor = 0.1,
    autovacuum_analyze_scale_factor = 0.05
);
```

### 3. 连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20       # 最大连接数
      minimum-idle: 5             # 最小空闲连接
      connection-timeout: 30000   # 连接超时（ms）
      idle-timeout: 600000        # 空闲超时（10分钟）
      max-lifetime: 1800000       # 最大生命周期（30分钟）
      connection-test-query: SELECT 1
```

**连接数计算公式**:
```
connections = ((core_count * 2) + effective_spindle_count)
```

例如: 4核 CPU + 1个磁盘 = 9个连接

### 4. 慢查询日志

```sql
-- postgresql.conf
log_min_duration_statement = 1000  -- 记录超过1秒的查询
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d '
log_statement = 'all'  -- 记录所有语句（生产环境用 'none'）
```

---

## 检查清单

**建表规范**:
- ✅ 使用 BIGSERIAL 主键
- ✅ 时间字段使用 TIMESTAMPTZ
- ✅ 添加 created_at/updated_at 触发器
- ✅ 表名、字段名小写+下划线

**PostgreSQL 特性**:
- ✅ 扩展字段使用 JSONB + GIN 索引
- ✅ 标签系统使用数组 + GIN 索引
- ✅ 复杂查询使用 CTE 提升可读性
- ✅ 统计分析使用窗口函数

**索引优化**:
- ✅ 选择正确的索引类型（B-Tree/GIN/BRIN）
- ✅ 部分索引减少索引大小
- ✅ 联合索引遵循最左前缀原则
- ✅ 定期 VACUUM ANALYZE

**SQL 规范**:
- ✅ 禁止 SELECT *
- ✅ 分页使用主键范围或延迟关联
- ✅ 禁止左模糊查询
- ✅ JOIN 字段有索引

**MyBatis Plus**:
- ✅ 主键策略 id-type: AUTO
- ✅ JSONB 使用 TypeHandler (PGobject)
- ✅ 数组使用 TypeHandler (java.sql.Array)
