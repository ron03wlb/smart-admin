---
description: 失敗重試與問題追蹤機制
---

## 編譯失敗自動診斷
```yaml
- name: Build with Diagnosis
  id: build
  continue-on-error: true
  run: mvn -B compile 2>&1 | tee build.log

- name: Analyze Failures
  if: steps.build.outcome == 'failure'
  run: |
    echo "## 🔴 編譯失敗診斷報告" >> $GITHUB_STEP_SUMMARY
    
    # 符號找不到
    if grep -q "cannot find symbol" build.log; then
      echo "### ❌ 符號缺失" >> $GITHUB_STEP_SUMMARY
      echo "**修復建議**: 檢查 import 語句和依賴聲明" >> $GITHUB_STEP_SUMMARY
      grep -A3 "cannot find symbol" build.log >> $GITHUB_STEP_SUMMARY
    fi
    
    # 包不存在
    if grep -q "package .* does not exist" build.log; then
      echo "### ❌ 依賴缺失" >> $GITHUB_STEP_SUMMARY
      echo "**修復建議**: 執行 mvn dependency:resolve" >> $GITHUB_STEP_SUMMARY
    fi
    
    # 類型不匹配
    if grep -q "incompatible types" build.log; then
      echo "### ❌ 類型不匹配" >> $GITHUB_STEP_SUMMARY
      echo "**修復建議**: 檢查泛型和類型轉換" >> $GITHUB_STEP_SUMMARY
    fi
```

## 測試失敗根因分析
```yaml
- name: Test Failure Analysis
  if: failure()
  run: |
    echo "## 🔴 測試失敗分析" >> $GITHUB_STEP_SUMMARY
    
    # 解析 Surefire 報告
    for report in target/surefire-reports/*.txt; do
      if grep -q "FAILURE" "$report"; then
        echo "### 失敗測試: $(basename $report .txt)" >> $GITHUB_STEP_SUMMARY
        grep -A10 "FAILURE" "$report" >> $GITHUB_STEP_SUMMARY
      fi
    done
```

## Quality Gate 失敗報告格式
```json
{
  "status": "ERROR",
  "project": "my-spring-app",
  "timestamp": "2026-01-10T10:30:00Z",
  "failedConditions": [
    {
      "metric": "new_coverage",
      "actual": "65.5%",
      "required": "80%",
      "gap": "-14.5%",
      "files": ["UserService.java", "OrderController.java"]
    },
    {
      "metric": "new_bugs",
      "actual": 3,
      "required": 0,
      "issues": [
        {"rule": "S2259", "file": "PaymentService.java:42", "message": "NPE 風險"}
      ]
    }
  ],
  "recommendations": [
    "添加 UserService 的單元測試，當前無測試覆蓋",
    "修復 PaymentService 第 42 行的空指針風險"
  ]
}
```

## 迭代修復循環