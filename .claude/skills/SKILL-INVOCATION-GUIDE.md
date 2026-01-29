# Skills 調用指南（系統層註冊不完整時的解決方案）

**問題**：部分 skills 雖然在專案層配置完整（skill-registry.yml、config.yml），但 Claude Code 系統層未註冊，導致 Skill tool 無法直接調用。

**影響範圍**：以下 8 個 skills 暫時無法使用 `/skill-name` 語法直接調用：
- igame-pm-analyst
- igaming-multi-tenant-wallet-pm
- archunit-test-generator
- fraud-detection-pattern-generator
- quality-gate-orchestrator
- batch-plan-executor
- (其他未註冊 skills)

---

## 📋 當前可用的 3 種調用方式

### 方式 1：直接讀取 SKILL.md（推薦）✅

**語法**：
```markdown
請讀取 .claude/skills/{priority}/{category}/{skill-name}/SKILL.md
並根據其指引執行 [任務描述]
```

**範例 A - igame-pm-analyst**：
```markdown
用戶：
請讀取 .claude/skills/extended/domain/igame-pm-analyst/README.md
並分析以下需求：

【需求】VIP 系統升級
- 新增鑽石 VIP 等級（存款 >= 100 萬）
- 每日返水提升到 1.5%
- 專屬客服經理
- 生日禮金 5000 元

請生成：
1. PRD 文檔（繁體中文）
2. 風險評估（資金安全/性能/合規）
3. SmartAdmin 分層設計建議
```

**範例 B - igaming-multi-tenant-wallet-pm**：
```markdown
用戶：
請讀取 .claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/SKILL.md
並執行 Phase 3（無縫錢包設計）：

【需求】整合 5 個遊戲供應商
- Evolution Gaming（真人）
- Pragmatic Play（老虎機）
- BTi Sports（體育）
- Asia Gaming（真人）
- BBIN（全平台）

設計要求：
- 單一錢包餘額
- 即時額度轉換（< 100ms）
- 支援並發 1000+ TPS
- 資金安全隔離

請生成：
1. 無縫錢包架構圖（Mermaid）
2. 時序圖（存款/提款/轉帳流程）
3. SmartAdmin Manager 層設計
```

**優點**：
- ✅ 立即可用，完整功能
- ✅ 所有 SKILL.md 定義的能力都可使用
- ✅ 支援多階段執行（phase-based skills）
- ✅ 繁體中文輸出

**缺點**：
- 需要輸入較長的路徑
- 無法享受 Skill tool 的自動觸發機制

---

### 方式 2：使用短別名（需補充 skill-aliases.json）⚠️

**前提**：需要先在 skill-aliases.json 中添加別名配置（見下方配置）。

**語法**：
```markdown
/igame-pm <任務描述>
/wallet-pm <任務描述>
```

**配置步驟**：

1. 編輯 `.claude/skills/skill-aliases.json`
2. 在 `aliases` 對象中添加：

```json
{
  "aliases": {
    // ... 現有別名 ...

    "igame-pm": {
      "target": "extended/domain/igame-pm-analyst",
      "deprecated": false,
      "message": "✅ iGaming PM 分析助手\n\n用法：\n  /igame-pm analyze <需求>\n  /igame-pm prd <功能名稱>\n\n文檔：.claude/skills/extended/domain/igame-pm-analyst/README.md",
      "since": "2026-01-29"
    },

    "wallet-pm": {
      "target": "extended/domain/igaming-multi-tenant-wallet-pm",
      "deprecated": false,
      "message": "✅ iGaming 多商戶錢包 PM\n\n用法：\n  /wallet-pm --phase=1  (需求收集)\n  /wallet-pm --phase=3  (無縫錢包設計)\n\n文檔：.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/SKILL.md",
      "since": "2026-01-29"
    }
  }
}
```

**範例**：
```markdown
用戶：/igame-pm analyze 需要開發風控系統，攔截可疑交易
用戶：/wallet-pm --phase=2 設計多商戶隔離策略
```

**狀態**：⚠️ 需要手動配置 skill-aliases.json（專案層配置）

---

### 方式 3：使用已註冊的相關 Skill 部分替代

**igame-feature-builder**（已註冊 ✅）可以部分替代：

```markdown
用戶：/igame-feature-builder 實作 VIP 系統
```

**差異對比**：

| Aspect | igame-pm-analyst | igame-feature-builder |
|--------|------------------|----------------------|
| **定位** | 產品經理（需求分析） | 技術實作（代碼生成） |
| **輸出物** | PRD 文檔、風險評估 | Java 代碼、測試 |
| **語言** | 繁體中文 | 英文 + 代碼註解 |
| **階段** | 需求分析 → 架構設計 | 技術實作 → 代碼生成 |
| **適用場景** | 需求不明確時 | 需求已明確，直接實作 |

**建議工作流**：
```
1. 需求分析階段（缺失）
   └─ 暫時方案：手動讀取 igame-pm-analyst/README.md

2. 技術實作階段（可用）
   └─ 使用：/igame-feature-builder
```

---

## 🔧 完整配置步驟（解決長期問題）

### Step 1：補充專案層別名配置（用戶可執行）

編輯文件：`.claude/skills/skill-aliases.json`

**添加內容**（在 `aliases` 對象中）：
```json
"igame-pm": {
  "target": "extended/domain/igame-pm-analyst",
  "deprecated": false,
  "message": "✅ iGaming PM 分析助手 - 需求分析、PRD 生成、風險評估",
  "since": "2026-01-29"
},

"wallet-pm": {
  "target": "extended/domain/igaming-multi-tenant-wallet-pm",
  "deprecated": false,
  "message": "✅ iGaming 多商戶錢包 PM 專家",
  "since": "2026-01-29"
},

"archunit": {
  "target": "foundation/backend/archunit-test-generator",
  "deprecated": false,
  "message": "✅ ArchUnit 測試生成器",
  "since": "2026-01-29"
},

"fraud": {
  "target": "extended/domain/fraud-detection-pattern-generator",
  "deprecated": false,
  "message": "✅ iGaming 風控模式生成器",
  "since": "2026-01-29"
},

"quality-gate": {
  "target": "extended/quality/quality-gate-orchestrator",
  "deprecated": false,
  "message": "✅ 質量門禁編排器",
  "since": "2026-01-29"
},

"batch": {
  "target": "extended/orchestration/batch-plan-executor",
  "deprecated": false,
  "message": "✅ 批量計劃執行器",
  "since": "2026-01-29"
}
```

**驗證**：
```bash
# 檢查 JSON 格式是否正確
cat .claude/skills/skill-aliases.json | jq .
```

---

### Step 2：等待系統層註冊（需 Claude Code 支援）

**無法由用戶執行**，需要 Claude Code 團隊在系統配置中添加：

```python
# Claude Code 系統層配置（示意）
# 實際文件路徑由 Claude Code 管理

AVAILABLE_SKILLS = [
    # ... 現有 24 個 skills ...

    # 新增缺少的 8 個 skills
    {
        "name": "igame-pm-analyst",
        "path": ".claude/skills/extended/domain/igame-pm-analyst",
        "triggers": ["iGame", "包網", "博弈", "需求分析", "PRD"],
        "description": "iGaming PM 分析助手 - 需求分析、PRD 生成、風險評估"
    },
    {
        "name": "igaming-multi-tenant-wallet-pm",
        "path": ".claude/skills/extended/domain/igaming-multi-tenant-wallet-pm",
        "triggers": ["multi-tenant", "white-label", "無縫錢包", "KYC/AML"],
        "description": "iGaming 多商戶錢包 PM 專家"
    },
    # ... 其他 6 個 skills ...
]
```

**狀態**：⏳ 等待 Claude Code 團隊更新系統配置

---

## 📊 配置完成度對比

| 配置層級 | 文件/組件 | 狀態 | 完成度 |
|---------|----------|------|--------|
| **專案層** | skill-registry.yml | ✅ | 100% (29/29) |
| **專案層** | config.yml (×29) | ✅ | 100% (29/29) |
| **專案層** | 目錄結構 | ✅ | 100% |
| **專案層** | 文檔（README, CLAUDE.md） | ✅ | 100% |
| **專案層** | skill-aliases.json | ⚠️ | 70% (缺 8 個別名) |
| **系統層** | Claude Code Skill Tool | ❌ | 76% (24/32 註冊) |

---

## 🎯 建議行動優先級

### 立即執行（今天）
1. ✅ **使用方式 1**（直接讀取 SKILL.md）- 立即可用，無需等待
2. ⚠️ **補充 skill-aliases.json**（可選）- 改善用戶體驗

### 短期（1-2 週）
3. 📧 **反映給 Claude Code 團隊** - 提交系統層註冊請求
4. 📊 **監控使用情況** - 追蹤別名使用頻率

### 長期（1 個月+）
5. ✅ **系統層更新完成** - Claude Code 團隊註冊缺少的 skills
6. 🔄 **遷移到標準語法** - 從方式 1 遷移到 `/skill-name` 語法

---

## 📞 支援與反饋

**問題回報**：
- GitHub Issues: https://github.com/anthropics/claude-code/issues
- 標題：`[Skill Registration] Missing 8 skills in system-level Skill tool`

**文檔參考**：
- [skill-registry.yml](.claude/skills/skill-registry.yml) - Skills 元數據（SSOT）
- [MIGRATION-REPORT-v3.0.0.md](.claude/skills/MIGRATION-REPORT-v3.0.0.md) - v3.0.0 遷移報告
- [README.md](.claude/skills/README.md) - Skills catalog v3.0.0

---

**版本**: 1.0.0
**更新日期**: 2026-01-29
**維護者**: SmartAdmin Architecture Team
**狀態**: ✅ 可用（暫時性解決方案已就緒）
