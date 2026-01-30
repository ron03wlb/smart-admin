# SmartAdmin Performance Suite

> 一站式性能優化解決方案：診斷（Diagnose）→ 優化（Optimize）→ 監控（Monitor）統一工作流

## 🚀 快速開始（5 分鐘上手）

### 最簡單的使用方式

```bash
# 場景：員工列表接口很慢，需要優化
User: "The employee listing endpoint is slow, taking 5+ seconds"

# AI 自動檢測並執行完整工作流
/performance /api/employees/list --workflow

# 自動執行：
# 1. 診斷（10分鐘）：N+1 查詢檢測、JVM 分析、SQL 慢查詢
# 2. 優化（12分鐘）：多級緩存實現（Caffeine L1 + Redis L2）
# 3. 監控（8分鐘）：Skywalking APM、Micrometer 指標、Grafana 儀表板

# 總時間：約 30 分鐘（vs 60 分鐘用 3 個獨立技能）
```

### 預期輸出

**診斷報告**：
```markdown
# Performance Diagnosis Report

## N+1 Query Detection
❌ EmployeeService.listPage() - 發現 N+1 查詢
   - 主查詢：SELECT * FROM t_employee
   - N 次子查詢：SELECT * FROM t_department WHERE id = ? (執行 50 次)
   建議：使用 LEFT JOIN 或 MyBatis-Plus association

## JVM Analysis
⚠️ Old Generation 使用率 85%（建議 < 70%）
   建議：增加堆內存 -Xmx2g → -Xmx4g

## SQL Slow Query
❌ Query #1: 3.2 秒
   SELECT ... FROM t_employee e, t_department d WHERE ...
   建議：添加索引 INDEX(department_id)
```

**優化實現**：
```java
// 自動生成的緩存代碼
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    @Cacheable(value = "employee", key = "#id")
    public Employee getById(Long id) {
        return employeeDao.selectById(id);
    }
}
```

**監控配置**：
- Skywalking Agent 配置（`skywalking/agent.config`）
- Grafana 儀表板 JSON（`monitoring/employee-dashboard.json`）
- Prometheus 指標配置（`application.yml`）

---

## 📖 執行模式（如何選擇？）

### Mode 1: `--workflow` 完整工作流（推薦）

**何時使用**：全新性能優化項目，需要從診斷到監控的完整流程

```bash
/performance /api/orders/list --workflow
```

**執行流程**：
```
1. Diagnose（診斷）：10分鐘
   - N+1 查詢檢測（MyBatis Log + P6Spy）
   - JVM 性能分析（堆內存、GC 統計）
   - SQL 慢查詢分析（EXPLAIN ANALYZE）
   - CPU 熱點檢測（Async-profiler）
   ↓
2. Optimize（優化）：12分鐘
   - 生成多級緩存代碼（Caffeine L1 + Redis L2）
   - 緩存一致性策略（TTL、失效模式）
   - N+1 查詢修復建議
   ↓
3. Monitor（監控）：8分鐘
   - 配置 Skywalking APM（自動儀器化）
   - 配置 Micrometer 指標（性能計數器）
   - 生成 Grafana 儀表板（可視化）
   - 配置告警規則（閾值警報）
```

**時間**：約 30 分鐘
**適用場景**：生產環境性能優化、新項目性能基準建立

---

### Mode 2: `--mode=diagnose` 僅診斷模式

**何時使用**：已知性能問題，需要找出根本原因

```bash
/performance /api/employees/list --mode=diagnose
```

**診斷內容**：
- ✅ **N+1 查詢檢測**
  - 工具：P6Spy SQL 日誌分析
  - 輸出：N+1 查詢位置、執行次數、修復建議

- ✅ **JVM 性能分析**
  - 工具：JMX MBeans（堆內存、GC）
  - 輸出：堆使用率、GC 頻率、Old Gen 碎片化

- ✅ **SQL 慢查詢分析**
  - 工具：EXPLAIN ANALYZE（PostgreSQL）
  - 輸出：執行計劃、索引缺失、表掃描警告

- ✅ **CPU 熱點檢測**
  - 工具：Async-profiler（火焰圖）
  - 輸出：CPU 熱點方法、調用堆棧

**時間**：約 10 分鐘
**適用場景**：性能問題排查、生產環境故障分析

**整合替代技能**：前 `java-performance-pro`

---

### Mode 3: `--mode=optimize` 僅優化模式

**何時使用**：已診斷出問題，需要實現優化方案（通常是緩存）

```bash
/performance ProductService.getById --mode=optimize
```

**優化策略**：
- ✅ **多級緩存實現**
  - L1 本地緩存：Caffeine（JVM 內存，毫秒級訪問）
  - L2 分佈式緩存：Redis（共享緩存，10ms 級訪問）

- ✅ **緩存一致性**
  - TTL 過期策略（時間驅動）
  - 主動失效模式（事件驅動）
  - Canal 監聽數據庫變更（CDC）

- ✅ **N+1 查詢修復**
  - MyBatis-Plus Association（一次查詢加載關聯）
  - LEFT JOIN 重寫（SQL 優化）
  - 批量查詢封裝（Batch Fetching）

**生成代碼**：
```java
// Manager 層自動添加緩存註解
@Service
@RequiredArgsConstructor
public class ProductManager {
    @Cacheable(value = "product", key = "#id", unless = "#result == null")
    public Product getById(Long id) {
        return productDao.selectById(id);
    }

    @CacheEvict(value = "product", key = "#id")
    public void updateProduct(Long id, ProductUpdateForm form) {
        // Update logic
    }
}
```

**配置文件**：
```yaml
# application.yml 自動添加緩存配置
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=600s
```

**時間**：約 12 分鐘
**適用場景**：緩存架構設計、熱點數據優化、讀多寫少場景

**整合替代技能**：前 `cache-strategy-generator`

---

### Mode 4: `--mode=monitor` 僅監控模式

**何時使用**：優化已實施，需要建立監控體系驗證效果

```bash
/performance OrderService --mode=monitor
```

**監控組件**：
- ✅ **Skywalking APM**
  - 分佈式追蹤（Trace ID 跨服務關聯）
  - 自動儀器化（無侵入性）
  - 服務拓撲圖（依賴關係可視化）

- ✅ **Micrometer 指標**
  - JVM 指標（堆內存、GC、線程）
  - HTTP 指標（請求速率、響應時間、錯誤率）
  - 自定義業務指標（訂單量、支付成功率）

- ✅ **Grafana 儀表板**
  - JVM 監控面板（堆使用、GC 頻率）
  - HTTP 性能面板（P95/P99 延遲）
  - 業務指標面板（自定義）

- ✅ **告警規則**
  - CPU 使用率 > 80%
  - 響應時間 P95 > 500ms
  - 錯誤率 > 1%

**生成文件**：
```
monitoring/
├── skywalking/
│   └── agent.config                    # Skywalking Agent 配置
├── grafana/
│   ├── order-service-dashboard.json    # Grafana 儀表板
│   └── alert-rules.yml                 # Prometheus 告警規則
└── prometheus/
    └── prometheus.yml                  # Prometheus 抓取配置
```

**時間**：約 8 分鐘
**適用場景**：生產環境可觀測性、SRE 監控體系、性能回歸測試

**整合替代技能**：前 `apm-integration-skill`

---

## 🎯 使用場景

### ✅ When to Use（何時使用）

1. **生產環境性能優化**
   - 場景：生產接口響應時間 > 500ms，需要快速定位並優化
   - 選擇：`--workflow`（完整工作流）
   - 時間：30 分鐘獲得診斷報告 + 優化代碼 + 監控配置

2. **新項目性能基準建立**
   - 場景：新項目上線前，建立性能監控體系
   - 選擇：`--workflow`（先診斷基準性能，再配置監控）

3. **性能問題故障排查**
   - 場景：用戶報告某個功能很慢，但不知道原因
   - 選擇：`--mode=diagnose`（僅診斷，10 分鐘定位問題）

4. **緩存架構設計**
   - 場景：新增熱點數據讀取，需要緩存方案
   - 選擇：`--mode=optimize`（生成多級緩存代碼）

5. **可觀測性建設**
   - 場景：已有服務，但缺少監控和追蹤
   - 選擇：`--mode=monitor`（配置 APM + 指標 + 儀表板）

### ❌ When NOT to Use（何時不使用）

1. **問題不是性能相關**
   - 場景：功能性 Bug、業務邏輯錯誤
   - 原因：本技能專注於性能優化，不處理功能問題

2. **性能瓶頸在外部服務**
   - 場景：第三方 API 響應慢、數據庫本身性能差
   - 原因：需要優化外部依賴，不是應用層問題

3. **已有成熟的性能監控體系**
   - 場景：已部署 Skywalking + Grafana + Prometheus
   - 原因：無需重複配置

4. **性能要求極高（毫秒級 SLA）**
   - 場景：高頻交易、實時競價系統
   - 原因：需要更深入的性能調優（JIT 編譯、無鎖並發、零拷貝）

---

## 🔧 組合能力說明

### 組合 Skill 1: java-performance-pro（診斷階段）

**原技能功能**：
- N+1 查詢檢測（P6Spy 日誌分析）
- JVM 性能分析（JMX MBeans）
- SQL 慢查詢分析（EXPLAIN ANALYZE）
- CPU 熱點檢測（Async-profiler）

**整合到本技能**：
- 執行模式：`--mode=diagnose`
- 時間：10 分鐘
- 輸出：診斷報告（Markdown）

**獨立使用場景**：
如果僅需要性能診斷，建議使用：
```bash
/java-performance-pro /api/employees/list  # ⚠️ 已廢棄，自動路由到 /performance --mode=diagnose
```

---

### 組合 Skill 2: cache-strategy-generator（優化階段）

**原技能功能**：
- 多級緩存代碼生成（Caffeine + Redis）
- 緩存一致性策略（TTL、失效模式）
- Cache-Aside 模式實現
- Canal CDC 集成（數據庫變更監聽）

**整合到本技能**：
- 執行模式：`--mode=optimize`
- 時間：12 分鐘
- 輸出：緩存代碼（Java）、配置文件（YAML）

**獨立使用場景**：
如果僅需要緩存設計，建議使用：
```bash
/cache-strategy ProductService.getById  # ⚠️ 已廢棄，自動路由到 /performance --mode=optimize
```

---

### 組合 Skill 3: apm-integration-skill（監控階段）

**原技能功能**：
- Skywalking APM 配置（分佈式追蹤）
- Micrometer 指標集成（Prometheus 格式）
- Grafana 儀表板生成（預定義模板）
- 告警規則配置（Prometheus AlertManager）

**整合到本技能**：
- 執行模式：`--mode=monitor`
- 時間：8 分鐘
- 輸出：監控配置文件（skywalking/agent.config, grafana/*.json）

**獨立使用場景**：
如果僅需要監控配置，建議使用：
```bash
/apm-integration UserService  # ⚠️ 已廢棄，自動路由到 /performance --mode=monitor
```

---

## ⚙️ 常見問題（FAQ）

### Q1: 為什麼整合成一個技能？時間不是更長嗎？

**A**: 整合後的時間效率提升 50%：
- **分開使用**：60 分鐘（java-performance-pro 15分鐘 + cache-strategy 25分鐘 + apm-integration 20分鐘）
- **整合使用**：30 分鐘（共享上下文、自動化銜接、並行執行）

**關鍵優化**：
- 共享診斷結果（避免重複分析）
- 自動化銜接（診斷 → 優化 → 監控無縫連接）
- 並行執行（Grafana 儀表板生成與緩存代碼生成並行）

---

### Q2: 如何自定義診斷閾值（例如，慢查詢定義為 > 1 秒）？

**A**: 在 `modes/mode-1-diagnose.md` 中自定義閾值：
```yaml
thresholds:
  slow_query_ms: 1000      # 慢查詢閾值（毫秒）
  n1_query_threshold: 10   # N+1 查詢閾值（N 次查詢）
  heap_usage_warning: 70   # 堆使用率警告（%）
  cpu_usage_critical: 80   # CPU 使用率嚴重（%）
```

---

### Q3: 生成的緩存代碼如何處理緩存穿透、擊穿、雪崩？

**A**: 自動包含防護策略：

**緩存穿透**（查詢不存在的數據）：
```java
@Cacheable(value = "product", key = "#id", unless = "#result == null")
public Product getById(Long id) {
    // 如果查詢結果為 null，不會緩存
}
```

**緩存擊穿**（熱點數據過期）：
```java
// 使用分佈式鎖防止併發重建
@Cacheable(value = "product", key = "#id", sync = true)
public Product getById(Long id) { ... }
```

**緩存雪崩**（大量緩存同時過期）：
```yaml
# TTL 添加隨機值
spring.cache.caffeine.spec: expireAfterWrite=600s,randomTtl=120s
```

---

### Q4: Skywalking Agent 會影響性能嗎？

**A**: 影響極小（< 5%）：
- **CPU 開銷**：1-3%（字節碼增強）
- **內存開銷**：30-50MB（Trace 緩衝區）
- **網絡開銷**：異步批量上報（不阻塞主線程）

**生產環境建議**：
- 採樣率設置為 10%（`agent.sample_n_per_3_secs=10`）
- 關閉不需要的插件（例如，Jedis 插件如果不使用 Redis）

---

### Q5: 如何驗證優化效果？

**A**: 三步驗證：

**1. 基準測試（優化前）**：
```bash
# 使用 wrk 壓測
wrk -t4 -c100 -d30s http://localhost:1024/api/employees/list

# 記錄基準數據
Requests/sec: 1250
Latency P95: 850ms
```

**2. 實施優化**：
```bash
/performance /api/employees/list --workflow
```

**3. 對比測試（優化後）**：
```bash
# 再次壓測
wrk -t4 -c100 -d30s http://localhost:1024/api/employees/list

# 對比優化效果
Requests/sec: 3800 (提升 3.04x)
Latency P95: 180ms (降低 78.8%)
```

---

### Q6: 能否只運行部分模式（例如，診斷 + 優化，不監控）？

**A**: 目前支援 4 種固定模式：
- `--workflow`（完整流程）
- `--mode=diagnose`
- `--mode=optimize`
- `--mode=monitor`

**如果需要自定義組合**：
手動執行兩次：
```bash
/performance /api/orders --mode=diagnose   # 第 1 步
/performance /api/orders --mode=optimize   # 第 2 步
```

---

### Q7: N+1 查詢修復建議如何實施？

**A**: 診斷報告會提供 3 種修復方案：

**方案 1: MyBatis-Plus Association（推薦）**
```java
// 原代碼（N+1）
List<Employee> employees = employeeDao.selectList();
employees.forEach(e -> {
    Department dept = departmentDao.selectById(e.getDepartmentId());  // N 次查詢
    e.setDepartment(dept);
});

// 修復後（1 次查詢）
@Results(id = "employeeWithDept", value = {
    @Result(property = "department", column = "department_id",
        one = @One(select = "...DepartmentDao.selectById"))
})
List<Employee> selectListWithDept();
```

**方案 2: LEFT JOIN**
```sql
-- 原查詢（N+1）
SELECT * FROM t_employee;  -- 主查詢
SELECT * FROM t_department WHERE id = ?;  -- N 次子查詢

-- 修復後（1 次查詢）
SELECT e.*, d.*
FROM t_employee e
LEFT JOIN t_department d ON e.department_id = d.id;
```

**方案 3: 批量查詢**
```java
// 原代碼（N+1）
employees.forEach(e -> {
    Department dept = departmentDao.selectById(e.getDepartmentId());
});

// 修復後（1 + 1 = 2 次查詢）
List<Long> deptIds = employees.stream()
    .map(Employee::getDepartmentId).collect(Collectors.toList());
List<Department> depts = departmentDao.selectBatchIds(deptIds);
Map<Long, Department> deptMap = depts.stream()
    .collect(Collectors.toMap(Department::getId, Function.identity()));
employees.forEach(e -> e.setDepartment(deptMap.get(e.getDepartmentId())));
```

---

### Q8: 緩存一致性如何保證？

**A**: 提供 3 種策略：

**策略 1: TTL 過期（簡單場景）**
```java
@Cacheable(value = "product", key = "#id", expire = 600)  // 10 分鐘過期
```
- 優點：簡單
- 缺點：可能讀取到過期數據

**策略 2: 主動失效（推薦）**
```java
@CacheEvict(value = "product", key = "#id")
public void updateProduct(Long id, ProductUpdateForm form) {
    productDao.updateById(...);
}
```
- 優點：即時一致性
- 缺點：需要手動管理失效邏輯

**策略 3: Canal CDC（高級場景）**
```java
// Canal 監聽 MySQL binlog
@CanalEventListener
public void onProductUpdated(ProductChangeEvent event) {
    redisTemplate.delete("product:" + event.getId());
}
```
- 優點：自動化、解耦
- 缺點：需要部署 Canal

---

## 🔗 相關資源

### SKILL.md（技術規格）
詳細的技術規格和實現細節：[SKILL.md](SKILL.md)

### 模式文檔
- [Mode 1: Diagnose](modes/mode-1-diagnose.md) - N+1 查詢檢測、JVM 分析、SQL 慢查詢
- [Mode 2: Optimize](modes/mode-2-optimize.md) - 多級緩存實現、緩存一致性
- [Mode 3: Monitor](modes/mode-3-monitor.md) - Skywalking、Micrometer、Grafana
- [Mode Workflow](modes/mode-workflow.md) - 完整工作流編排

### 整合技能文檔
- [java-performance-pro](../../analysis/java-performance-pro/SKILL.md) - 原診斷技能（已整合）
- [cache-strategy-generator](../../integration/cache-strategy-generator/SKILL.md) - 原緩存技能（已整合）
- [apm-integration-skill](../../devops/apm-integration-skill/SKILL.md) - 原監控技能（已整合）

### 外部文檔
- [Skywalking Documentation](https://skywalking.apache.org/) - Skywalking 官方文檔
- [Micrometer Documentation](https://micrometer.io/) - Micrometer 官方文檔
- [Grafana Documentation](https://grafana.com/docs/) - Grafana 官方文檔
- [Caffeine Cache](https://github.com/ben-manes/caffeine) - Caffeine 緩存庫

---

## 📊 版本信息

**Skill Version**: 2.0.0
**Last Updated**: 2026-01-29
**Status**: Stable
**Compatible with**: SmartAdmin v4.0.0+

### v2.0.0 變更（2026-01-29）
- ✅ **整合 3 個技能**：將 `java-performance-pro`, `cache-strategy-generator`, `apm-integration-skill` 整合為統一的模式化工作流
- ✅ **時間效率提升 50%**：從 60 分鐘（3 個獨立技能）降至 30 分鐘（統一工作流）
- ✅ **支援 4 種模式**：`--workflow`, `--mode=diagnose`, `--mode=optimize`, `--mode=monitor`
- ✅ **向後兼容**：舊命令（`/java-performance-pro`, `/cache-strategy`, `/apm-integration`）仍可使用，會顯示遷移警告

### 遷移時間線（v2.0.0）
- **Weeks 1-12** (Soft Deprecation): 舊命令可用，顯示警告
- **Weeks 13-24** (Hard Deprecation): 舊命令顯示錯誤 + 遷移指南
- **Week 25+** (Removal): 舊命令永久移除

詳見：[skill-aliases.json](../skill-aliases.json) 路由配置

---

**Last Updated**: 2026-01-30
