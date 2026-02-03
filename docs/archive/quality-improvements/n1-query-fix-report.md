# N+1 查詢問題修復報告

**修復日期**: 2026-01-30
**優先級**: P0 (Critical)
**受影響模塊**: OA Notice Module
**修復人**: Claude Sonnet 4.5

---

## 問題描述

### 原始問題 (NoticeService.getUpdateFormVO:228-229)

```java
for (NoticeVisibleRangeVO noticeVisibleRange : noticeVisibleRangeList) {
    if (...) {
        // ✅ 員工數據已批量查詢 (正確)
        EmployeeEntity employeeEntity = employeeMap.get(...);
    } else {
        // ❌ 部門數據在循環中逐個查詢 (N+1問題)
        DepartmentVO departmentVO = departmentDao.selectDepartmentVO(noticeVisibleRange.getDataId());
        noticeVisibleRange.setDataName(...);
    }
}
```

**性能影響**:
- 1000個部門通知 = **1001次數據庫查詢**
  - 1次查詢通知
  - 1000次逐個查詢部門 (N+1問題)

---

## 修復方案

### 1. Service層優化 (NoticeService.java:203-238)

**修復前**:
```java
for (NoticeVisibleRangeVO noticeVisibleRange : noticeVisibleRangeList) {
    if (...員工...) {
        // 員工批量查詢 ✅
    } else {
        // ❌ 逐個查詢部門
        DepartmentVO departmentVO = departmentDao.selectDepartmentVO(noticeVisibleRange.getDataId());
    }
}
```

**修復後**:
```java
// 批量查詢部門數據 (修復N+1查詢問題)
List<Long> departmentIdList = noticeVisibleRangeList.stream()
    .filter(e -> NoticeVisibleRangeDataTypeEnum.DEPARTMENT.getValue().equals(e.getDataType()))
    .map(NoticeVisibleRangeVO::getDataId)
    .collect(Collectors.toList());

Map<Long, DepartmentVO> departmentMap;
if (CollectionUtils.isNotEmpty(departmentIdList)) {
    departmentMap = departmentDao.selectBatchDepartmentVO(departmentIdList).stream()
        .collect(Collectors.toMap(DepartmentVO::getDepartmentId, Function.identity()));
} else {
    departmentMap = Maps.newHashMap();
}

// 填充部門名稱
for (NoticeVisibleRangeVO noticeVisibleRange : noticeVisibleRangeList) {
    if (...部門...) {
        DepartmentVO departmentVO = departmentMap.get(noticeVisibleRange.getDataId());
        noticeVisibleRange.setDataName(...);
    }
}
```

### 2. Dao層新增批量查詢方法 (DepartmentDao.java)

```java
/** 批量查詢部門信息 (修復N+1查詢問題) */
List<DepartmentVO> selectBatchDepartmentVO(@Param("departmentIdList") List<Long> departmentIdList);
```

### 3. MyBatis Mapper新增SQL (DepartmentMapper.xml)

```xml
<select id="selectBatchDepartmentVO"
        resultType="net.lab1024.sa.admin.module.system.department.domain.vo.DepartmentVO">
    SELECT t_department.*,
           t_employee.actual_name   as managerName,
           parent_department.department_name   as parentName
    FROM t_department
             left join t_employee on t_department.manager_id = t_employee.employee_id
             left join t_department parent_department on t_department.parent_id = parent_department.department_id
    where t_department.department_id in
    <foreach collection="departmentIdList" item="item" open="(" separator="," close=")">
        #{item}
    </foreach>
</select>
```

---

## 性能改善

### 數據庫查詢次數對比

| 場景 | 修復前 | 修復後 | 改善 |
|------|--------|--------|------|
| **100個部門** | 101次查詢 | 2次查詢 | ✅ 減少98% |
| **1000個部門** | 1001次查詢 | 2次查詢 | ✅ 減少99.8% |
| **10000個部門** | 10001次查詢 | 2次查詢 | ✅ 減少99.98% |

### 查詢時間對比 (估算)

假設:
- 單次查詢延遲: 5ms
- 批量查詢延遲: 10ms

| 場景 | 修復前 | 修復後 | 改善 |
|------|--------|--------|------|
| **100個部門** | ~505ms | ~15ms | ✅ 快97% |
| **1000個部門** | ~5005ms (5秒) | ~15ms | ✅ 快99.7% |
| **10000個部門** | ~50005ms (50秒) | ~15ms | ✅ 快99.97% |

---

## 測試驗證

### 測試文件: NoticeServiceN1QueryFixTest.java

**測試案例1: 驗證批量查詢**
```java
@Test
@DisplayName("驗證批量查詢部門數據 - 修復N+1查詢問題")
void testBatchQueryDepartments_FixN1Query() {
    // 2個員工 + 3個部門 (共5條記錄)

    // 關鍵驗證: 確保批量查詢只調用一次
    verify(departmentDao, times(1)).selectBatchDepartmentVO(anyList());
    verify(departmentDao, times(0)).selectDepartmentVO(anyLong()); // ✅ 無單個查詢
}
```

**測試案例2: 空部門列表**
```java
@Test
@DisplayName("驗證空部門列表場景 - 不執行批量查詢")
void testEmptyDepartmentList_NoBatchQuery() {
    // 只有員工,沒有部門

    // 驗證沒有調用部門批量查詢
    verify(departmentDao, times(0)).selectBatchDepartmentVO(anyList());
}
```

**測試案例3: 性能測試**
```java
@Test
@DisplayName("性能測試 - 模擬1000個部門通知")
void testPerformance_1000Departments() {
    // 1000個部門

    // 關鍵驗證: 1000個部門只需1次批量查詢
    verify(departmentDao, times(1)).selectBatchDepartmentVO(anyList());
    verify(departmentDao, times(0)).selectDepartmentVO(anyLong());

    // 結果: 查詢1000個部門耗時 < 50ms (Mock環境)
}
```

### 測試執行結果

```bash
./gradlew :sa-admin:test --tests NoticeServiceN1QueryFixTest
```

**結果**: ✅ **3 tests PASSED**
- ✅ 驗證批量查詢部門數據 - 修復N+1查詢問題
- ✅ 驗證空部門列表場景 - 不執行批量查詢
- ✅ 性能測試 - 模擬1000個部門通知

---

## 修改文件清單

### 主要代碼修改 (3個文件)

1. **NoticeService.java** (Service層)
   - 路徑: `sa-admin/src/main/java/.../notice/service/NoticeService.java`
   - 修改: 添加部門批量查詢邏輯 (Line 220-239)

2. **DepartmentDao.java** (Dao層接口)
   - 路徑: `sa-admin/src/main/java/.../department/dao/DepartmentDao.java`
   - 新增: `selectBatchDepartmentVO()` 方法 (Line 28-29)

3. **DepartmentMapper.xml** (MyBatis Mapper)
   - 路徑: `sa-admin/src/main/resources/mapper/system/department/DepartmentMapper.xml`
   - 新增: `selectBatchDepartmentVO` SQL查詢 (Line 34-44)

### 測試文件 (1個文件)

4. **NoticeServiceN1QueryFixTest.java**
   - 路徑: `sa-admin/src/test/java/.../notice/NoticeServiceN1QueryFixTest.java`
   - 新增: 完整的N+1查詢修復驗證測試 (188行)

---

## 驗收標準

### ✅ 所有標準已達成

- [x] **功能正確性**: 部門名稱填充正確,無數據遺漏
- [x] **性能改善**: 1000個部門查詢從1001次 → 2次查詢
- [x] **測試覆蓋**: 3個測試案例全部通過
- [x] **代碼質量**: Spotless格式化通過,無編譯警告
- [x] **向後兼容**: 不影響現有功能

---

## 後續建議

### 短期 (Week 1-2)

1. ✅ **完成** - N+1查詢修復驗證
2. 🔄 **進行中** - 補充Controller層測試
3. ⏳ **待執行** - 消除魔法數字 (LoginService)

### 中期 (Week 3-6)

4. ⏳ 全面掃描其他潛在N+1查詢問題
   - 使用Hibernate Statistics或P6Spy日誌分析
   - 重點檢查循環中的Dao調用

5. ⏳ 建立性能測試基線
   - 添加JMH微基準測試
   - 記錄關鍵查詢的性能指標

### 長期 (Week 7-12)

6. ⏳ 建立N+1查詢檢測機制
   - 集成P6Spy自動檢測
   - 添加CI/CD性能測試門檻

---

## 經驗總結

### ✅ 最佳實踐

1. **批量查詢優先**: 對於集合類型的關聯數據,始終優先使用批量查詢
2. **Map快速查找**: 使用`Collectors.toMap()`將List轉為Map,O(1)查找
3. **空集合處理**: 添加空集合判斷,避免不必要的數據庫調用
4. **測試驗證**: 使用Mockito驗證Dao調用次數,確保批量查詢生效

### ⚠️ 注意事項

1. **MyBatis IN查詢限制**: 某些數據庫對IN子句數量有限制 (Oracle 1000個)
   - 解決方案: 分批查詢或使用臨時表

2. **內存消耗**: 批量查詢會一次性加載所有數據到內存
   - 解決方案: 對於超大數據集使用分頁批量查詢

3. **事務邊界**: 確保批量查詢在同一事務內執行
   - 解決方案: 在Service或Manager層使用`@Transactional`

---

## 參考資料

- **SmartAdmin架構規則**: `.agent/rules/foundation/10-architecture-rules.md`
- **質量標準**: `.claude/shared/knowledge/quality-standards.md`
- **MyBatis批量查詢**: [MyBatis Dynamic SQL](https://mybatis.org/mybatis-dynamic-sql/docs/select.html)

---

**報告版本**: 1.0
**狀態**: ✅ 已完成並驗證
**下一步**: 繼續Week 1-2其他P0任務 (Controller測試、魔法數字消除)
