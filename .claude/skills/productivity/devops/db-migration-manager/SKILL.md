---
name: db-migration-manager
description: [P2 - Productivity] Generate and manage Flyway/Liquibase database migrations for SmartAdmin. Use when creating schema changes (new tables, columns, indexes), modifying existing tables, handling data migrations, or managing database versioning. Automatically triggered when user mentions "database migration", "Flyway", "Liquibase", "schema change", "add column", "create table", or "database versioning".
---

# Database Migration Manager

Generate and manage database migrations with Flyway or Liquibase for SmartAdmin projects, supporting both PostgreSQL and MySQL.

## Quick Start

**Most common usage:**
```
User: "Create a migration to add a brand table"
User: "Add email column to employee table"
User: "Generate Flyway migration for the new department structure"
User: "Create rollback script for the last migration"
```

You will:
1. Analyze schema change requirements
2. Generate versioned migration script (Flyway SQL or Liquibase XML)
3. Create rollback script
4. Validate migration syntax
5. Test migration in local environment

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "database migration" - Database schema migration
- "Flyway" - Flyway migration tool
- "Liquibase" - Liquibase migration tool
- "migration script" - Migration script generation

**Secondary Keywords** (Medium confidence):
- "schema change" - Context: database schema modifications
- "rollback script" - Context: migration rollback generation
- "versioned migration" - Context: Flyway/Liquibase versioning
- "add column" / "add table" - Context: schema alteration

**Phrase Patterns**:
- "Create migration to [action]" - Example: "Create migration to add brand table"
- "Add [column/table] to [table]" - Example: "Add email column to employee table"
- "Generate [tool] migration for [change]" - Example: "Generate Flyway migration for department structure"

**Example User Requests**:
```
User: "Create a migration to add a brand table"
User: "Add email column to employee table"
User: "Generate Flyway migration for the new department structure"
User: "Create rollback script for the last migration"
```

**Note**: This skill can also be manually invoked via `/db-migration-manager` command.

## Core Capabilities

### 1. Flyway Migration Generation

**Naming Convention:**
```
V{version}__{description}.sql
V1.0.0__Create_employee_table.sql
V1.0.1__Add_email_to_employee.sql
V1.1.0__Create_brand_table.sql
```

**Create Table Migration:**
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
COMMENT ON COLUMN t_brand.brand_name IS 'Brand name (max 50 chars, unique)';
COMMENT ON COLUMN t_brand.status IS 'Status (1=Enabled, 0=Disabled)';
```

**Add Column Migration:**
```sql
-- V1.1.1__Add_email_to_employee.sql
ALTER TABLE t_employee ADD COLUMN email VARCHAR(100);

-- Add index for search
CREATE INDEX idx_employee_email ON t_employee(email);

-- Add comment
COMMENT ON COLUMN t_employee.email IS 'Employee email address (optional)';
```

**Rollback Script:**
```sql
-- U1.1.1__Rollback_add_email_to_employee.sql
DROP INDEX IF EXISTS idx_employee_email;
ALTER TABLE t_employee DROP COLUMN email;
```

---

### 2. Liquibase ChangeSet Generation

**XML Format:**
```xml
<!-- db/changelog/db.changelog-1.1.0.xml -->
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="1.1.0-create-brand-table" author="developer">
        <createTable tableName="t_brand">
            <column name="brand_id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="brand_name" type="VARCHAR(50)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="brand_logo" type="VARCHAR(200)"/>
            <column name="description" type="VARCHAR(500)"/>
            <column name="sort" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="INT" defaultValueNumeric="1">
                <constraints nullable="false"/>
            </column>
            <column name="deleted_flag" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="update_time" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="create_time" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex indexName="idx_brand_name" tableName="t_brand">
            <column name="brand_name"/>
        </createIndex>

        <createIndex indexName="idx_brand_status_deleted" tableName="t_brand">
            <column name="status"/>
            <column name="deleted_flag"/>
        </createIndex>

        <rollback>
            <dropTable tableName="t_brand"/>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

**YAML Format (Alternative):**
```yaml
# db/changelog/db.changelog-1.1.0.yaml
databaseChangeLog:
  - changeSet:
      id: 1.1.0-create-brand-table
      author: developer
      changes:
        - createTable:
            tableName: t_brand
            columns:
              - column:
                  name: brand_id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: brand_name
                  type: VARCHAR(50)
                  constraints:
                    nullable: false
                    unique: true
              - column:
                  name: brand_logo
                  type: VARCHAR(200)
              - column:
                  name: status
                  type: INT
                  defaultValueNumeric: 1
                  constraints:
                    nullable: false
        - createIndex:
            indexName: idx_brand_name
            tableName: t_brand
            columns:
              - column:
                  name: brand_name
      rollback:
        - dropTable:
            tableName: t_brand
```

---

### 3. Data Migration Patterns

**Insert Reference Data:**
```sql
-- V1.1.2__Insert_brand_initial_data.sql
INSERT INTO t_brand (brand_name, brand_logo, description, sort, status) VALUES
('Nike', 'https://example.com/nike.png', 'Sports brand', 1, 1),
('Adidas', 'https://example.com/adidas.png', 'Sports brand', 2, 1),
('Puma', 'https://example.com/puma.png', 'Sports brand', 3, 1);
```

**Update Existing Data:**
```sql
-- V1.1.3__Update_employee_department.sql
-- Migrate employees from old department to new department
UPDATE t_employee 
SET department_id = 10
WHERE department_id = 5;
```

**Complex Data Migration:**
```sql
-- V1.1.4__Migrate_user_roles.sql
-- Create new role_employee records from old user_role table
INSERT INTO t_role_employee (employee_id, role_id, create_time)
SELECT u.employee_id, u.role_id, NOW()
FROM t_user_role u
WHERE NOT EXISTS (
    SELECT 1 FROM t_role_employee r
    WHERE r.employee_id = u.employee_id
      AND r.role_id = u.role_id
);
```

---

### 4. Schema Modification Patterns

**Add Foreign Key:**
```sql
-- V1.1.5__Add_brand_fk_to_goods.sql
ALTER TABLE t_goods 
ADD COLUMN brand_id BIGINT;

ALTER TABLE t_goods
ADD CONSTRAINT fk_goods_brand
FOREIGN KEY (brand_id) REFERENCES t_brand(brand_id);

CREATE INDEX idx_goods_brand_id ON t_goods(brand_id);
```

**Modify Column Type:**
```sql
-- V1.1.6__Change_employee_phone_length.sql
-- PostgreSQL
ALTER TABLE t_employee 
ALTER COLUMN phone TYPE VARCHAR(20);

-- MySQL
ALTER TABLE t_employee 
MODIFY COLUMN phone VARCHAR(20);
```

**Add Unique Constraint:**
```sql
-- V1.1.7__Add_unique_constraint_employee_login.sql
-- Create unique index (allows null values)
CREATE UNIQUE INDEX idx_employee_login_unique 
ON t_employee(login_name) 
WHERE deleted_flag = FALSE;

-- Or add constraint (depends on DB)
ALTER TABLE t_employee
ADD CONSTRAINT uk_employee_login_name UNIQUE (login_name);
```

---

### 5. Migration Validation

**Flyway Configuration (application.yml):**
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    out-of-order: false
    placeholder-replacement: true
    placeholders:
      schema: smart_admin
```

**Run Migration:**
```bash
# Gradle
./gradlew flywayMigrate

# Maven
./mvnw flyway:migrate

# Flyway CLI
flyway migrate -url=jdbc:postgresql://localhost:5432/smart_admin -user=admin -password=secret
```

**Validate Migration:**
```bash
# Check migration status
./gradlew flywayInfo

# Validate checksums
./gradlew flywayValidate

# Repair failed migration
./gradlew flywayRepair
```

---

### 6. Rollback Strategies

**Flyway Rollback (Teams Edition):**
```bash
# Undo last migration
./gradlew flywayUndo

# Undo to specific version
./gradlew flywayUndo -Dflyway.target=1.0.5
```

**Manual Rollback (Community Edition):**
```sql
-- Create rollback script manually
-- U1.1.0__Rollback_create_brand_table.sql
DROP TABLE IF EXISTS t_brand;
```

**Liquibase Rollback:**
```bash
# Rollback last changeSet
liquibase rollback-count 1

# Rollback to specific tag
liquibase rollback TAG_1.0.0

# Rollback to date
liquibase rollback-to-date 2024-01-01
```

---

## Migration Workflow

### Step 1: Analyze Schema Change

**Identify:**
- Table(s) affected
- Column types and constraints
- Indexes needed
- Foreign keys
- Default values
- Data migration requirements

### Step 2: Generate Migration Script

**Determine version number:**
```
Current: V1.0.9
Next:    V1.1.0 (minor feature)
Or:      V1.0.10 (patch fix)
```

**Create migration file:**
```
db/migration/V1.1.0__Create_brand_table.sql
```

### Step 3: Write Migration SQL

**Follow SmartAdmin table standards:**
- Primary key: `{table}_id BIGSERIAL`
- Deleted flag: `deleted_flag BOOLEAN NOT NULL DEFAULT FALSE`
- Timestamps: `update_time`, `create_time` with `DEFAULT CURRENT_TIMESTAMP`
- Status field: `status INT NOT NULL DEFAULT 1`

### Step 4: Create Rollback Script

**Always provide rollback:**
```sql
-- Rollback for CREATE TABLE
DROP TABLE IF EXISTS t_brand;

-- Rollback for ADD COLUMN
ALTER TABLE t_employee DROP COLUMN email;

-- Rollback for INSERT
DELETE FROM t_brand WHERE brand_id IN (1, 2, 3);
```

### Step 5: Test Migration

**Local testing:**
```bash
# Apply migration
./gradlew flywayMigrate

# Verify schema
psql -d smart_admin -c "\d t_brand"

# Test rollback
./gradlew flywayUndo  # or run rollback script manually
```

### Step 6: Document Migration

**Add migration notes:**
```sql
-- V1.1.0__Create_brand_table.sql
-- Purpose: Add brand management feature
-- JIRA: SMART-123
-- Author: developer
-- Date: 2024-01-15

CREATE TABLE t_brand (
    ...
);
```

---

## Database-Specific Patterns

### PostgreSQL Patterns

**Serial vs. Identity:**
```sql
-- Old style (SERIAL)
brand_id BIGSERIAL PRIMARY KEY

-- New style (IDENTITY - PostgreSQL 10+)
brand_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
```

**UUID Primary Key:**
```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE t_session (
    session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ...
);
```

**Full-Text Search:**
```sql
-- Add tsvector column
ALTER TABLE t_employee ADD COLUMN search_vector tsvector;

-- Create GIN index
CREATE INDEX idx_employee_search ON t_employee USING GIN(search_vector);

-- Update search vector
UPDATE t_employee SET search_vector = 
    to_tsvector('english', actual_name || ' ' || COALESCE(email, ''));
```

### MySQL Patterns

**Auto-Increment:**
```sql
CREATE TABLE t_brand (
    brand_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**JSON Column:**
```sql
ALTER TABLE t_config ADD COLUMN metadata JSON;

-- Index JSON field (MySQL 8.0+)
ALTER TABLE t_config ADD INDEX idx_config_metadata_type ((CAST(metadata->>'$.type' AS CHAR(50))));
```

---

## Best Practices

### 1. Version Numbering

**Semantic versioning:**
- `V1.0.0` - Major version (breaking changes)
- `V1.1.0` - Minor version (new features)
- `V1.1.1` - Patch version (bug fixes)

### 2. Migration Safety

**Checklist:**
- [ ] Migrations are idempotent (can run multiple times)
- [ ] No destructive operations without backup
- [ ] Foreign keys added after data population
- [ ] Indexes created for new columns used in WHERE/JOIN
- [ ] Default values provided for NOT NULL columns
- [ ] Rollback script tested

### 3. Performance Considerations

**For large tables:**
```sql
-- BAD: Locks table during ALTER
ALTER TABLE t_employee ADD COLUMN email VARCHAR(100);

-- GOOD: Add column with null, then populate and add constraint
ALTER TABLE t_employee ADD COLUMN email VARCHAR(100);
-- Populate in batches
UPDATE t_employee SET email = CONCAT(login_name, '@example.com') WHERE email IS NULL LIMIT 1000;
-- Add constraint after population
ALTER TABLE t_employee ALTER COLUMN email SET NOT NULL;
```

### 4. Zero-Downtime Migrations

**Pattern:**
1. Add new column (nullable)
2. Dual-write to old and new columns
3. Backfill data
4. Switch reads to new column
5. Remove old column

**Example:**
```sql
-- Step 1: Add new column
ALTER TABLE t_employee ADD COLUMN email_new VARCHAR(100);

-- Step 2-3: Application writes to both, backfill data
-- (Done in application code over time)

-- Step 4: Swap columns
ALTER TABLE t_employee RENAME COLUMN email TO email_old;
ALTER TABLE t_employee RENAME COLUMN email_new TO email;

-- Step 5: Drop old column (after verifying)
ALTER TABLE t_employee DROP COLUMN email_old;
```

---

## Validation Checklist

**Before Committing Migration:**
- [ ] Version number follows semantic versioning
- [ ] Migration script is idempotent
- [ ] Rollback script provided and tested
- [ ] Indexes created for foreign keys
- [ ] Comments added for tables and columns
- [ ] Migration tested locally
- [ ] No hardcoded values (use placeholders)
- [ ] Database-specific syntax avoided (or versioned separately)

---

## References

Detailed migration guides:
- [references/flyway-patterns.md](references/flyway-patterns.md) - Flyway best practices and patterns
- [references/liquibase-patterns.md](references/liquibase-patterns.md) - Liquibase changeset patterns
- [references/zero-downtime-migrations.md](references/zero-downtime-migrations.md) - Safe production migrations

## Time Savings

**Manual Migration Creation:** 30-60 minutes per migration
**Skill-Guided Migration:** 10-15 minutes
**Time Saved: 60-75% reduction**

**Quality Improvements:**
- ✅ Consistent versioning
- ✅ Automatic rollback scripts
- ✅ SmartAdmin table standards enforced
- ✅ Database-specific optimizations
