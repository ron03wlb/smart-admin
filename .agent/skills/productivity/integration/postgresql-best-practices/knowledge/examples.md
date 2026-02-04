# PostgreSQL Best Practices - Examples

## 範例 1: HikariCP 連線池優化

**問題**: 生產環境頻繁出現連線超時

**分析步驟**:
```sql
-- 1. 查看當前連線狀態
SELECT count(*), state FROM pg_stat_activity GROUP BY state;

-- 2. 查看等待連線的查詢
SELECT pid, now() - query_start AS duration, query
FROM pg_stat_activity
WHERE state = 'active' AND query_start < now() - interval '30 seconds';
```

**優化配置**:
```yaml
spring:
  datasource:
    hikari:
      # 基於 4 核心 CPU 計算
      minimum-idle: 5
      maximum-pool-size: 10

      # 連線生命週期
      idle-timeout: 600000      # 10 分鐘
      max-lifetime: 1800000     # 30 分鐘

      # 連線獲取超時
      connection-timeout: 30000 # 30 秒

      # 連線驗證
      connection-test-query: SELECT 1
      validation-timeout: 5000
```

---

## 範例 2: N+1 查詢修復

**問題**: 員工列表查詢觸發大量部門查詢

**Before (N+1 問題)**:
```java
public List<EmployeeVO> list() {
    List<EmployeeEntity> employees = employeeDao.selectList();
    return employees.stream()
        .map(e -> {
            EmployeeVO vo = SmartBeanUtil.copy(e, EmployeeVO.class);
            // N+1: 每個員工查一次部門
            DepartmentEntity dept = departmentDao.selectById(e.getDepartmentId());
            vo.setDepartmentName(dept.getDepartmentName());
            return vo;
        })
        .collect(Collectors.toList());
}
```

**After (JOIN 優化)**:
```java
// EmployeeDao.java
@Select("""
    SELECT e.*, d.department_name
    FROM t_employee e
    LEFT JOIN t_department d ON e.department_id = d.department_id
    WHERE e.deleted_flag = false
    """)
List<EmployeeWithDeptVO> selectWithDepartment();

// EmployeeService.java
public List<EmployeeVO> list() {
    return employeeDao.selectWithDepartment();
}
```

---

## 範例 3: 慢查詢優化

**問題**: 員工搜索超過 5 秒

**EXPLAIN ANALYZE 分析**:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM t_employee
WHERE department_id = 1 AND status = 1
ORDER BY create_time DESC
LIMIT 20;

-- 結果:
-- Seq Scan on t_employee (cost=0.00..15234.00 rows=10000 width=200)
--   Filter: ((department_id = 1) AND (status = 1))
--   Rows Removed by Filter: 990000
-- Sort (cost=...) Sort Method: external merge  Disk: 15MB
```

**優化方案**:
```sql
-- 1. 創建複合索引
CREATE INDEX idx_employee_dept_status_time
ON t_employee(department_id, status, create_time DESC);

-- 2. 再次分析
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM t_employee
WHERE department_id = 1 AND status = 1
ORDER BY create_time DESC
LIMIT 20;

-- 優化後結果:
-- Index Scan using idx_employee_dept_status_time on t_employee
--   Index Cond: ((department_id = 1) AND (status = 1))
-- Planning Time: 0.2 ms
-- Execution Time: 1.5 ms
```

---

## 範例 4: 完整優化報告

```markdown
# PostgreSQL 效能優化報告

## 1. 連線池分析

| 指標 | 當前值 | 建議值 | 狀態 |
|------|--------|--------|------|
| max-pool-size | 50 | 10 | ❌ 過大 |
| min-idle | 10 | 5 | ⚠️ 偏高 |
| 利用率 | 15% | - | ✅ 正常 |

**建議**: 減少 max-pool-size 至 10，避免資源浪費

## 2. N+1 查詢檢測

發現 3 處 N+1 問題:
1. `EmployeeService.list()` - 部門查詢
2. `OrderService.getDetail()` - 商品查詢
3. `ReportService.generate()` - 用戶查詢

## 3. 索引建議

```sql
-- 高優先級
CREATE INDEX idx_employee_dept_status ON t_employee(department_id, status);
CREATE INDEX idx_order_user_time ON t_order(user_id, create_time DESC);

-- 中優先級
CREATE INDEX idx_product_category ON t_product(category_id) WHERE deleted_flag = false;
```
```
