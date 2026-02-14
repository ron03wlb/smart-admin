---
name: db-migration-manager
description: "資料庫遷移腳本生成與管理"
priority: P2
category: devops
---

# Database Migration Manager

為 SmartAdmin 專案生成和管理 Flyway/Liquibase 資料庫遷移腳本，支援 PostgreSQL 和 MySQL。

## Usage

```
User: "Create a migration to add a brand table"
AI: [Generate versioned migration script with rollback]
```

## When to Use

- 創建新資料表
- 修改現有資料表結構
- 添加/修改索引
- 資料遷移腳本
- 資料庫版本控管

## Generated Output

- Flyway SQL 遷移腳本 (V{version}__{description}.sql)
- Liquibase XML 變更集
- 回滾腳本
- 資料遷移腳本

## Workflow

1. **分析變更需求**
   - 確認資料表名稱和欄位
   - 檢查命名規範 (t_ 前綴, 單數形式)

2. **生成版本號**
   - 使用語義化版本 (V1.0.0, V1.0.1)
   - 確保版本順序正確

3. **創建遷移腳本**
   - CREATE TABLE/ALTER TABLE 語句
   - 索引創建 (idx_ 前綴)
   - 欄位註解 (COMMENT ON)

4. **生成回滾腳本**
   - DROP TABLE/ALTER TABLE DROP COLUMN
   - 回滾驗證步驟

5. **驗證遷移**
   - 語法檢查
   - 本地環境測試

## Related Rules

- [D01-postgresql-basics.md](../../../rules/technology/database/D01-postgresql-basics.md)
- [D02-postgresql-advanced.md](../../../rules/technology/database/D02-postgresql-advanced.md)
- [F01-naming-conventions.md](../../../rules/foundation/F01-naming-conventions.md)

## Example Session

**User:** 創建 brand 品牌資料表的遷移腳本

**AI Agent Actions:**
1. 確認表名: `t_brand` (遵循 SmartAdmin 命名規範)
2. 生成 `V1.1.0__Create_brand_table.sql`
3. 包含欄位: brand_id, brand_name, brand_logo, status, deleted_flag
4. 創建索引: `idx_brand_name`, `idx_brand_status_deleted`
5. 添加欄位註解
6. 生成回滾腳本 `R__Drop_brand_table.sql`
