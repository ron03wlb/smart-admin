# Quality Gate Orchestrator - Examples

## 範例 1: 完整品質檢查流程

**User Request:**
```
執行完整品質檢查
```

**AI Actions:**
```bash
# Step 1: 格式化代碼
./gradlew spotlessApply

# Step 2: Checkstyle 檢查
./gradlew checkstyleMain checkstyleTest

# Step 3: PMD 分析
./gradlew pmdMain

# Step 4: SpotBugs 檢測
./gradlew spotbugsMain

# Step 5: 架構測試
./gradlew test --tests ArchitectureTest

# Step 6: 覆蓋率報告
./gradlew jacocoTestReport
```

**報告輸出:**
```
Quality Gate Results:
├── Spotless: ✅ Applied
├── Checkstyle: ✅ 0 errors
├── PMD: ✅ 0 violations
├── SpotBugs: ✅ 0 bugs
├── ArchUnit: ✅ All tests passed
└── JaCoCo: ✅ 82% coverage
```

## 範例 2: 處理 Checkstyle 錯誤

**錯誤:**
```
[ERROR] EmployeeService.java:45: Line is longer than 120 characters
```

**解決:**
```java
// ❌ 錯誤 - 行太長
public ResponseDTO<PageResult<EmployeeVO>> queryEmployeesByDepartmentAndStatus(EmployeeQueryForm form) {

// ✅ 正確 - 換行
public ResponseDTO<PageResult<EmployeeVO>> queryEmployeesByDepartmentAndStatus(
        EmployeeQueryForm form) {
```

## 範例 3: 處理 PMD 違規

**錯誤:**
```
[PMD] AvoidReassigningParameters - Assignment to parameter 'name'
```

**解決:**
```java
// ❌ 錯誤
public void process(String name) {
    name = name.trim();  // 重新賦值參數
}

// ✅ 正確
public void process(String name) {
    String trimmedName = name.trim();  // 使用本地變數
}
```

## 範例 4: 處理 SpotBugs 警告

**錯誤:**
```
[SpotBugs] EI_EXPOSE_REP - May expose internal representation
```

**解決方案 A (推薦): 添加排除規則**
```xml
<!-- spotbugs-exclude.xml -->
<Match>
    <Class name="~.*VO" />
    <Bug pattern="EI_EXPOSE_REP" />
</Match>
```

**解決方案 B: 防禦性複製**
```java
// ✅ 防禦性複製
public List<String> getItems() {
    return new ArrayList<>(this.items);
}
```

## 範例 5: 處理 ArchUnit 失敗

**錯誤:**
```
Architecture Violation: Service layer uses java.util.Optional
```

**解決:**
```java
// ❌ 違規
import java.util.Optional;
public Optional<EmployeeVO> getById(Long id) { ... }

// ✅ 正確
import io.vavr.control.Option;
public Option<EmployeeVO> getById(Long id) { ... }
```

## 常見品質門檻配置

### Checkstyle 嚴重程度調整

```xml
<!-- 將某些規則降級為警告 -->
<module name="LineLength">
    <property name="max" value="120"/>
    <property name="severity" value="warning"/>
</module>
```

### PMD 規則排除

```xml
<!-- pmd-ruleset.xml -->
<rule ref="category/java/bestpractices.xml">
    <exclude name="JUnitTestsShouldIncludeAssert"/>
</rule>
```
