# .agent/skills/ - Antigravity AI Skills

## 定位說明

**重要**：此目錄中的技能專為 **Antigravity 和其他AI助手**（非Claude Code）設計。

### 系統分界

| 目錄 | 目標使用者 | 技能數量 | 範圍 |
|------|-----------|---------|------|
| **.claude/skills/** | Claude Code CLI | 35個 | 通用開發技能（P0-P2） |
| **.agent/skills/** | Antigravity, Gemini等 | 3個 | SmartAdmin專用技能 |

**為什麼分開？**
- Claude Code 有特定的技能格式和調用機制
- Antigravity 和其他AI需要不同的指令結構
- 避免跨系統的複雜性和混淆

## 當前技能清單

### 1. smartadmin-crud-generator
**用途**：生成完整的SmartAdmin CRUD模塊（後端+前端+測試）

**觸發詞**：「生成CRUD」、「新增模塊」

**相關規則**：
- [10-architecture-rules.md](../rules/foundation/10-architecture-rules.md)
- [01-naming-conventions.md](../rules/foundation/01-naming-conventions.md)

### 2. quality-gate-orchestrator
**用途**：多工具品質門檻編排（Checkstyle, PMD, SpotBugs, ArchUnit）

**觸發詞**：「品質檢查」、「SonarQube本地」

**相關規則**：
- [11-checkstyle-rules.md](../rules/quality-tools/11-checkstyle-rules.md)
- [12-pmd-rules.md](../rules/quality-tools/12-pmd-rules.md)

### 3. smartadmin-testing-suite
**用途**：SmartAdmin測試套件（單元測試+整合測試+ArchUnit）

**觸發詞**：「執行測試」、「測試套件」

**相關規則**：
- [16-jacoco-coverage-rules.md](../rules/quality-tools/16-jacoco-coverage-rules.md)
- [ArchitectureTest.java](../configs/ArchitectureTest.java)

## 新增技能指南

### 技能檔案結構
```
.agent/skills/{skill-name}/
├── SKILL.md          (必須)
├── examples/         (可選)
└── templates/        (可選)
```

### SKILL.md 模板
參考現有技能的結構：
- YAML frontmatter（name, description）
- Usage 部分
- Capabilities/Features
- Workflow 步驟
- Related Rules 連結
- Example Session

### 規則整合
所有技能必須參考 `.agent/rules/` 中的相關規則，確保與SmartAdmin架構標準一致。

## 與 .claude/skills/ 的協調

**關鍵原則**：
- ❌ **不要複製**：避免兩個系統中出現重複的技能
- ✅ **明確分工**：Claude Code技能放在 `.claude/`，其他AI技能放在 `.agent/`
- ✅ **規則共享**：兩者都參考統一的 `.agent/rules/` 系統

## 技術債與未來計畫

**已知限制**：
- 目前僅3個技能，相對於 `.claude/` 的35個較少
- 未來可根據Antigravity使用情況擴充

**擴充原則**：
1. 優先開發SmartAdmin專用技能
2. 避免與 `.claude/skills/` 重複
3. 確保與 `.agent/rules/` 架構規則一致
