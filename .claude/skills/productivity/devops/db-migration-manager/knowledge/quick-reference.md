# DB Migration Manager - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: db-migration-manager (P2 - Productivity/DevOps)

---

## Command Quick Reference

| Command | Purpose | Duration |
|---------|---------|----------|
| Flyway Setup | Setup Flyway migration | ~8 min |
| Liquibase Setup | Setup Liquibase migration | ~10 min |
| Create Migration | Write migration script | ~10 min |
| Run Migration | Apply database changes | ~3 min |
| Rollback | Revert changes (Liquibase only) | ~5 min |

---

## Migration Tool Selection

### Option 1: Flyway (Recommended for SmartAdmin)

**Use When**: Simple SQL-based migrations

**Pros**:
- ✅ Simple and lightweight
- ✅ Pure SQL migrations
- ✅ Easy to understand
- ✅ Fast execution

**Cons**:
- ⚠️ No rollback support (manual)

**Setup**:
```gradle
dependencies {
    implementation 'org.flywaydb:flyway-core:9.22.0'
    runtimeOnly 'org.flywaydb:flyway-database-postgresql:9.22.0'
}
```

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    validate-on-migrate: true
```

**Migration File Naming**:
```
src/main/resources/db/migration/
├── V1__Initial_schema.sql
├── V2__Add_employee_table.sql
├── V3__Add_department_column.sql
└── V4__Create_index_employee_dept.sql
```

**Migration Script Example**:
```sql
-- V2__Add_employee_table.sql
CREATE TABLE t_employee (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(200) UNIQUE NOT NULL,
    dept_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_employee_dept FOREIGN KEY (dept_id) REFERENCES t_department(id)
);

CREATE INDEX idx_employee_dept ON t_employee(dept_id);
CREATE INDEX idx_employee_email ON t_employee(email);
```

**Time to Setup**: 8-10 minutes

---

### Option 2: Liquibase (Advanced Features)

**Use When**: Need rollback support or complex migrations

**Pros**:
- ✅ Rollback support
- ✅ XML/YAML/JSON formats
- ✅ Database-independent

**Cons**:
- ⚠️ More complex
- ⚠️ Slower than Flyway

**Setup**:
```gradle
dependencies {
    implementation 'org.liquibase:liquibase-core:4.23.1'
}
```

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

**Changelog Structure**:
```xml
<!-- db/changelog/db.changelog-master.xml -->
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.23.xsd">

    <include file="db/changelog/changes/001-create-employee-table.xml"/>
    <include file="db/changelog/changes/002-add-department-column.xml"/>
</databaseChangeLog>
```

**Changeset Example**:
```xml
<!-- 001-create-employee-table.xml -->
<databaseChangeLog>
    <changeSet id="1" author="admin">
        <createTable tableName="t_employee">
            <column name="id" type="BIGSERIAL">
                <constraints primaryKey="true"/>
            </column>
            <column name="name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="email" type="VARCHAR(200)">
                <constraints nullable="false" unique="true"/>
            </column>
        </createTable>

        <createIndex tableName="t_employee" indexName="idx_employee_email">
            <column name="email"/>
        </createIndex>

        <rollback>
            <dropTable tableName="t_employee"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

**Time to Setup**: 10-15 minutes

---

## Migration Patterns

### Pattern 1: Add Column (with Default Value)

**Flyway**:
```sql
-- V5__Add_status_column.sql
ALTER TABLE t_employee ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';
UPDATE t_employee SET status = 'ACTIVE' WHERE status IS NULL;
ALTER TABLE t_employee ALTER COLUMN status SET NOT NULL;
```

**Liquibase**:
```xml
<changeSet id="5" author="admin">
    <addColumn tableName="t_employee">
        <column name="status" type="VARCHAR(20)" defaultValue="ACTIVE">
            <constraints nullable="false"/>
        </column>
    </addColumn>

    <rollback>
        <dropColumn tableName="t_employee" columnName="status"/>
    </rollback>
</changeSet>
```

---

### Pattern 2: Rename Column (Safe Migration)

**Flyway (3-step process)**:
```sql
-- V6__Rename_column_step1.sql
-- Step 1: Add new column
ALTER TABLE t_employee ADD COLUMN full_name VARCHAR(100);
UPDATE t_employee SET full_name = name;

-- V7__Rename_column_step2.sql
-- Step 2: Deploy application supporting both columns
-- (Application reads from full_name, writes to both)

-- V8__Rename_column_step3.sql
-- Step 3: Drop old column (after verification)
ALTER TABLE t_employee DROP COLUMN name;
ALTER TABLE t_employee ALTER COLUMN full_name SET NOT NULL;
```

**Time to Migrate**: 3 deployments (zero downtime)

---

### Pattern 3: Data Migration

**Flyway**:
```sql
-- V9__Migrate_employee_data.sql
-- Migrate data from old to new structure
INSERT INTO t_employee_new (id, name, dept_id, created_at)
SELECT id, name, dept_id, created_at
FROM t_employee_old
WHERE deleted = FALSE;

-- Drop old table
DROP TABLE t_employee_old;
```

---

### Pattern 4: Index Creation (Non-blocking)

**PostgreSQL (Concurrent Index)**:
```sql
-- V10__Add_concurrent_index.sql
-- Create index without locking table
CREATE INDEX CONCURRENTLY idx_employee_created_at ON t_employee(created_at);
```

**Time to Execute**: Depends on table size (no downtime)

---

## Production Best Practices

### Practice 1: Version Naming Convention

```
V{major}_{minor}_{patch}__{description}.sql

Examples:
V1_0_0__Initial_schema.sql
V1_1_0__Add_employee_table.sql
V1_1_1__Fix_employee_email_constraint.sql
V2_0_0__Major_refactor.sql
```

---

### Practice 2: Always Backups Before Migration

```bash
#!/bin/bash
# pre-migration-backup.sh

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="backup_${TIMESTAMP}.sql"

# Backup database
pg_dump -h localhost -U postgres smartadmin > $BACKUP_FILE

# Run migration
./gradlew flywayMigrate

# Verify
./gradlew flywayInfo
```

---

### Practice 3: Rollback Strategy

**Flyway (Manual Rollback)**:
```sql
-- rollback/R__Undo_V5__Remove_status.sql
ALTER TABLE t_employee DROP COLUMN status;
```

**Liquibase (Built-in Rollback)**:
```bash
# Rollback last changeset
./gradlew liquibaseRollbackCount -PliquibaseCommandValue=1

# Rollback to specific tag
./gradlew liquibaseRollback -PliquibaseCommandValue=version-1.0
```

---

### Practice 4: CI/CD Integration

**GitHub Actions**:
```yaml
name: Database Migration

on:
  push:
    branches: [main]
    paths:
      - 'src/main/resources/db/migration/**'

jobs:
  migrate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run Flyway Migration
        run: ./gradlew flywayMigrate
        env:
          DB_URL: jdbc:postgresql://prod-db:5432/smartadmin
          DB_USER: ${{ secrets.DB_USER }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}

      - name: Verify Migration
        run: ./gradlew flywayInfo
```

---

## Troubleshooting

### Error 1: Checksum Mismatch

**Symptom**: `Validate failed: Migration checksum mismatch`

**Cause**: Migration file modified after execution

**Fix**:
```bash
# Repair Flyway metadata
./gradlew flywayRepair

# Or manually update flyway_schema_history
UPDATE flyway_schema_history
SET checksum = (calculated checksum)
WHERE version = 'V5';
```

---

### Error 2: Migration Fails Mid-Execution

**Symptom**: Database in inconsistent state

**Fix**:
```bash
# Mark migration as failed
./gradlew flywayRepair

# Fix SQL script
# Re-run migration
./gradlew flywayMigrate
```

---

## Time Estimates

| Task | Implementation | Testing | Total |
|------|---------------|---------|-------|
| Flyway Setup | 8 min | 3 min | 11 min |
| Liquibase Setup | 10 min | 5 min | 15 min |
| Simple Migration | 5 min | 3 min | 8 min |
| Complex Migration | 15 min | 10 min | 25 min |
| Rollback Script | 5 min | 5 min | 10 min |

**Full Migration System**: 30-40 minutes

---

**See Also**:
- [PostgreSQL Best Practices](../../integration/postgresql-best-practices/) - Database optimization
- [CI/CD Pipeline Builder](../cicd-pipeline-builder/) - Automate migrations
