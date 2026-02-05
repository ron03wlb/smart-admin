# Database Migration Manager - Examples

## 範例 1: 創建新資料表

**需求**: 創建 brand 品牌資料表

```sql
-- V1.1.0__Create_brand_table.sql
CREATE TABLE t_brand (
    brand_id BIGSERIAL PRIMARY KEY,
    brand_name VARCHAR(50) NOT NULL,
    brand_logo VARCHAR(200),
    description VARCHAR(500),
    sort INT NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 1,
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_brand_name ON t_brand(brand_name);
CREATE INDEX idx_brand_status_deleted ON t_brand(status, deleted_flag);

-- Comments
COMMENT ON TABLE t_brand IS 'Brand management table';
COMMENT ON COLUMN t_brand.brand_id IS 'Brand ID (primary key)';
COMMENT ON COLUMN t_brand.brand_name IS 'Brand name (max 50 chars)';
COMMENT ON COLUMN t_brand.status IS 'Status (1=Enabled, 0=Disabled)';
```

---

## 範例 2: 添加欄位

**需求**: 為 employee 表添加 email 欄位

```sql
-- V1.1.1__Add_email_to_employee.sql
ALTER TABLE t_employee ADD COLUMN email VARCHAR(100);

-- Add index for email lookups
CREATE INDEX idx_employee_email ON t_employee(email);

-- Add comment
COMMENT ON COLUMN t_employee.email IS 'Employee email address';
```

**回滾腳本**:
```sql
-- R__Remove_email_from_employee.sql
DROP INDEX IF EXISTS idx_employee_email;
ALTER TABLE t_employee DROP COLUMN IF EXISTS email;
```

---

## 範例 3: 資料遷移

**需求**: 將 is_deleted 欄位遷移為 deleted_flag

```sql
-- V1.2.0__Migrate_deleted_flag.sql
BEGIN;

-- Add new column
ALTER TABLE t_employee ADD COLUMN deleted_flag BOOLEAN DEFAULT FALSE;

-- Migrate data
UPDATE t_employee SET deleted_flag = (is_deleted = 1);

-- Set NOT NULL after migration
ALTER TABLE t_employee ALTER COLUMN deleted_flag SET NOT NULL;

-- Drop old column
ALTER TABLE t_employee DROP COLUMN is_deleted;

COMMIT;
```

---

## 範例 4: Liquibase XML 格式

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="1.1.0-create-brand" author="smartadmin">
        <createTable tableName="t_brand">
            <column name="brand_id" type="BIGSERIAL" autoIncrement="true">
                <constraints primaryKey="true"/>
            </column>
            <column name="brand_name" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="INT" defaultValue="1">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <rollback>
            <dropTable tableName="t_brand"/>
        </rollback>
    </changeSet>

</databaseChangeLog>
```
