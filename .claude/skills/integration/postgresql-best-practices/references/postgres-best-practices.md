# PostgreSQL Best Practices for SmartAdmin

本文檔整理 SmartAdmin 項目中 PostgreSQL 數據庫的最佳實踐和常見優化模式。

---

## 目錄

1. [連接池優化](#連接池優化-hikaricp)
2. [N+1 查詢避免](#n1-查詢避免)
3. [索引策略](#索引策略)
4. [查詢優化](#查詢優化)
5. [事務管理](#事務管理)
6. [監控告警](#監控告警)

---

## 連接池優化 (HikariCP)

### 基本原則

**黃金公式**（HikariCP 官方）:
```
connections = ((core_count × 2) + effective_spindle_count)
```

**參數解釋**:
- `core_count`: CPU 核心數
- `effective_spindle_count`: 硬碟主軸數（HDD = 實際數量，SSD = 1）

### SmartAdmin 標準配置

#### 開發環境（單機 MacBook Pro M2）

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 2
      maximum-pool-size: 5  # (4 × 2) + 1 = 9，取 5 節省資源
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

#### 測試環境（4 核雲服務器）

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 3
      maximum-pool-size: 10  # (4 × 2) + 1 = 9，保留 10% 冗餘
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

#### 生產環境（8 核雲服務器，高峰期 QPS 500+）

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20  # (8 × 2) + 1 = 17，保留 20% 冗餘
      connection-timeout: 60000  # 高峰期增加超時時間
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000  # 生產環境啟用洩漏檢測
      register-mbeans: true            # 啟用 JMX 監控
```

### 調優檢查清單

| 檢查項 | 標準 | 工具 |
|--------|------|------|
| 利用率 | 50-80% | JMX MXBean |
| 等待線程 | < 5 | JMX MXBean |
| 超時錯誤 | < 10/小時 | 應用日誌 |
| 連接創建時間 | < 100ms | P6Spy 日誌 |

---

## N+1 查詢避免

### 識別 N+1 模式

**定義**: 執行 1 次主查詢 + N 次關聯查詢

**典型症狀**:
```java
// ❌ N+1 Anti-Pattern
List<Employee> employees = employeeDao.listAll();  // 1 次查詢
for (Employee emp : employees) {
    Department dept = departmentDao.getById(emp.getDeptId());  // N 次查詢
    emp.setDepartment(dept);
}
```

### 修復策略

#### 策略 1: MyBatis-Plus LEFT JOIN（推薦）

```java
// ✅ 使用 LEFT JOIN 一次獲取
@Mapper
public interface EmployeeDao extends BaseMapper<EmployeeEntity> {
    @Select("""
        SELECT e.*, d.department_name, d.manager_id
        FROM t_employee e
        LEFT JOIN t_department d ON e.dept_id = d.department_id
        WHERE e.deleted_flag = 0
        """)
    List<EmployeeVO> listEmployeesWithDepartment();
}
```

#### 策略 2: MyBatis ResultMap 嵌套查詢（適用於複雜關聯）

```xml
<resultMap id="EmployeeWithDepartmentMap" type="EmployeeVO">
    <id property="employeeId" column="employee_id"/>
    <result property="employeeName" column="employee_name"/>
    <association property="department" javaType="DepartmentVO">
        <id property="departmentId" column="dept_id"/>
        <result property="departmentName" column="department_name"/>
    </association>
</resultMap>

<select id="listEmployeesWithDepartment" resultMap="EmployeeWithDepartmentMap">
    SELECT e.*, d.department_name
    FROM t_employee e
    LEFT JOIN t_department d ON e.dept_id = d.department_id
    WHERE e.deleted_flag = 0
</select>
```

#### 策略 3: IN 查詢批量獲取（適用於簡單關聯）

```java
// ✅ 批量查詢關聯數據
List<Employee> employees = employeeDao.listAll();  // 1 次查詢
List<Long> deptIds = employees.stream()
    .map(Employee::getDeptId)
    .distinct()
    .collect(Collectors.toList());

Map<Long, Department> deptMap = departmentDao.selectBatchIds(deptIds)  // 1 次查詢
    .stream()
    .collect(Collectors.toMap(Department::getId, Function.identity()));

employees.forEach(emp -> emp.setDepartment(deptMap.get(emp.getDeptId())));
```

### P6Spy 檢測配置

```properties
# spy.properties
appender=com.p6spy.engine.spy.appender.FileLogger
logfile=logs/spy.log
dateformat=yyyy-MM-dd HH:mm:ss
driverlist=org.postgresql.Driver

# 自定義日誌格式（便於解析）
logMessageFormat=com.p6spy.engine.spy.appender.CustomLineFormat
customLogMessageFormat=%(currentTime)|%(executionTime)|%(category)|%(sql)

# 過濾慢查詢（僅記錄 > 100ms）
executionThreshold=100
```

---

## 索引策略

### 索引類型選擇

| 場景 | 索引類型 | 範例 |
|------|---------|------|
| 主鍵 | B-Tree (自動) | `PRIMARY KEY (id)` |
| 外鍵 | B-Tree | `CREATE INDEX idx_employee_dept_id ON t_employee(dept_id)` |
| 頻繁 WHERE 條件 | B-Tree | `CREATE INDEX idx_employee_status ON t_employee(status)` |
| 全文檢索 | GIN | `CREATE INDEX idx_employee_name_gin ON t_employee USING GIN (to_tsvector('english', employee_name))` |
| 範圍查詢 | B-Tree | `CREATE INDEX idx_order_create_time ON t_order(create_time)` |
| 唯一約束 | Unique | `CREATE UNIQUE INDEX uk_employee_phone ON t_employee(phone)` |

### 複合索引規則

**最左前綴匹配原則**:

```sql
-- 創建複合索引
CREATE INDEX idx_employee_dept_status ON t_employee(dept_id, status, create_time);

-- ✅ 可使用索引
SELECT * FROM t_employee WHERE dept_id = 1;
SELECT * FROM t_employee WHERE dept_id = 1 AND status = 1;
SELECT * FROM t_employee WHERE dept_id = 1 AND status = 1 AND create_time > '2026-01-01';

-- ❌ 無法使用索引
SELECT * FROM t_employee WHERE status = 1;
SELECT * FROM t_employee WHERE create_time > '2026-01-01';
```

### 索引監控查詢

```sql
-- 查找缺少索引的表（Seq Scan 次數高）
SELECT
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    seq_tup_read / NULLIF(seq_scan, 0) AS avg_tup_per_scan
FROM pg_stat_user_tables
WHERE seq_scan > 100  -- Seq Scan 次數 > 100
ORDER BY seq_scan DESC
LIMIT 20;

-- 查找未使用的索引
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexrelname NOT LIKE 'pg_%'
ORDER BY pg_relation_size(indexrelid) DESC;
```

---

## 查詢優化

### EXPLAIN ANALYZE 使用

```sql
-- 分析查詢執行計劃
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
SELECT e.*, d.department_name
FROM t_employee e
LEFT JOIN t_department d ON e.dept_id = d.department_id
WHERE e.status = 1
ORDER BY e.create_time DESC
LIMIT 20;
```

**關鍵指標解讀**:

| 指標 | 含義 | 理想值 |
|------|------|--------|
| **Seq Scan** | 全表掃描 | 避免（應使用 Index Scan） |
| **Index Scan** | 索引掃描 | ✅ 理想 |
| **Bitmap Index Scan** | 位圖索引掃描 | ✅ 適用於多條件查詢 |
| **Nested Loop** | 嵌套循環 JOIN | 小表 JOIN 可接受 |
| **Hash Join** | 哈希 JOIN | ✅ 大表 JOIN 首選 |
| **Planning Time** | 計劃時間 | < 10ms |
| **Execution Time** | 執行時間 | < 100ms（取決於業務） |

### 分頁查詢優化

#### ❌ 深分頁問題

```sql
-- 低效：需要掃描前 10,000 條記錄
SELECT * FROM t_employee
ORDER BY create_time DESC
LIMIT 20 OFFSET 10000;
```

#### ✅ 游標分頁（推薦）

```sql
-- 使用上次最後一條記錄的 ID 作為游標
SELECT * FROM t_employee
WHERE create_time < '2026-01-20 10:00:00'  -- 上次最後一條的 create_time
ORDER BY create_time DESC
LIMIT 20;
```

#### ✅ SmartAdmin PageHelper 配置

```java
// PageParam.java
@Data
public class PageParam {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String cursor;  // 游標（上次最後一條記錄的 ID）
}

// EmployeeService.java
public PageResult<EmployeeVO> listEmployees(PageParam pageParam) {
    if (StringUtils.isNotBlank(pageParam.getCursor())) {
        // 游標分頁
        return employeeDao.listByCursor(pageParam.getCursor(), pageParam.getPageSize());
    } else {
        // 普通分頁
        Page<EmployeeEntity> page = Page.of(pageParam.getPageNum(), pageParam.getPageSize());
        return PageUtil.convert(employeeDao.selectPage(page, null), EmployeeVO.class);
    }
}
```

---

## 事務管理

### SmartAdmin 事務規範

**強制規則**（由 ArchUnit 測試強制執行）:

```java
// ❌ Service 層禁止 @Transactional
@Service
public class EmployeeService {
    @Transactional(rollbackFor = Throwable.class)  // ❌ 違反規則
    public void saveEmployee(EmployeeEntity employee) {
        // ...
    }
}

// ✅ Manager 層使用 @Transactional
@Service
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)  // ✅ 正確
    public void saveEmployeeTransaction(EmployeeEntity employee) {
        employeeDao.insert(employee);
        // 多表操作...
    }
}
```

### 事務最佳實踐

1. **最小化事務範圍**
   ```java
   // ❌ 錯誤：事務包含 HTTP 調用
   @Transactional
   public void processOrder(Order order) {
       orderDao.insert(order);
       httpClient.notifyPayment(order);  // ❌ 網絡 I/O 在事務內
   }

   // ✅ 正確：僅數據庫操作在事務內
   public void processOrder(Order order) {
       saveOrderTransaction(order);
       httpClient.notifyPayment(order);  // ✅ 網絡 I/O 在事務外
   }

   @Transactional
   private void saveOrderTransaction(Order order) {
       orderDao.insert(order);
   }
   ```

2. **使用正確的隔離級別**
   ```java
   // 默認：READ_COMMITTED（SmartAdmin 標準）
   @Transactional(rollbackFor = Throwable.class)
   public void normalOperation() { }

   // 防止髒讀、不可重複讀、幻讀（性能較低）
   @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Throwable.class)
   public void criticalOperation() { }
   ```

3. **顯式回滾異常**
   ```java
   // ✅ SmartAdmin 強制規範
   @Transactional(rollbackFor = Throwable.class)  // 捕獲所有異常
   public void saveEmployee(EmployeeEntity employee) { }
   ```

---

## 監控告警

### Grafana 監控指標

#### HikariCP 連接池

```promql
# 活躍連接數
hikaricp_connections_active{pool="SmartAdminHikariPool"}

# 空閒連接數
hikaricp_connections_idle{pool="SmartAdminHikariPool"}

# 等待線程數
hikaricp_connections_pending{pool="SmartAdminHikariPool"}

# 連接創建時間（P95）
histogram_quantile(0.95, hikaricp_connections_creation_seconds_bucket{pool="SmartAdminHikariPool"})
```

#### PostgreSQL 數據庫

```promql
# 活躍連接數
pg_stat_activity_count{state="active"}

# 長事務（> 30 秒）
pg_stat_activity_max_tx_duration{state="active"} > 30

# 慢查詢（> 1 秒）
pg_stat_statements_mean_time_seconds > 1

# 表膨脹率
pg_stat_user_tables_n_dead_tup / pg_stat_user_tables_n_live_tup > 0.2
```

### 告警規則（Prometheus）

```yaml
# hikaricp-alerts.yml
groups:
  - name: hikaricp
    interval: 30s
    rules:
      - alert: HikariCPHighUtilization
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "HikariCP 連接池利用率過高"
          description: "{{ $labels.pool }} 利用率 {{ $value | humanizePercentage }}，建議增加 maximum-pool-size"

      - alert: HikariCPConnectionTimeout
        expr: increase(hikaricp_connections_timeout_total[5m]) > 10
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "HikariCP 連接超時"
          description: "{{ $labels.pool }} 在過去 5 分鐘內發生 {{ $value }} 次超時"

  - name: postgresql
    interval: 30s
    rules:
      - alert: PostgreSQLSlowQuery
        expr: pg_stat_statements_mean_time_seconds > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL 慢查詢"
          description: "查詢 {{ $labels.query }} 平均執行時間 {{ $value }}s"

      - alert: PostgreSQLHighConnections
        expr: pg_stat_activity_count / pg_settings_max_connections > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL 連接數過高"
          description: "當前連接數 {{ $value | humanizePercentage }}，接近上限"
```

---

## 參考資料

### 官方文檔

- [PostgreSQL Performance Tips](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [MyBatis-Plus Performance](https://baomidou.com/pages/223848/)

### SmartAdmin 內部文檔

- [架構規則](../../../../.agent/rules/foundation/10-architecture-rules.md)
- [Manager 層規範](../../../../.agent/rules/foundation/09-manager-layer.md)
- [事務管理模式](../../../../.claude/shared/knowledge/smartadmin-patterns.md#transaction-management)

---

**維護者**: SmartAdmin Skills Team
**最後更新**: 2026-01-29
**適用版本**: SmartAdmin v3.0.0+, PostgreSQL 14+
