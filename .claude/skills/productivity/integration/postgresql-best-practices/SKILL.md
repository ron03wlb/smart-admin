---
name: postgresql-best-practices
description: [P2 - Productivity] PostgreSQL performance analysis and optimization (HikariCP tuning, N+1 detection, EXPLAIN ANALYZE, index recommendations). Use when investigating database timeouts, high connection usage, or query performance issues.
---

# PostgreSQL Best Practices Skill

**Version**: 1.0.0
**Priority**: P2 (Productivity)
**Category**: Integration
**Status**: Stable

---

## 概述

PostgreSQL 性能分析和優化技能，專為 SmartAdmin 項目設計，提供全面的數據庫性能診斷和優化建議。

### 核心功能

| 功能 | 描述 | 輸出 |
|------|------|------|
| **HikariCP 分析** | 分析連接池利用率，推薦最佳配置 | 調優建議（min-idle, max-pool-size） |
| **N+1 查詢檢測** | 解析 P6Spy 日誌，識別 N+1 模式 | N+1 模式清單 + SQL |
| **EXPLAIN ANALYZE** | 自動執行慢查詢分析 | 執行計劃 + 性能指標 |
| **索引建議** | 基於 pg_stat_user_tables 推薦索引 | CREATE INDEX 語句 |

---

## Version

**Skill Version**: 1.0.0
**Last Updated**: 2026-01-31
**Compatible With**: SmartAdmin v4.0.0+, .claude/ system v3.0.2+

---

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "database performance" - Database performance analysis
- "PostgreSQL optimization" - PostgreSQL query optimization
- "HikariCP tuning" - HikariCP connection pool tuning
- "N+1 query detection" - Detect N+1 query patterns
- "slow query analysis" - Analyze slow queries

**Secondary Keywords** (Medium confidence):
- "connection pool" - Context: HikariCP connection pooling
- "index recommendation" - Context: database index optimization
- "EXPLAIN ANALYZE" - Context: PostgreSQL execution plan analysis
- "pg_stat" - Context: PostgreSQL statistics analysis

**Phrase Patterns**:
- "Analyze [database issue]" - Example: "Analyze database timeout errors"
- "Optimize [query type]" - Example: "Optimize employee search queries"
- "Tune [component]" - Example: "Tune HikariCP connection pool"

**Example User Requests**:
```
User: "Analyze database performance issues causing timeouts"
User: "Detect N+1 query patterns in Employee module"
User: "Tune HikariCP connection pool configuration"
User: "Optimize slow PostgreSQL queries with index recommendations"
```

**Note**: This skill can also be manually invoked via `/postgresql-best-practices` or `/postgres-analyze` command.

### 命令觸發

```bash
# 完整分析（推薦）
/postgres-analyze

# 僅 HikariCP 分析
/postgres-analyze --mode hikaricp-only

# 僅 N+1 檢測
/postgres-analyze --mode n1-detection --log logs/spy.log

# 僅 EXPLAIN ANALYZE
/postgres-analyze --mode explain-analyze --query "SELECT * FROM employee WHERE dept_id = 1"

# 僅索引建議
/postgres-analyze --mode index-recommendations
```

---

## 使用範例

### 範例 1: 完整性能分析

**場景**：應用響應緩慢，懷疑數據庫性能問題

```
使用者：我們的應用最近變慢了，能幫我分析一下數據庫性能嗎？

AI 助手：[觸發 postgresql-best-practices skill]

執行步驟：
1. ✅ HikariCP 分析
   - 當前利用率：85%（高）
   - 建議：max-pool-size: 10 → 15

2. ✅ N+1 查詢檢測
   - 發現 3 個 N+1 模式：
     * EmployeeService.listEmployees() - 缺少 JOIN
     * DepartmentService.getDepartmentDetails() - 缺少 LEFT JOIN
     * RoleService.getRolesByUserId() - 應使用 IN 查詢

3. ✅ EXPLAIN ANALYZE
   - 5 個慢查詢已分析
   - 發現 2 個全表掃描（Seq Scan）

4. ✅ 索引建議
   - 建議為 5 個表創建索引
   - 預計性能提升：40-60%

報告已生成：docs/performance/postgresql-performance-report.md
```

### 範例 2: HikariCP 調優

**場景**：連接池頻繁超時

```
使用者：連接池經常報 "Connection timeout" 錯誤

AI 助手：[觸發 postgresql-best-practices skill --mode hikaricp-only]

分析結果：
- 當前配置：
  * minimum-idle: 2
  * maximum-pool-size: 10
  * connection-timeout: 30000ms

- 實際利用率：
  * 活躍連接：平均 9.2
  * 空閒連接：平均 0.3
  * 等待線程：15

- 建議調整：
  * maximum-pool-size: 10 → 20（增加連接數）
  * minimum-idle: 2 → 5（保持更多空閒連接）
  * connection-timeout: 30000 → 60000（增加超時時間）

預期效果：減少 90% 的超時錯誤
```

### 範例 3: N+1 查詢檢測

**場景**：員工列表頁面載入緩慢

```
使用者：員工列表頁面要 5 秒才能加載完，怎麼回事？

AI 助手：[觸發 postgresql-best-practices skill --mode n1-detection]

檢測結果：

❌ N+1 模式 #1：EmployeeService.listEmployees()
   SQL 模板：SELECT * FROM t_employee WHERE dept_id = ?
   執行次數：127 次
   總耗時：3.2 秒

問題原因：
   for (Employee emp : employees) {
       emp.setDepartment(departmentDao.getById(emp.getDeptId()));  // ← N+1
   }

修復建議：
   // 使用 JOIN 一次性獲取
   SELECT e.*, d.* FROM t_employee e
   LEFT JOIN t_department d ON e.dept_id = d.id

預期性能提升：3.2s → 0.3s（提升 90%）
```

---

## 輸出格式

### 報告結構

生成的性能報告（Markdown 格式）包含以下部分：

```markdown
# PostgreSQL Performance Report

**Generated**: 2026-01-29 10:30:00
**Database**: SmartAdmin (PostgreSQL 16.1)
**Analysis Duration**: 45 seconds

---

## 📊 Executive Summary

- ⚠️  HikariCP 利用率過高（85%，建議 < 80%）
- ❌ 發現 3 個 N+1 查詢模式
- ✅ EXPLAIN ANALYZE: 8/10 查詢已優化
- ⚠️  建議為 5 個表創建索引

**優先級行動項**：
1. [HIGH] 修復 EmployeeService N+1 查詢（預計提升 90% 性能）
2. [HIGH] 調整 HikariCP max-pool-size: 10 → 15
3. [MEDIUM] 為 t_employee.dept_id 創建索引
4. [LOW] 優化 DepartmentService 查詢

---

## 🔧 HikariCP Connection Pool Analysis

### 當前配置
- minimum-idle: 2
- maximum-pool-size: 10
- connection-timeout: 30000ms

### 利用率統計
- 活躍連接：平均 8.5 / 最大 10
- 空閒連接：平均 1.5
- 利用率：85%（⚠️  接近上限）

### 建議調整
\`\`\`yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 15  # 10 → 15
      minimum-idle: 3        # 2 → 3
\`\`\`

---

## 🔍 N+1 Query Detection Results

### N+1 模式 #1: EmployeeService.listEmployees()
- **SQL 模板**: `SELECT * FROM t_employee WHERE dept_id = ?`
- **執行次數**: 127 次
- **總耗時**: 3.2 秒
- **嚴重性**: HIGH

**修復建議**：
\`\`\`java
// 修改前（N+1）
for (Employee emp : employees) {
    emp.setDepartment(departmentDao.getById(emp.getDeptId()));
}

// 修改後（JOIN）
SELECT e.*, d.* FROM t_employee e
LEFT JOIN t_department d ON e.dept_id = d.id
\`\`\`

---

## 📈 EXPLAIN ANALYZE Reports

### 慢查詢 #1: EmployeeDao.listEmployees()
\`\`\`sql
EXPLAIN ANALYZE
SELECT * FROM t_employee WHERE dept_id = 1
\`\`\`

**執行計劃**：
- Seq Scan on t_employee (cost=0.00..245.00 rows=5000 width=120) (actual time=0.015..3.482 rows=127 loops=1)
- Planning Time: 0.082 ms
- Execution Time: 3.521 ms

**問題**：全表掃描（Seq Scan），缺少索引

---

## 🎯 Index Recommendations

| 表名 | 列 | Seq Scan 次數 | 建議索引 | 預計提升 |
|------|-----|--------------|---------|---------|
| t_employee | dept_id | 5,241 | ✅ 高優先級 | 70% |
| t_employee | role_id | 3,102 | ✅ 高優先級 | 60% |
| t_order | user_id | 1,843 | ✅ 中優先級 | 50% |

**CREATE INDEX 語句**：
\`\`\`sql
-- High Priority
CREATE INDEX idx_employee_dept_id ON t_employee(dept_id);
CREATE INDEX idx_employee_role_id ON t_employee(role_id);

-- Medium Priority
CREATE INDEX idx_order_user_id ON t_order(user_id);
\`\`\`

---

## ✅ Action Items (Prioritized)

1. **[HIGH]** 修復 EmployeeService N+1 查詢
   - 預計耗時：30 分鐘
   - 預計提升：90%（3.2s → 0.3s）

2. **[HIGH]** 創建 t_employee.dept_id 索引
   - 預計耗時：5 分鐘
   - 預計提升：70%

3. **[MEDIUM]** 調整 HikariCP max-pool-size
   - 預計耗時：5 分鐘
   - 預計提升：減少 80% 超時錯誤

4. **[LOW]** 優化 DepartmentService 查詢
   - 預計耗時：20 分鐘
   - 預計提升：30%

---

## 📖 References
- [HikariCP Best Practices](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- [PostgreSQL EXPLAIN](https://www.postgresql.org/docs/current/sql-explain.html)
- [SmartAdmin Performance Guide](../../../references/postgres-best-practices.md)
```

---

## 依賴配置

### Gradle 依賴

```kotlin
// build.gradle.kts
dependencies {
    // P6Spy for SQL logging
    runtimeOnly("com.p6spy:p6spy:3.9.1")

    // Testcontainers for integration testing
    testImplementation("org.testcontainers:postgresql:1.19.3")
}
```

### Spring Boot 配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:p6spy:postgresql://localhost:5432/smartadmin
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
    hikari:
      minimum-idle: 2
      maximum-pool-size: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: SmartAdminHikariPool
```

### P6Spy 配置

```properties
# spy.properties
appender=com.p6spy.engine.spy.appender.FileLogger
logfile=logs/spy.log
dateformat=yyyy-MM-dd HH:mm:ss
driverlist=org.postgresql.Driver
logMessageFormat=com.p6spy.engine.spy.appender.CustomLineFormat
customLogMessageFormat=%(currentTime)|%(executionTime)|%(category)|%(sql)
```

---

## 質量標準

### HikariCP 利用率標準

| 利用率 | 狀態 | 行動 |
|--------|------|------|
| < 50% | ✅ 優秀 | 可考慮減少 max-pool-size |
| 50-80% | ✅ 良好 | 維持現狀 |
| 80-90% | ⚠️ 警告 | 建議增加 max-pool-size |
| > 90% | ❌ 危險 | 立即增加 max-pool-size |

### N+1 檢測標準

- **模式定義**：相同 SQL 模板執行 > 10 次，間隔 < 100ms
- **嚴重性**：HIGH（必須修復）
- **優先級**：P0（影響用戶體驗）

### 索引建議標準

- **最低 Seq Scan 次數**：100
- **優先級排序**：按 Seq Scan 次數降序
- **預期提升**：根據 pg_stat_user_tables 統計數據計算

---

## 故障排除

### 問題 1: P6Spy 日誌未生成

**症狀**：N+1 檢測返回空結果

**原因**：P6Spy 未正確配置

**解決方案**：
```bash
# 1. 檢查 spy.properties 是否存在
ls src/main/resources/spy.properties

# 2. 驗證日誌目錄權限
mkdir -p logs
chmod 755 logs

# 3. 測試 P6Spy 是否正常工作
tail -f logs/spy.log
```

### 問題 2: HikariCP MXBean 無法訪問

**症狀**：HikariCP 分析返回 "MXBean not available"

**原因**：HikariCP 未啟用 JMX

**解決方案**：
```yaml
spring:
  datasource:
    hikari:
      register-mbeans: true  # 啟用 JMX
```

### 問題 3: EXPLAIN ANALYZE 權限錯誤

**症狀**：執行 EXPLAIN ANALYZE 時報 "permission denied"

**原因**：數據庫用戶權限不足

**解決方案**：
```sql
-- 授予 SELECT 權限
GRANT SELECT ON ALL TABLES IN SCHEMA public TO smartadmin_user;
```

---

## 參考資料

### 內部文檔

- [HikariCP 調優範例](examples/hikaricp-tuning-example.md)
- [PostgreSQL 最佳實踐](references/postgres-best-practices.md)
- [性能報告模板](templates/performance-report.md.template)

### 外部資源

- [HikariCP About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- [PostgreSQL Performance Tips](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [P6Spy Documentation](https://p6spy.readthedocs.io/en/latest/)

---

## 版本歷史

| 版本 | 日期 | 變更內容 |
|------|------|---------|
| 1.0.0 | 2026-01-29 | 初始版本，支持 4 種分析功能 |

---

**維護者**: SmartAdmin Skills Team
**最後更新**: 2026-01-29
