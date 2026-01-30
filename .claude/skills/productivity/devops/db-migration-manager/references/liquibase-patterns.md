# Liquibase Migration Patterns

**Version**: 1.0.0
**Last Updated**: 2026-01-24
**Liquibase Version**: 4.x

## Overview

Liquibase is a database-agnostic migration tool that uses XML, YAML, or JSON formats to describe schema changes. This document covers essential patterns for SmartAdmin projects.

## Changeset Structure

### Basic Changeset

```xml
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="1.0.0-create-employee-table" author="developer">
        <createTable tableName="employee">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="email" type="VARCHAR(255)">
                <constraints nullable="false" unique="true"/>
            </column>
        </createTable>
    </changeSet>

</databaseChangeLog>
```

### Changeset Attributes

| Attribute | Description | Required | Example |
|-----------|-------------|----------|---------|
| `id` | Unique changeset identifier | Yes | "1.0.0-create-employee" |
| `author` | Developer name/email | Yes | "john.doe@example.com" |
| `context` | Execution context (dev, prod) | No | "dev,test" |
| `labels` | Tags for filtering | No | "v4.0.0,employee-module" |
| `runAlways` | Re-run on every update | No | "false" (default) |
| `runOnChange` | Re-run if changeset changes | No | "false" (default) |
| `failOnError` | Stop on error | No | "true" (default) |

## Format Options

### 1. XML Format (Most Common)

**Advantages**:
- Strong IDE support (autocomplete, validation)
- Extensive Liquibase feature support
- Clear structure with nested elements

**Example**:
```xml
<changeSet id="1.1.0-add-employee-department" author="developer">
    <addColumn tableName="employee">
        <column name="department_id" type="BIGINT">
            <constraints nullable="false"/>
        </column>
    </addColumn>

    <addForeignKeyConstraint
        baseTableName="employee"
        baseColumnNames="department_id"
        constraintName="fk_employee_department"
        referencedTableName="department"
        referencedColumnNames="id"/>
</changeSet>
```

### 2. YAML Format

**Advantages**:
- More concise than XML
- Easier to read for simple changes
- Better for version control diffs

**Example**:
```yaml
databaseChangeLog:
  - changeSet:
      id: 1.1.0-add-employee-department
      author: developer
      changes:
        - addColumn:
            tableName: employee
            columns:
              - column:
                  name: department_id
                  type: BIGINT
                  constraints:
                    nullable: false

        - addForeignKeyConstraint:
            baseTableName: employee
            baseColumnNames: department_id
            constraintName: fk_employee_department
            referencedTableName: department
            referencedColumnNames: id
```

### 3. JSON Format

**Advantages**:
- Machine-readable format
- Easy to generate programmatically
- Good for API-driven migrations

**Example**:
```json
{
  "databaseChangeLog": [
    {
      "changeSet": {
        "id": "1.1.0-add-employee-department",
        "author": "developer",
        "changes": [
          {
            "addColumn": {
              "tableName": "employee",
              "columns": [
                {
                  "column": {
                    "name": "department_id",
                    "type": "BIGINT",
                    "constraints": {
                      "nullable": false
                    }
                  }
                }
              ]
            }
          }
        ]
      }
    }
  ]
}
```

### 4. SQL Format

**Advantages**:
- Familiar syntax for DBAs
- Direct control over SQL statements
- Useful for complex database-specific operations

**Example**:
```sql
-- liquibase formatted sql

-- changeset developer:1.1.0-add-employee-department
ALTER TABLE employee ADD COLUMN department_id BIGINT NOT NULL;
ALTER TABLE employee ADD CONSTRAINT fk_employee_department
    FOREIGN KEY (department_id) REFERENCES department(id);

-- rollback ALTER TABLE employee DROP CONSTRAINT fk_employee_department;
-- rollback ALTER TABLE employee DROP COLUMN department_id;
```

## Preconditions

Preconditions allow conditional execution based on database state.

### Common Preconditions

**1. Table Exists**
```xml
<changeSet id="1.2.0-add-employee-column" author="developer">
    <preConditions onFail="MARK_RAN">
        <tableExists tableName="employee"/>
    </preConditions>

    <addColumn tableName="employee">
        <column name="phone" type="VARCHAR(20)"/>
    </addColumn>
</changeSet>
```

**2. Table Does Not Exist**
```xml
<changeSet id="1.0.0-create-employee" author="developer">
    <preConditions onFail="MARK_RAN">
        <not>
            <tableExists tableName="employee"/>
        </not>
    </preConditions>

    <createTable tableName="employee">
        <!-- columns -->
    </createTable>
</changeSet>
```

**3. Column Exists**
```xml
<changeSet id="1.3.0-modify-employee-email" author="developer">
    <preConditions onFail="HALT">
        <columnExists tableName="employee" columnName="email"/>
    </preConditions>

    <modifyDataType tableName="employee" columnName="email" newDataType="VARCHAR(255)"/>
</changeSet>
```

**4. Index Exists**
```xml
<changeSet id="1.4.0-recreate-employee-index" author="developer">
    <preConditions onFail="MARK_RAN">
        <indexExists tableName="employee" indexName="idx_employee_email"/>
    </preConditions>

    <dropIndex tableName="employee" indexName="idx_employee_email"/>
    <createIndex tableName="employee" indexName="idx_employee_email" unique="true">
        <column name="email"/>
    </createIndex>
</changeSet>
```

**5. Database Type**
```xml
<changeSet id="1.5.0-postgres-specific" author="developer">
    <preConditions onFail="MARK_RAN">
        <dbms type="postgresql"/>
    </preConditions>

    <sql>
        CREATE INDEX CONCURRENTLY idx_employee_name ON employee(name);
    </sql>
</changeSet>
```

**6. Custom SQL Check**
```xml
<changeSet id="1.6.0-migrate-only-if-data-exists" author="developer">
    <preConditions onFail="MARK_RAN">
        <sqlCheck expectedResult="1">
            SELECT COUNT(*) > 0 FROM legacy_employees
        </sqlCheck>
    </preConditions>

    <sql>
        INSERT INTO employee (name, email) SELECT name, email FROM legacy_employees;
    </sql>
</changeSet>
```

### Precondition Actions

| Action | Description | Use Case |
|--------|-------------|----------|
| `HALT` | Stop execution, fail deployment | Critical requirement |
| `MARK_RAN` | Skip changeset, mark as executed | Optional migration |
| `WARN` | Log warning, continue execution | Non-critical check |
| `CONTINUE` | Ignore precondition failure | Rare, not recommended |

## Rollback Strategies

### 1. Automatic Rollback

Liquibase automatically generates rollback for many change types:

```xml
<changeSet id="1.7.0-add-employee-salary" author="developer">
    <addColumn tableName="employee">
        <column name="salary" type="DECIMAL(10,2)"/>
    </addColumn>
    <!-- Automatic rollback: DROP COLUMN salary -->
</changeSet>
```

**Auto-rollback supported for**:
- `createTable` → `dropTable`
- `addColumn` → `dropColumn`
- `createIndex` → `dropIndex`
- `addForeignKeyConstraint` → `dropForeignKeyConstraint`

### 2. Manual Rollback (XML)

```xml
<changeSet id="1.8.0-complex-data-migration" author="developer">
    <sql>
        UPDATE employee SET status = 'ACTIVE' WHERE hire_date >= CURRENT_DATE - INTERVAL '1 year';
    </sql>

    <rollback>
        <sql>
            UPDATE employee SET status = NULL WHERE hire_date >= CURRENT_DATE - INTERVAL '1 year';
        </sql>
    </rollback>
</changeSet>
```

### 3. Rollback Using Change Tags

```xml
<changeSet id="1.9.0-add-employee-bonus" author="developer">
    <addColumn tableName="employee">
        <column name="bonus" type="DECIMAL(10,2)" defaultValue="0"/>
    </addColumn>

    <rollback>
        <dropColumn tableName="employee" columnName="bonus"/>
    </rollback>
</changeSet>
```

### 4. Empty Rollback (Irreversible)

```xml
<changeSet id="1.10.0-archive-old-employees" author="developer">
    <sql>
        INSERT INTO employee_archive SELECT * FROM employee WHERE deleted = TRUE;
        DELETE FROM employee WHERE deleted = TRUE;
    </sql>

    <rollback>
        <!-- Manual restoration required -->
        <empty/>
    </rollback>
</changeSet>
```

### 5. Rollback to Tag

```xml
<!-- Tag a stable state -->
<changeSet id="tag-v4.0.0" author="developer">
    <tagDatabase tag="v4.0.0"/>
</changeSet>

<!-- Later rollback to tag -->
<!-- liquibase rollback v4.0.0 -->
```

## Context and Labels

### Context-Based Execution

**Define contexts in changesets**:
```xml
<changeSet id="1.11.0-dev-seed-data" author="developer" context="dev">
    <insert tableName="employee">
        <column name="name" value="Test User"/>
        <column name="email" value="test@example.com"/>
    </insert>
</changeSet>

<changeSet id="1.12.0-prod-only-index" author="developer" context="prod">
    <createIndex tableName="employee" indexName="idx_employee_performance">
        <column name="department_id"/>
        <column name="hire_date"/>
    </createIndex>
</changeSet>
```

**Execute with context**:
```bash
# Development
liquibase update --contexts=dev

# Production
liquibase update --contexts=prod

# Multiple contexts
liquibase update --contexts="dev,test"
```

### Label-Based Filtering

**Define labels in changesets**:
```xml
<changeSet id="1.13.0-employee-module" author="developer" labels="v4.0.0,employee">
    <createTable tableName="employee_performance">
        <!-- columns -->
    </createTable>
</changeSet>
```

**Execute with labels**:
```bash
# Deploy only v4.0.0 changes
liquibase update --labels=v4.0.0

# Deploy employee module changes
liquibase update --labels=employee
```

## Include Files

Organize changesets across multiple files for better maintainability.

### Master Changelog

**db/changelog/db.changelog-master.xml**:
```xml
<databaseChangeLog>
    <!-- Include files in order -->
    <include file="db/changelog/v1.0.0/baseline.xml"/>
    <include file="db/changelog/v1.1.0/employee-module.xml"/>
    <include file="db/changelog/v1.2.0/department-module.xml"/>

    <!-- Include directory (all files in order) -->
    <includeAll path="db/changelog/v1.3.0/"/>
</databaseChangeLog>
```

### Module-Specific Changelog

**db/changelog/v1.1.0/employee-module.xml**:
```xml
<databaseChangeLog>
    <changeSet id="1.1.0-create-employee" author="developer">
        <createTable tableName="employee">
            <!-- columns -->
        </createTable>
    </changeSet>

    <changeSet id="1.1.1-create-employee-indexes" author="developer">
        <createIndex tableName="employee" indexName="idx_employee_email">
            <column name="email"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

## Change Types Reference

### Table Operations

**Create Table**:
```xml
<createTable tableName="employee" remarks="Employee master data">
    <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
    </column>
    <column name="name" type="VARCHAR(100)" remarks="Employee full name">
        <constraints nullable="false"/>
    </column>
</createTable>
```

**Drop Table**:
```xml
<dropTable tableName="employee" cascadeConstraints="true"/>
```

**Rename Table**:
```xml
<renameTable oldTableName="employee" newTableName="employees"/>
```

### Column Operations

**Add Column**:
```xml
<addColumn tableName="employee">
    <column name="phone" type="VARCHAR(20)" remarks="Contact phone number"/>
    <column name="address" type="TEXT"/>
</addColumn>
```

**Drop Column**:
```xml
<dropColumn tableName="employee" columnName="phone"/>
```

**Rename Column**:
```xml
<renameColumn tableName="employee"
              oldColumnName="phone"
              newColumnName="contact_phone"
              columnDataType="VARCHAR(20)"/>
```

**Modify Data Type**:
```xml
<modifyDataType tableName="employee"
                columnName="email"
                newDataType="VARCHAR(255)"/>
```

**Add/Drop Not Null**:
```xml
<addNotNullConstraint tableName="employee"
                      columnName="email"
                      columnDataType="VARCHAR(255)"
                      defaultNullValue="unknown@example.com"/>

<dropNotNullConstraint tableName="employee"
                       columnName="phone"
                       columnDataType="VARCHAR(20)"/>
```

### Index Operations

**Create Index**:
```xml
<createIndex tableName="employee" indexName="idx_employee_email" unique="true">
    <column name="email"/>
</createIndex>

<!-- Composite index -->
<createIndex tableName="employee" indexName="idx_employee_dept_hire">
    <column name="department_id"/>
    <column name="hire_date"/>
</createIndex>
```

**Drop Index**:
```xml
<dropIndex tableName="employee" indexName="idx_employee_email"/>
```

### Constraint Operations

**Add Primary Key**:
```xml
<addPrimaryKey tableName="employee"
               columnNames="id"
               constraintName="pk_employee"/>
```

**Add Foreign Key**:
```xml
<addForeignKeyConstraint
    baseTableName="employee"
    baseColumnNames="department_id"
    constraintName="fk_employee_department"
    referencedTableName="department"
    referencedColumnNames="id"
    onDelete="RESTRICT"
    onUpdate="CASCADE"/>
```

**Add Unique Constraint**:
```xml
<addUniqueConstraint tableName="employee"
                     columnNames="email"
                     constraintName="uk_employee_email"/>
```

**Add Check Constraint**:
```xml
<sql dbms="postgresql,mysql">
    ALTER TABLE employee ADD CONSTRAINT chk_employee_salary CHECK (salary >= 0);
</sql>
```

**Drop Constraint**:
```xml
<dropForeignKeyConstraint baseTableName="employee"
                          constraintName="fk_employee_department"/>

<dropUniqueConstraint tableName="employee"
                      constraintName="uk_employee_email"/>
```

### Data Operations

**Insert Data**:
```xml
<insert tableName="employee">
    <column name="id" valueNumeric="1"/>
    <column name="name" value="John Doe"/>
    <column name="email" value="john@example.com"/>
    <column name="hire_date" valueDate="2024-01-15"/>
</insert>
```

**Update Data**:
```xml
<update tableName="employee">
    <column name="status" value="ACTIVE"/>
    <where>hire_date >= '2024-01-01'</where>
</update>
```

**Delete Data**:
```xml
<delete tableName="employee">
    <where>deleted = TRUE AND updated_at < CURRENT_DATE - INTERVAL '1 year'</where>
</delete>
```

**Load Data from CSV**:
```xml
<loadData tableName="employee"
          file="db/data/employees.csv"
          separator=","
          quotchar="&quot;">
    <column name="id" type="NUMERIC"/>
    <column name="name" type="STRING"/>
    <column name="hire_date" type="DATE"/>
</loadData>
```

### SQL Operations

**Raw SQL**:
```xml
<sql dbms="postgresql">
    CREATE INDEX CONCURRENTLY idx_employee_name ON employee(name);
</sql>

<sql dbms="mysql">
    CREATE INDEX idx_employee_name ON employee(name);
</sql>
```

**SQL File**:
```xml
<sqlFile path="db/scripts/complex_migration.sql"
         dbms="postgresql"
         splitStatements="true"
         stripComments="true"/>
```

## Custom Change Classes (Java)

For complex migrations, create custom change classes.

### Java Custom Change

```java
package net.lab1024.sa.base.migration;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class EmployeeDataMigration implements CustomTaskChange {

    @Override
    public void execute(Database database) throws CustomChangeException {
        // Custom migration logic
        try (Connection conn = database.getConnection()) {
            // Batch process employees
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE employee SET status = ? WHERE hire_date >= ?"
            );
            ps.setString(1, "ACTIVE");
            ps.setDate(2, Date.valueOf("2024-01-01"));
            ps.executeUpdate();
        } catch (Exception e) {
            throw new CustomChangeException("Migration failed", e);
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Employee data migration completed successfully";
    }

    @Override
    public void setUp() throws SetupException {
        // Initialize resources
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // Set resource accessor
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
```

### Use Custom Change in Changelog

```xml
<changeSet id="1.14.0-custom-employee-migration" author="developer">
    <customChange class="net.lab1024.sa.base.migration.EmployeeDataMigration"/>
</changeSet>
```

## Best Practices

### DO

1. **Use semantic changeset IDs**
   - Format: `{version}-{description}` (e.g., "1.1.0-add-employee-table")
   - Makes history readable

2. **Leverage preconditions**
   - Prevent duplicate changes
   - Validate database state before migration

3. **Always provide rollback**
   - Manual rollback for complex changes
   - Document irreversible migrations

4. **Organize with include files**
   - One file per feature/module
   - Master changelog includes all

5. **Use contexts for environments**
   - `dev` for development seed data
   - `prod` for production-only optimizations

6. **Add comments and remarks**
   - Document table/column purpose
   - Explain complex migrations

7. **Test migrations thoroughly**
   - Test forward migration
   - Test rollback
   - Validate on production-like data

### DON'T

1. **Don't modify executed changesets**
   - Checksum will fail
   - Create new changeset instead

2. **Don't use runAlways carelessly**
   - Can cause data loss
   - Use only for views/procedures

3. **Don't skip preconditions**
   - Prevent duplicate table/column errors
   - Validate dependencies

4. **Don't mix schema and data changes**
   - Separate changesets for clarity
   - Easier to rollback

5. **Don't ignore database differences**
   - Use `dbms` attribute for database-specific SQL
   - Test on all target databases

## Integration with SmartAdmin

### Gradle Configuration

```gradle
// build.gradle
plugins {
    id 'org.liquibase.gradle' version '2.2.0'
}

dependencies {
    liquibaseRuntime 'org.liquibase:liquibase-core:4.20.0'
    liquibaseRuntime 'org.postgresql:postgresql:42.7.3'
    liquibaseRuntime 'org.yaml:snakeyaml:2.0'
}

liquibase {
    activities {
        main {
            changelogFile 'src/main/resources/db/changelog/db.changelog-master.xml'
            url project.findProperty('liquibase.url') ?: 'jdbc:postgresql://localhost:5432/smartadmin_dev'
            username project.findProperty('liquibase.username') ?: 'smartadmin'
            password project.findProperty('liquibase.password') ?: 'password'
            contexts 'dev'
        }
    }
}
```

### Spring Boot Configuration

```yaml
# application.yml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
    contexts: ${spring.profiles.active}
    default-schema: public
    liquibase-schema: public
    drop-first: false
```

### Project Structure

```
smart-admin-api-java21-springboot3/
└── src/main/resources/
    └── db/
        ├── changelog/
        │   ├── db.changelog-master.xml
        │   ├── v1.0.0/
        │   │   └── baseline.xml
        │   ├── v1.1.0/
        │   │   ├── employee-module.xml
        │   │   └── employee-indexes.xml
        │   └── v1.2.0/
        │       └── department-module.xml
        └── data/
            └── seed/
                ├── employees.csv
                └── departments.csv
```

## Additional Resources

- [Liquibase Documentation](https://docs.liquibase.com/)
- [Liquibase Change Types](https://docs.liquibase.com/change-types/home.html)
- [Liquibase Best Practices](https://www.liquibase.org/get-started/best-practices)
- [Flyway vs Liquibase Comparison](../SKILL.md#flyway-vs-liquibase-comparison)
