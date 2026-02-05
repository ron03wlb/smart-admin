# PostgreSQL Best Practices - Quick Reference

## HikariCP 配置公式

```
最佳連線數 = (核心數 × 2) + 有效磁碟數

範例 (4 核心, SSD):
connections = (4 × 2) + 1 = 9
```

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 10
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-timeout: 30000
```

## N+1 查詢檢測

**P6Spy 日誌分析**:
```sql
-- 識別重複查詢模式
SELECT * FROM t_department WHERE department_id = ?  -- 重複 N 次
```

**解決方案**:
```java
// 使用 JOIN 或 IN 查詢
@Select("SELECT e.*, d.department_name FROM t_employee e " +
        "LEFT JOIN t_department d ON e.department_id = d.department_id " +
        "WHERE e.status = 1")
List<EmployeeVO> selectWithDepartment();
```

## EXPLAIN ANALYZE 關鍵指標

| 指標 | 好 | 差 | 說明 |
|------|----|----|------|
| Seq Scan | 小表 OK | 大表避免 | 全表掃描 |
| Index Scan | ✅ 推薦 | - | 索引掃描 |
| Bitmap Heap Scan | 部分行 | 大量行 | 位圖掃描 |
| Sort | Memory | Disk | 排序方式 |

```sql
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT * FROM t_employee WHERE department_id = 1;
```

## 索引建議查詢

```sql
-- 識別缺少索引的表
SELECT
    schemaname,
    relname AS table_name,
    seq_scan,
    idx_scan,
    n_tup_ins + n_tup_upd + n_tup_del AS write_ops,
    CASE WHEN idx_scan = 0 THEN 'Missing Index?' ELSE 'OK' END AS status
FROM pg_stat_user_tables
WHERE seq_scan > 1000
ORDER BY seq_scan DESC;
```

## 常用索引類型

| 類型 | 用途 | 語法 |
|------|------|------|
| B-tree | 等值、範圍查詢 | `CREATE INDEX idx_x ON t(col)` |
| GIN | 全文搜索、JSONB | `CREATE INDEX idx_x ON t USING GIN(col)` |
| GiST | 地理空間 | `CREATE INDEX idx_x ON t USING GiST(col)` |
| BRIN | 大表順序數據 | `CREATE INDEX idx_x ON t USING BRIN(col)` |
