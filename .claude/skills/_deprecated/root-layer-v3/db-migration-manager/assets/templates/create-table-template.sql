-- V{version}__Create_{table_name}_table.sql
-- Purpose: {description}
-- Author: {author}
-- Date: {date}

CREATE TABLE t_{table_name} (
    {table_name}_id BIGSERIAL PRIMARY KEY,
    
    -- Business columns
    {column_name} {column_type} {constraints},
    
    -- Standard columns (required for SmartAdmin)
    status INT NOT NULL DEFAULT 1,
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_{table_name}_{column} ON t_{table_name}({column});
CREATE INDEX idx_{table_name}_status_deleted ON t_{table_name}(status, deleted_flag);

-- Comments (PostgreSQL)
COMMENT ON TABLE t_{table_name} IS '{table_description}';
COMMENT ON COLUMN t_{table_name}.{column_name} IS '{column_description}';

-- Rollback script: U{version}__Rollback_create_{table_name}_table.sql
-- DROP TABLE IF EXISTS t_{table_name};
