# 命名規範監控指標

**版本**: 1.0.0
**最後更新**: 2026-02-02
**維護團隊**: SmartAdmin Architecture Team

---

## 📊 監控目標

確保 SmartAdmin 代碼庫始終遵循單數命名標準，通過自動化監控及時發現並修正違規。

**核心目標**: `naming_violations_count = 0`

---

## 📈 監控指標定義

### 1. 命名違規計數（Naming Violations Count）

**指標名稱**: `naming_violations_count`
**指標類型**: Counter
**數據源**: ArchUnit 測試結果
**採集頻率**: 每次 CI/CD 運行

**定義**：
- 當前代碼庫中不符合單數命名標準的 Entity 類數量
- 包括所有帶 `@TableName` 註解且使用複數形式的類
- 不包括豁免清單中的表（metrics, statistics, analytics）

**目標值**: 0
**告警閾值**: > 0

**採集方法**：
```bash
# 通過 ArchUnit 測試採集
./gradlew :sa-admin:test --tests ArchitectureTest#tableNameMustBeSingular

# 解析測試結果
violations=$(grep "violated" build/test-results/test/*.xml | wc -l)
echo "naming_violations_count=$violations"
```

---

### 2. 違規修復時間（Violation Fix Time）

**指標名稱**: `violation_fix_time_hours`
**指標類型**: Gauge
**數據源**: GitHub Issues / JIRA
**採集頻率**: 每日

**定義**：
- 從違規被發現到修復完成的平均時間（小時）
- 計算方式：修復 PR 合併時間 - 違規 Issue 創建時間

**目標值**: < 24 小時
**告警閾值**: > 48 小時

**採集方法**：
```bash
# GitHub API 查詢
gh api graphql -f query='
  query {
    repository(owner: "your-org", name: "smart-admin") {
      issues(labels: ["naming-violation"], states: CLOSED, last: 10) {
        nodes {
          createdAt
          closedAt
        }
      }
    }
  }
'
```

---

### 3. PR 阻止率（PR Block Rate）

**指標名稱**: `pr_block_rate_percentage`
**指標類型**: Percentage
**數據源**: GitHub Actions
**採集頻率**: 每週

**定義**：
- 因命名違規被 CI/CD 阻止的 PR 數量 / 總 PR 數量
- 高阻止率表示開發者對命名標準認知不足

**目標值**: < 5%
**告警閾值**: > 10%

**採集方法**：
```bash
# 統計本週 PR 數據
total_prs=$(gh pr list --state all --created "$(date -d '7 days ago' +%Y-%m-%d)" --json number | jq length)
blocked_prs=$(gh run list --workflow naming-check.yml --status failure | wc -l)

block_rate=$((blocked_prs * 100 / total_prs))
echo "pr_block_rate_percentage=$block_rate"
```

---

### 4. 豁免表數量（Exemption Count）

**指標名稱**: `exemption_count`
**指標類型**: Gauge
**數據源**: 01-naming-conventions.md
**採集頻率**: 每月

**定義**：
- 豁免清單中的表名模式數量
- 監控豁免清單是否過度膨脹

**目標值**: ≤ 5
**告警閾值**: > 10

**採集方法**：
```bash
# 解析豁免清單
exemption_count=$(grep -c "t_.*_" .agent/rules/foundation/01-naming-conventions.md | grep "保持複數" || echo 0)
echo "exemption_count=$exemption_count"
```

---

### 5. 測試覆蓋率（Test Coverage）

**指標名稱**: `naming_test_coverage_percentage`
**指標類型**: Percentage
**數據源**: ArchUnit 測試
**採集頻率**: 每次 CI/CD 運行

**定義**：
- ArchUnit tableNameMustBeSingular 測試覆蓋的 Entity 類數量 / 總 Entity 類數量

**目標值**: 100%
**告警閾值**: < 95%

**採集方法**：
```bash
# 統計 Entity 類總數
total_entities=$(find . -name "*Entity.java" | wc -l)

# 統計測試覆蓋的 Entity 類數
tested_entities=$(./gradlew :sa-admin:test --tests ArchitectureTest#tableNameMustBeSingular --info | grep "checked" | wc -l)

coverage=$((tested_entities * 100 / total_entities))
echo "naming_test_coverage_percentage=$coverage"
```

---

## 📉 監控儀表板配置

### Grafana Dashboard 範例

```json
{
  "dashboard": {
    "title": "SmartAdmin Naming Convention Metrics",
    "panels": [
      {
        "id": 1,
        "title": "Naming Violations Count",
        "type": "singlestat",
        "targets": [
          {
            "expr": "naming_violations_count",
            "refId": "A"
          }
        ],
        "thresholds": "0,1",
        "colors": ["#299c46", "#e24d42", "#e24d42"]
      },
      {
        "id": 2,
        "title": "PR Block Rate (Weekly)",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(pr_block_rate_percentage[7d])",
            "refId": "A"
          }
        ],
        "alert": {
          "conditions": [
            {
              "evaluator": {
                "params": [10],
                "type": "gt"
              }
            }
          ]
        }
      },
      {
        "id": 3,
        "title": "Violation Fix Time (Hours)",
        "type": "gauge",
        "targets": [
          {
            "expr": "avg(violation_fix_time_hours)",
            "refId": "A"
          }
        ],
        "thresholds": {
          "mode": "absolute",
          "steps": [
            { "color": "green", "value": 0 },
            { "color": "yellow", "value": 24 },
            { "color": "red", "value": 48 }
          ]
        }
      },
      {
        "id": 4,
        "title": "Exemption Count Trend",
        "type": "graph",
        "targets": [
          {
            "expr": "exemption_count",
            "refId": "A"
          }
        ],
        "alert": {
          "conditions": [
            {
              "evaluator": {
                "params": [10],
                "type": "gt"
              }
            }
          ]
        }
      }
    ]
  }
}
```

---

## 🔔 告警規則配置

### Prometheus Alert Rules

```yaml
groups:
  - name: naming_convention_alerts
    interval: 1h
    rules:
      # Rule 1: 命名違規告警
      - alert: NamingViolationDetected
        expr: naming_violations_count > 0
        for: 1h
        labels:
          severity: critical
          team: architecture
        annotations:
          summary: "Naming convention violations detected"
          description: "{{ $value }} Entity classes violate singular naming standard"
          runbook_url: "https://wiki.example.com/runbooks/naming-violation"

      # Rule 2: PR 阻止率過高
      - alert: HighPRBlockRate
        expr: pr_block_rate_percentage > 10
        for: 1w
        labels:
          severity: warning
          team: architecture
        annotations:
          summary: "High PR block rate due to naming violations"
          description: "{{ $value }}% of PRs blocked by naming checks (threshold: 10%)"
          action: "Schedule team training session"

      # Rule 3: 豁免清單膨脹
      - alert: ExemptionListGrowth
        expr: exemption_count > 10
        for: 1d
        labels:
          severity: warning
          team: architecture
        annotations:
          summary: "Exemption list growing too large"
          description: "{{ $value }} exemptions (threshold: 10)"
          action: "Review exemption list and remove unnecessary entries"

      # Rule 4: 測試覆蓋率下降
      - alert: TestCoverageDecreased
        expr: naming_test_coverage_percentage < 95
        for: 1d
        labels:
          severity: warning
          team: qa
        annotations:
          summary: "ArchUnit test coverage dropped below 95%"
          description: "Current coverage: {{ $value }}%"
          action: "Update ArchUnit tests to cover new Entity classes"
```

---

## 📊 CI/CD 集成

### GitHub Actions Metrics 輸出

在 `naming-check.yml` 中添加指標輸出：

```yaml
- name: Export Metrics
  if: always()
  run: |
    # 採集違規數量
    violations=$(grep -c "violated" build/test-results/test/*.xml || echo 0)

    # 輸出到 GitHub Actions 摘要
    echo "### Naming Convention Metrics" >> $GITHUB_STEP_SUMMARY
    echo "" >> $GITHUB_STEP_SUMMARY
    echo "| Metric | Value |" >> $GITHUB_STEP_SUMMARY
    echo "|--------|-------|" >> $GITHUB_STEP_SUMMARY
    echo "| Violations Count | $violations |" >> $GITHUB_STEP_SUMMARY
    echo "| Target | 0 |" >> $GITHUB_STEP_SUMMARY
    echo "| Status | $([ $violations -eq 0 ] && echo '✅ PASS' || echo '❌ FAIL') |" >> $GITHUB_STEP_SUMMARY

    # 輸出到環境變量（可被後續步驟讀取）
    echo "NAMING_VIOLATIONS_COUNT=$violations" >> $GITHUB_ENV
```

### Slack 告警集成

```yaml
- name: Send Slack Notification
  if: failure()
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {
        "text": "🚨 Naming Convention Violation Detected",
        "blocks": [
          {
            "type": "header",
            "text": {
              "type": "plain_text",
              "text": "Naming Convention Check Failed"
            }
          },
          {
            "type": "section",
            "fields": [
              {
                "type": "mrkdwn",
                "text": "*PR:* <${{ github.event.pull_request.html_url }}|#${{ github.event.number }}>"
              },
              {
                "type": "mrkdwn",
                "text": "*Violations:* ${{ env.NAMING_VIOLATIONS_COUNT }}"
              },
              {
                "type": "mrkdwn",
                "text": "*Author:* ${{ github.actor }}"
              }
            ]
          },
          {
            "type": "actions",
            "elements": [
              {
                "type": "button",
                "text": {
                  "type": "plain_text",
                  "text": "View Details"
                },
                "url": "${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}"
              }
            ]
          }
        ]
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## 📈 月度報告生成

### 自動化報告腳本

```bash
#!/bin/bash
# generate-monthly-report.sh

REPORT_DATE=$(date +%Y-%m)
REPORT_FILE="docs/monitoring/reports/naming-convention-report-$REPORT_DATE.md"

cat > "$REPORT_FILE" <<EOF
# 命名規範監控月報 - $REPORT_DATE

**生成日期**: $(date +%Y-%m-%d)
**報告週期**: $REPORT_DATE

---

## 📊 指標摘要

| 指標 | 本月 | 上月 | 趨勢 |
|------|------|------|------|
| 違規數量 | $(get_metric naming_violations_count) | $(get_metric_last_month naming_violations_count) | $(get_trend naming_violations_count) |
| PR 阻止率 | $(get_metric pr_block_rate_percentage)% | $(get_metric_last_month pr_block_rate_percentage)% | $(get_trend pr_block_rate_percentage) |
| 平均修復時間 | $(get_metric violation_fix_time_hours)h | $(get_metric_last_month violation_fix_time_hours)h | $(get_trend violation_fix_time_hours) |
| 豁免表數量 | $(get_metric exemption_count) | $(get_metric_last_month exemption_count) | $(get_trend exemption_count) |

---

## 🎯 達標情況

- $(check_target naming_violations_count 0) 違規數量 = 0
- $(check_target pr_block_rate_percentage 5) PR 阻止率 < 5%
- $(check_target violation_fix_time_hours 24) 修復時間 < 24h
- $(check_target exemption_count 5) 豁免數量 ≤ 5

---

## 📝 問題與改進

### 本月發現的問題

$(list_issues_this_month)

### 改進行動

$(list_improvements_this_month)

---

**下次審閱**: $(date -d "+1 month" +%Y-%m-%d)
EOF

echo "Report generated: $REPORT_FILE"
```

---

## 📋 定期審查清單

### 每週審查（週五下午）

- [ ] 檢查本週違規數量
- [ ] 確認所有違規已修復
- [ ] 檢查 PR 阻止率是否正常
- [ ] 確認 CI/CD 檢查運行正常

### 每月審查（月末最後一天）

- [ ] 生成月度報告
- [ ] 分析趨勢變化
- [ ] 識別重複問題
- [ ] 規劃改進行動
- [ ] 更新豁免清單（如需要）

### 季度審查（每季度最後一天）

- [ ] 全面審查命名標準合理性
- [ ] 評估豁免清單必要性
- [ ] 檢查自動化工具有效性
- [ ] 更新文檔和培訓材料
- [ ] 規劃下季度改進

---

## 🔍 故障排查

### 問題 1: 指標數據缺失

**症狀**: 監控儀表板顯示無數據

**排查步驟**:
1. 確認 CI/CD workflow 是否正常運行
2. 檢查指標採集腳本是否執行
3. 驗證數據推送到監控系統

**解決方案**:
```bash
# 手動觸發指標採集
gh workflow run naming-check.yml

# 檢查最近的 workflow 運行
gh run list --workflow naming-check.yml --limit 5
```

### 問題 2: 告警誤報

**症狀**: 收到告警但實際無違規

**排查步驟**:
1. 檢查豁免清單配置
2. 驗證 ArchUnit 測試邏輯
3. 確認指標採集邏輯

**解決方案**:
- 更新豁免清單
- 修正 ArchUnit 測試條件
- 調整告警閾值

---

## 📚 相關文檔

- [命名規範規則](../../.agent/rules/foundation/01-naming-conventions.md) - Section 7
- [ArchUnit 測試](../../.agent/configs/ArchitectureTest.java) - tableNameMustBeSingular
- [CI/CD Workflow](../../.github/workflows/naming-check.yml)
- [遷移報告](../audit/NAMING-CONVENTION-MIGRATION-REPORT.md)

---

## 版本歷史

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-02-02 | 初始版本：定義 5 個監控指標和告警規則 |

---

**維護團隊**: SmartAdmin Architecture Team
**下次審閱**: 2026-03-02（每月審閱）
