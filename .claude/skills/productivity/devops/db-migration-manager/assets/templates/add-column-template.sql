-- V{version}__Add_{column_name}_to_{table_name}.sql
-- Purpose: {description}
-- Author: {author}
-- Date: {date}

-- Add column
ALTER TABLE t_{table_name} ADD COLUMN {column_name} {column_type} {constraints};

-- Add index (if needed for search/join)
CREATE INDEX idx_{table_name}_{column_name} ON t_{table_name}({column_name});

-- Add comment (PostgreSQL)
COMMENT ON COLUMN t_{table_name}.{column_name} IS '{column_description}';

-- Rollback script: U{version}__Rollback_add_{column_name}_to_{table_name}.sql
-- DROP INDEX IF EXISTS idx_{table_name}_{column_name};
-- ALTER TABLE t_{table_name} DROP COLUMN {column_name};
