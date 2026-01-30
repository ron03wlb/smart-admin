-- V{version}__Add_{fk_name}_fk_to_{table_name}.sql
-- Purpose: {description}
-- Author: {author}
-- Date: {date}

-- Step 1: Add column
ALTER TABLE t_{table_name} ADD COLUMN {fk_column} BIGINT;

-- Step 2: Populate with default or existing values
-- UPDATE t_{table_name} SET {fk_column} = {default_value} WHERE {fk_column} IS NULL;

-- Step 3: Add foreign key constraint
ALTER TABLE t_{table_name}
ADD CONSTRAINT fk_{table_name}_{ref_table}
FOREIGN KEY ({fk_column}) REFERENCES t_{ref_table}({ref_column});

-- Step 4: Add index for FK (improves JOIN performance)
CREATE INDEX idx_{table_name}_{fk_column} ON t_{table_name}({fk_column});

-- Rollback script: U{version}__Rollback_add_{fk_name}_fk_to_{table_name}.sql
-- ALTER TABLE t_{table_name} DROP CONSTRAINT IF EXISTS fk_{table_name}_{ref_table};
-- DROP INDEX IF EXISTS idx_{table_name}_{fk_column};
-- ALTER TABLE t_{table_name} DROP COLUMN {fk_column};
