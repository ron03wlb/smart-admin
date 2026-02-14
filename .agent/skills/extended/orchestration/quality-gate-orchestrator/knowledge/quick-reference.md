# Quality Gate Orchestrator - Quick Reference

## 執行順序

```
1. Spotless    → 代碼格式化
2. Checkstyle  → 代碼風格
3. PMD         → 代碼品質
4. SpotBugs    → Bug 檢測
5. ArchUnit    → 架構驗證
6. JaCoCo      → 代碼覆蓋率
```

## 快速命令

```bash
# 完整品質檢查
./gradlew spotlessApply checkstyleMain pmdMain spotbugsMain test jacocoTestReport

# 單獨執行
./gradlew spotlessApply        # 格式化
./gradlew checkstyleMain       # 風格檢查
./gradlew pmdMain              # PMD 分析
./gradlew spotbugsMain         # SpotBugs 檢測
./gradlew test --tests ArchitectureTest  # 架構測試
./gradlew jacocoTestReport     # 覆蓋率報告
```

## 通過標準

| 工具 | 通過條件 |
|------|----------|
| Checkstyle | 0 errors |
| PMD | 0 violations (P1-P3) |
| SpotBugs | 0 bugs |
| ArchUnit | All tests pass |
| JaCoCo | >= 80% coverage |

## 常見 PMD 抑制

```java
// CallSuperInConstructor - 空構造器
@SuppressWarnings("PMD.CallSuperInConstructor")
public MyClass() { }

// AvoidReassigningParameters
@SuppressWarnings("PMD.AvoidReassigningParameters")
public void method(String param) {
    String localParam = param;  // 使用本地變數
}
```

## 常見 SpotBugs 排除

```xml
<!-- spotbugs-exclude.xml -->
<Match>
    <Class name="~.*Form" />
    <Bug pattern="EI_EXPOSE_REP,EI_EXPOSE_REP2" />
</Match>
```

## 報告位置

| 工具 | 報告路徑 |
|------|----------|
| Checkstyle | `build/reports/checkstyle/` |
| PMD | `build/reports/pmd/` |
| SpotBugs | `build/reports/spotbugs/` |
| JaCoCo | `build/reports/jacoco/` |

## 相關規則

- [Q01-checkstyle-rules.md](../../../../rules/quality-tools/Q01-checkstyle-rules.md)
- [Q02-pmd-rules.md](../../../../rules/quality-tools/Q02-pmd-rules.md)
- [Q03-spotbugs-rules.md](../../../../rules/quality-tools/Q03-spotbugs-rules.md)
