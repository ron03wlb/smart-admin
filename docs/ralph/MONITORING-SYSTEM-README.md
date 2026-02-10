# Ralph Loop 智能監控與休息系統

**版本**: 1.0.0
**創建日期**: 2026-02-10
**狀態**: ✅ Production Ready

## 概述

Ralph Loop 智能監控系統為 SmartAdmin 的 Ralph Wiggum 24小時自主優化循環提供：
- ✅ **持續運行**: 24小時無人值守自主工作
- ✅ **智能監控**: 每30分鐘自動檢查 API 使用量
- ✅ **動態休息**: 根據多維度風險評分調整休息時長 (0-60分鐘)
- ✅ **5小時保護**: 避免觸發 Claude API 的5小時使用限制
- ✅ **Ultrathink 模式**: 深度分析和決策記錄能力

## 系統架構

```
Ralph Loop (ralph-igaming-docs.sh)
    │
    ├─→ [每次迭代] usage-tracker.js
    │   └─→ 記錄 token 使用量到 metrics/YYYY-MM-DD-HHmm.json
    │
    ├─→ [每30分鐘] quota-manager.js
    │   └─→ 聚合數據 → quota.json
    │
    ├─→ [每30分鐘] threshold-checker.sh
    │   └─→ 返回決策: 0=繼續 / 1=休息 / 2=停止
    │
    └─→ [休息時] rest-strategy.js
        └─→ 計算休息分鐘數 (0-60)
```

### 風險評分公式

```
risk_score =
  (token_usage_pct × 0.40) +      # Token使用率 40%
  (api_call_rate × 0.30) +        # API調用頻率 30%
  (failure_rate × 0.15) +         # 失敗率 15%
  (five_hour_proximity × 0.15)    # 接近5小時限制 15%
```

### 休息策略映射

| 風險評分 | 休息時長 | 說明 |
|---------|---------|------|
| 0.0-0.3 | 0分鐘 | 低風險，繼續運行 |
| 0.3-0.6 | 5-10分鐘 | 中風險，短暫休息 |
| 0.6-0.8 | 15-30分鐘 | 高風險，適度休息 |
| 0.8-1.0 | 45-60分鐘 | 嚴重，長時間休息 |

## 文件結構

```
docs/ralph/
├── scripts/                          # 監控腳本目錄
│   ├── usage-tracker.js              # Token使用量記錄器 (200行)
│   ├── quota-manager.js              # 配額管理器 (180行)
│   ├── threshold-checker.sh          # 閾值檢查器 (100行)
│   ├── rest-strategy.js              # 休息策略計算器 (150行)
│   └── test-monitoring-system.sh     # 端到端測試腳本
├── metrics/                          # 使用量數據存儲
│   └── YYYY-MM-DD-HHmm.json         # 時間戳JSON文件
├── reports/                          # 聚合報告目錄
├── logs/                             # 日誌目錄
├── thresholds.yml                    # 閾值配置文件 (200行)
├── ralph-igaming-docs.sh             # 主循環腳本 (已增強)
├── PROMPT.md                         # 提示詞 (已添加Ultrathink)
├── progress.md                       # 任務進度追蹤
├── guardrails.md                     # 經驗教訓記錄
└── MONITORING-SYSTEM-README.md       # 本文件
```

## 快速開始

### 1. 驗證配置

```bash
# 查看配置文件
cat docs/ralph/thresholds.yml

# 驗證所有組件存在
ls -la docs/ralph/scripts/
```

### 2. 測試運行 (3次迭代)

```bash
cd /c/Workspace/open_source/smart-admin
bash docs/ralph/ralph-igaming-docs.sh 3
```

**預期行為**:
- 每次迭代會記錄 token 使用量到 `metrics/` 目錄
- 每30分鐘會生成 `quota.json` 並檢查閾值
- 如果風險評分 > 0.6，會自動休息

### 3. 生產環境運行

```bash
# 在 tmux 中後台運行 (推薦)
tmux new-session -d -s ralph-igaming \
  "bash docs/ralph/ralph-igaming-docs.sh 120"

# 查看即時日誌
tmux attach -t ralph-igaming

# 或者直接後台運行
nohup bash docs/ralph/ralph-igaming-docs.sh 120 > ralph-output.log 2>&1 &
```

## 監控和調試

### 查看當前配額狀態

```bash
# 查看最新配額數據
cat docs/ralph/quota.json | head -30

# 查看風險評分
node -p "JSON.parse(require('fs').readFileSync('docs/ralph/quota.json', 'utf8')).risk_score"
```

### 查看任務進度

```bash
# 查看已完成/待處理任務
cat docs/ralph/progress.md | grep -E "\[x\]|\[ \]" | head -10

# 統計進度
echo "已完成: $(grep -c '\[x\]' docs/ralph/progress.md)"
echo "待處理: $(grep -c '\[ \]' docs/ralph/progress.md)"
```

### 查看日誌

```bash
# 查看最新日誌
tail -50 docs/ralph/logs/*.log

# 查看休息事件
grep "REST triggered" docs/ralph/logs/*.log

# 查看錯誤
grep -i error docs/ralph/logs/*.log
```

### 查看使用量數據

```bash
# 查看最新的 metrics 文件
ls -lt docs/ralph/metrics/*.json | head -5

# 查看某個 metrics 文件內容
cat docs/ralph/metrics/$(ls docs/ralph/metrics/*.json | tail -1)
```

## 配置說明

### thresholds.yml 關鍵配置

```yaml
# API 層級 (tier-1 到 tier-4)
api_tier: "tier-4"

# Token 使用閾值 (百分比)
token_thresholds:
  input:
    moderate: 60    # 60% - 開始密切監控
    high: 80        # 80% - 建議休息
    critical: 90    # 90% - 強制休息

# 監控間隔 (秒)
monitoring:
  quota_check_interval_seconds: 1800  # 30分鐘

# 休息策略
rest_strategy:
  risk_bands:
    moderate:       # 風險 0.3-0.6
      min_minutes: 5
      max_minutes: 10
    high:           # 風險 0.6-0.8
      min_minutes: 15
      max_minutes: 30
    critical:       # 風險 0.8-1.0
      min_minutes: 45
      max_minutes: 60

# 5小時窗口保護
five_hour_window:
  enabled: true
  warning_threshold_minutes: 270    # 4.5小時警告
  critical_threshold_minutes: 285   # 4.75小時強制停止
```

### 針對不同 API Tier 的建議配置

| API Tier | Input TPM | 建議閾值 | 建議監控間隔 |
|----------|-----------|---------|-------------|
| tier-1 | 50k | moderate: 50%, high: 70%, critical: 85% | 15分鐘 |
| tier-2 | 100k | moderate: 55%, high: 75%, critical: 88% | 20分鐘 |
| tier-3 | 200k | moderate: 58%, high: 78%, critical: 90% | 25分鐘 |
| tier-4 | 400k | moderate: 60%, high: 80%, critical: 90% | 30分鐘 |

## Ultrathink 模式

在 PROMPT.md 中已添加 Ultrathink 模式，用於深度分析和決策記錄。

### 何時使用 Ultrathink

- 不確定選擇哪種方法
- 存在多個有效解決方案
- 需要理解為什麼之前的嘗試失敗
- 需要權衡不同方案的利弊

### Ultrathink 輸出格式

```markdown
## Ultrathink: Should I link Risk_Strategy to Risk_System_Architecture?

**Context**: ...

**Analysis**:
1. ...
2. Option A: ...
   - Pros: ...
   - Cons: ...
3. Option B: ...

**Decision**: ...

**Reasoning**: ...

**Confidence**: High (9/10)
```

## 故障排查

### 問題1：監控腳本未執行

**症狀**: `metrics/` 目錄為空，沒有生成 JSON 文件

**原因**: usage-tracker.js 執行失敗

**解決**:
```bash
# 手動測試 usage-tracker.js
echo "Input tokens: 10000" | node docs/ralph/scripts/usage-tracker.js \
  --iteration=1 --duration=30 --log-file=/tmp/test.json

# 檢查錯誤
cat /tmp/test.json
```

### 問題2：休息時間異常

**症狀**: 休息時間過長或過短

**原因**: 風險評分計算錯誤或配置錯誤

**解決**:
```bash
# 查看當前風險評分
cat docs/ralph/quota.json | grep risk_score

# 手動測試休息策略
echo '{"risk_score": 0.7}' > /tmp/test-quota.json
node docs/ralph/scripts/rest-strategy.js --quota-file=/tmp/test-quota.json
```

### 問題3：5小時限制觸發

**症狀**: Claude API 返回 "5-hour limit" 錯誤

**原因**: 未能及時休息

**解決**:
```bash
# 降低閾值（更保守）
# 在 thresholds.yml 中：
# token_thresholds:
#   input:
#     moderate: 50  # 從 60 降到 50
#     high: 70      # 從 80 降到 70

# 或者縮短監控間隔
# monitoring:
#   quota_check_interval_seconds: 900  # 從 1800 (30分鐘) 改為 900 (15分鐘)
```

### 問題4：Bash 腳本 bc 命令不存在

**症狀**: `rest-strategy.sh: line 67: bc: command not found`

**解決**: 使用 Node.js 版本（已自動整合）
```bash
# ralph-igaming-docs.sh 已自動使用 Node.js 版本
# 不需要額外操作
```

## 成功指標

### 功能性指標

- **監控準確性**: 95%+ token 數據正確
- **休息決策正確率**: 90%+ 避免 API 限制
- **5小時限制避免率**: 100% 不觸發

### 性能指標

- **監控開銷**: < 1% 迭代總時間
- **數據聚合速度**: < 5秒 (90天數據)
- **配額檢查延遲**: < 2秒

### 業務指標

- **持續運行時長**: 24小時不中斷
- **任務完成率**: 80%+ 任務24小時內完成
- **休息時間佔比**: 10-20% 總時間

## 最佳實踐

### 1. 定期檢查日誌

```bash
# 每天檢查一次
grep -E "(CRITICAL|ERROR|REST triggered)" docs/ralph/logs/*.log | tail -20
```

### 2. 調整配置

根據實際使用情況調整 `thresholds.yml`:
- 如果經常觸發休息 → 提高閾值
- 如果觸發5小時限制 → 降低閾值

### 3. 監控數據備份

```bash
# 每週備份 metrics 數據
tar -czf ralph-metrics-$(date +%Y%m%d).tar.gz docs/ralph/metrics/
```

### 4. 定期清理舊數據

```bash
# 刪除90天前的 metrics 文件
find docs/ralph/metrics/ -name "*.json" -mtime +90 -delete
```

## 進階功能 (Phase 2 - 可選)

### Phase 2: 通知與分析

- [ ] 實現 `aggregate-usage.js` 數據聚合器
- [ ] 實現 `notify.sh` (Telegram/Slack 通知)
- [ ] 生成每日/週使用量統計報告

### Phase 3: 可視化

- [ ] 創建 HTML 儀表板 (`usage-dashboard.html`)
- [ ] 整合 Chart.js 繪製趨勢圖
- [ ] 實現自動刷新邏輯

## 技術細節

### 數據流

```
每次迭代:
1. Ralph 執行 Claude 提示詞 → 獲得輸出 (含 token 數據)
2. usage-tracker.js 記錄到 metrics/YYYY-MM-DD-HHmm.json

每 30 分鐘:
3. quota-manager.js 聚合最近 30 分鐘數據 → 生成 quota.json
4. threshold-checker.sh 檢查閾值 → 返回 exit code
5. 如果需要休息:
   - rest-strategy.js 計算休息時長
   - sleep N 分鐘
6. 恢復執行
```

### Token 計算

```javascript
// 有效 token 數 (考慮 cached token 折扣)
effective_tokens = input_tokens + output_tokens - (cached_tokens × 0.9)

// 使用率百分比
token_usage_pct = (input_tokens / max_tokens_in_window) × 100
```

### 風險評分詳解

```javascript
// 多維度風險評估
risk_score =
  (token_usage_pct / 100 × 0.40) +     // 40% 權重
  (api_call_rate_pct / 100 × 0.30) +   // 30% 權重
  (failure_rate × 0.15) +              // 15% 權重
  (five_hour_proximity × 0.15)         // 15% 權重

// 範圍: 0.0 (無風險) ~ 1.0 (極高風險)
```

## 版本歷史

### v1.0.0 (2026-02-10)

**Phase 1 (P0) - 基礎監控系統**

✅ **已實現功能**:
- Token 使用量記錄器 (`usage-tracker.js`)
- 配額管理器 (`quota-manager.js`)
- 閾值檢查器 (`threshold-checker.sh`)
- 休息策略計算器 (`rest-strategy.js`)
- 配置文件 (`thresholds.yml`)
- 主循環增強 (`ralph-igaming-docs.sh`)
- Ultrathink 模式 (`PROMPT.md`)
- 跨平台兼容 (Windows/Linux/macOS)

✅ **測試狀態**:
- 所有核心組件測試通過
- 跨平台兼容性驗證完成

## 支持與反饋

如遇問題或有改進建議，請參考：
1. 本 README 的故障排查章節
2. 檢查 `docs/ralph/logs/` 中的日誌
3. 查看計劃文件 `C:\Users\ron.chang\.claude\plans\wobbly-roaming-muffin.md`

## 授權

本監控系統是 SmartAdmin 項目的一部分，遵循 SmartAdmin 的開源授權協議。

---

**最後更新**: 2026-02-10
**維護者**: SmartAdmin Team
**版本**: 1.0.0 - Production Ready
