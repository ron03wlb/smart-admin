# Database Migration Manager - Quick Reference

## Flyway 命名規範

```
V{version}__{description}.sql    # 版本遷移
U{version}__{description}.sql    # 撤銷遷移 (Flyway Teams)
R__{description}.sql             # 可重複遷移
```

**範例**:
- `V1.0.0__Create_employee_table.sql`
- `V1.0.1__Add_email_to_employee.sql`
- `V1.1.0__Create_brand_table.sql`

## SmartAdmin 表結構模板

```sql
CREATE TABLE t_{entity} (
    {entity}_id BIGSERIAL PRIMARY KEY,
    -- 業務欄位
    {field_name} VARCHAR(100) NOT NULL,
    -- 標準欄位
    sort INT NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 1,
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,
    remark VARCHAR(500),
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_{entity}_{field} ON t_{entity}({field});
CREATE INDEX idx_{entity}_status_deleted ON t_{entity}(status, deleted_flag);

-- 註解
COMMENT ON TABLE t_{entity} IS '{Entity} management table';
COMMENT ON COLUMN t_{entity}.{entity}_id IS '{Entity} ID (primary key)';
```

## 常用操作

| 操作 | SQL 語法 |
|------|----------|
| 添加欄位 | `ALTER TABLE t_x ADD COLUMN col TYPE;` |
| 修改欄位 | `ALTER TABLE t_x ALTER COLUMN col TYPE newtype;` |
| 刪除欄位 | `ALTER TABLE t_x DROP COLUMN col;` |
| 添加索引 | `CREATE INDEX idx_x_col ON t_x(col);` |
| 添加唯一約束 | `ALTER TABLE t_x ADD CONSTRAINT uk_x_col UNIQUE (col);` |

## 資料遷移模板

```sql
-- 資料遷移 (with transaction)
BEGIN;

-- 備份原資料
CREATE TABLE t_backup AS SELECT * FROM t_original;

-- 執行遷移
UPDATE t_original SET new_col = old_col WHERE condition;

-- 驗證
SELECT COUNT(*) FROM t_original WHERE new_col IS NULL;

COMMIT;
```
