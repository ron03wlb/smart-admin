# Flyway Migration Patterns and Best Practices

## File Naming Convention

```
V{version}__{description}.sql
V1.0.0__Initial_schema.sql
V1.0.1__Add_brand_table.sql
V1.1.0__Add_email_to_employee.sql
V2.0.0__Major_refactor_departments.sql

U{version}__{description}.sql  (Undo migrations - Teams Edition only)
U1.0.1__Rollback_add_brand_table.sql
```

**Version format:**
- `V` = Versioned migration (required)
- `U` = Undo migration (optional)
- `__` = Double underscore separator (required)
- Description uses underscores: `Add_email_to_employee`

## SmartAdmin Table Standards

Every table migration should follow:

```sql
CREATE TABLE t_{module_name} (
    {module}_id BIGSERIAL PRIMARY KEY,  -- Auto-increment primary key
    -- Business columns here
    status INT NOT NULL DEFAULT 1,      -- Status (1=active, 0=inactive)
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,  -- Soft delete
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_{table}_{column} ON t_{table}({column});
CREATE INDEX idx_{table}_status_deleted ON t_{table}(status, deleted_flag);

-- Comments (PostgreSQL)
COMMENT ON TABLE t_{table} IS 'Table description';
COMMENT ON COLUMN t_{table}.{column} IS 'Column description';
```

## Migration Patterns by Type

### 1. Create Table

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

CREATE UNIQUE INDEX idx_brand_name_unique ON t_brand(brand_name) WHERE deleted_flag = FALSE;
CREATE INDEX idx_brand_status_deleted ON t_brand(status, deleted_flag);

COMMENT ON TABLE t_brand IS 'Brand management table';
COMMENT ON COLUMN t_brand.brand_name IS 'Brand name (max 50 chars, unique when active)';
```

**Rollback:**
```sql
-- U1.1.0__Rollback_create_brand_table.sql
DROP TABLE IF EXISTS t_brand;
```

### 2. Add Column

```sql
-- V1.1.1__Add_email_to_employee.sql
ALTER TABLE t_employee ADD COLUMN email VARCHAR(100);
CREATE INDEX idx_employee_email ON t_employee(email);
COMMENT ON COLUMN t_employee.email IS 'Employee email address (optional)';
```

**Rollback:**
```sql
-- U1.1.1__Rollback_add_email_to_employee.sql
DROP INDEX IF EXISTS idx_employee_email;
ALTER TABLE t_employee DROP COLUMN email;
```

### 3. Add Foreign Key

```sql
-- V1.1.2__Add_brand_fk_to_goods.sql
-- Step 1: Add column
ALTER TABLE t_goods ADD COLUMN brand_id BIGINT;

-- Step 2: Populate with default value (if needed)
UPDATE t_goods SET brand_id = 1 WHERE brand_id IS NULL;

-- Step 3: Add foreign key constraint
ALTER TABLE t_goods
ADD CONSTRAINT fk_goods_brand
FOREIGN KEY (brand_id) REFERENCES t_brand(brand_id);

-- Step 4: Add index for FK
CREATE INDEX idx_goods_brand_id ON t_goods(brand_id);
```

**Rollback:**
```sql
-- U1.1.2__Rollback_add_brand_fk_to_goods.sql
ALTER TABLE t_goods DROP CONSTRAINT IF EXISTS fk_goods_brand;
DROP INDEX IF EXISTS idx_goods_brand_id;
ALTER TABLE t_goods DROP COLUMN brand_id;
```

### 4. Modify Column

```sql
-- V1.1.3__Change_employee_phone_length.sql
-- PostgreSQL
ALTER TABLE t_employee ALTER COLUMN phone TYPE VARCHAR(20);

-- MySQL
ALTER TABLE t_employee MODIFY COLUMN phone VARCHAR(20);
```

**Rollback:**
```sql
-- U1.1.3__Rollback_change_employee_phone_length.sql
ALTER TABLE t_employee ALTER COLUMN phone TYPE VARCHAR(15);
```

### 5. Rename Column

```sql
-- V1.1.4__Rename_employee_name_to_actual_name.sql
ALTER TABLE t_employee RENAME COLUMN name TO actual_name;
```

**Rollback:**
```sql
-- U1.1.4__Rollback_rename_employee_name_to_actual_name.sql
ALTER TABLE t_employee RENAME COLUMN actual_name TO name;
```

### 6. Add Index

```sql
-- V1.1.5__Add_index_employee_department_status.sql
CREATE INDEX idx_employee_dept_status ON t_employee(department_id, status);
```

**Rollback:**
```sql
-- U1.1.5__Rollback_add_index_employee_department_status.sql
DROP INDEX IF EXISTS idx_employee_dept_status;
```

### 7. Data Migration

```sql
-- V1.1.6__Migrate_department_employees.sql
-- Migrate employees from old department (5) to new department (10)
UPDATE t_employee 
SET department_id = 10, update_time = CURRENT_TIMESTAMP
WHERE department_id = 5 AND deleted_flag = FALSE;
```

**Rollback:**
```sql
-- U1.1.6__Rollback_migrate_department_employees.sql
UPDATE t_employee 
SET department_id = 5, update_time = CURRENT_TIMESTAMP
WHERE department_id = 10 AND deleted_flag = FALSE;
```

### 8. Insert Reference Data

```sql
-- V1.1.7__Insert_initial_brands.sql
INSERT INTO t_brand (brand_id, brand_name, description, sort, status) VALUES
(1, 'Nike', 'Sports brand', 1, 1),
(2, 'Adidas', 'Sports brand', 2, 1),
(3, 'Puma', 'Sports brand', 3, 1)
ON CONFLICT (brand_id) DO NOTHING;  -- Idempotent
```

**Rollback:**
```sql
-- U1.1.7__Rollback_insert_initial_brands.sql
DELETE FROM t_brand WHERE brand_id IN (1, 2, 3);
```

## Flyway Configuration

### Spring Boot (application.yml)

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true          # Allow migration on existing DB
    validate-on-migrate: true          # Validate checksums
    out-of-order: false                # Enforce sequential order
    placeholder-replacement: true      # Enable ${placeholder} substitution
    placeholders:
      schema: smart_admin
      table_prefix: t_
```

### Gradle (build.gradle.kts)

```kotlin
plugins {
    id("org.flywaydb.flyway") version "10.4.1"
}

flyway {
    url = "jdbc:postgresql://localhost:5432/smart_admin"
    user = "admin"
    password = "secret"
    locations = arrayOf("filesystem:src/main/resources/db/migration")
    baselineOnMigrate = true
}
```

## Common Flyway Commands

```bash
# Apply all pending migrations
./gradlew flywayMigrate

# Show migration status
./gradlew flywayInfo

# Validate applied migrations
./gradlew flywayValidate

# Repair failed migration
./gradlew flywayRepair

# Clean database (WARNING: Drops all objects)
./gradlew flywayClean

# Undo last migration (Teams Edition)
./gradlew flywayUndo
```

## Idempotency Patterns

### 1. CREATE TABLE IF NOT EXISTS

```sql
CREATE TABLE IF NOT EXISTS t_brand (
    brand_id BIGSERIAL PRIMARY KEY,
    ...
);
```

### 2. ALTER TABLE with existence check

```sql
-- PostgreSQL
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 't_employee' AND column_name = 'email'
    ) THEN
        ALTER TABLE t_employee ADD COLUMN email VARCHAR(100);
    END IF;
END $$;
```

### 3. INSERT with ON CONFLICT

```sql
INSERT INTO t_brand (brand_id, brand_name, sort, status) 
VALUES (1, 'Nike', 1, 1)
ON CONFLICT (brand_id) DO NOTHING;
```

### 4. CREATE INDEX IF NOT EXISTS

```sql
CREATE INDEX IF NOT EXISTS idx_employee_email ON t_employee(email);
```

## Migration Testing Checklist

Before committing migration:

- [ ] Migration runs successfully on clean database
- [ ] Migration is idempotent (can run multiple times)
- [ ] Rollback script works correctly
- [ ] Indexes created for foreign keys
- [ ] No hardcoded values (use placeholders)
- [ ] Comments added for tables and columns
- [ ] Version number incremented correctly
- [ ] File naming convention followed

## Troubleshooting

### Failed Migration

```bash
# Symptom: Migration failed, flyway_schema_history marked as failed
# Fix: Manually fix database, then repair
./gradlew flywayRepair

# Or rollback manually and re-run
psql -d smart_admin -f U1.1.0__Rollback_create_brand_table.sql
./gradlew flywayMigrate
```

### Checksum Mismatch

```bash
# Symptom: Migration changed after being applied
# Fix: Repair checksums (only if you know it's safe)
./gradlew flywayRepair

# Or create new migration instead of modifying
```

### Out-of-Order Migration

```yaml
# Allow out-of-order migrations (not recommended for production)
spring:
  flyway:
    out-of-order: true
```
