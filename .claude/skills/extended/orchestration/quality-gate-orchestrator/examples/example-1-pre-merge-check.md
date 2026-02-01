# Example 1: Pre-Merge Quality Gate

## Scenario
合併前自動執行全套質量檢查（Checkstyle + PMD + SpotBugs + ArchUnit）

## Input
```bash
User: "Run quality gate before merging PR"
```

## Generated Command
```bash
./gradlew qualityGate

# Orchestrates:
# 1. ./gradlew checkstyleMain
# 2. ./gradlew pmdMain
# 3. ./gradlew spotbugsMain
# 4. ./gradlew :sa-admin:test --tests ArchitectureTest
```

## Expected Result
```
Quality Gate Report:
✅ Checkstyle: PASSED (0 violations)
✅ PMD: PASSED (0 violations)
✅ SpotBugs: PASSED (0 bugs)
✅ ArchUnit: PASSED (all rules)

Overall: ✅ PASSED - Safe to merge
```
