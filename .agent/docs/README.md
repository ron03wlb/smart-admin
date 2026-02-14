# .agent/docs/ - 快速參考文檔

## 目錄用途

此目錄包含從 `.agent/rules/` 系統提取的**快速參考卡和摘要文檔**，適合快速查詢和日常使用。

**與 `.agent/rules/` 的關係**：
- `.agent/rules/` = **完整詳細的規則文檔**（25個檔案，用於深入學習）
- `.agent/docs/` = **精煉的快速參考**（3個檔案，用於日常查詢）

**與 `.claude/shared/knowledge/` 的關係**：
- `.claude/shared/knowledge/` = Claude Code專用知識庫
- `.agent/docs/` = Antigravity和通用AI參考文檔
- 內容可能重疊，但格式和定位不同

## 檔案清單

### 1. coding-standards-summary.md
**用途**：SmartAdmin 編碼標準的快速摘要

**涵蓋範圍**：
- 命名約定（類別、方法、變量）
- 架構層次規則（Controller → Service → Manager → Dao）
- 常見模式（ResponseDTO, PageResult, Option）
- 依賴注入規則（@RequiredArgsConstructor）

**何時使用**：
- 快速查詢命名規範
- 確認架構層次呼叫關係
- 提醒常見模式的用法

**對應完整規則**：
- [F01-naming-conventions.md](../rules/foundation/F01-naming-conventions.md)
- [F04-architecture-rules.md](../rules/foundation/F04-architecture-rules.md)

---

### 2. faq-troubleshooting.md
**用途**：常見問題和故障排除指南

**涵蓋範圍**：
- 構建失敗（Gradle）
- ArchUnit測試失敗
- MyBatis-Plus配置問題
- Sa-Token認證問題
- Vavr Option使用錯誤

**何時使用**：
- 遇到構建或測試失敗
- 不確定如何解決特定錯誤
- 需要快速找到解決方案

**對應完整規則**：
- [Q01-checkstyle-rules.md](../rules/quality-tools/Q01-checkstyle-rules.md)
- [F04-architecture-rules.md](../rules/foundation/F04-architecture-rules.md)
- [D04-mybatis-plus-core.md](../rules/technology/database/D04-mybatis-plus-core.md)

---

### 3. translation-glossary.md
**用途**：SmartAdmin 專業術語的中英對照表

**涵蓋範圍**：
- 架構術語（Controller, Service, Manager, Dao）
- 技術組件（SmartAdmin專用工具）
- 業務概念（iGaming領域術語）
- 常見縮寫（DTO, VO, BO, PO）

**何時使用**：
- 撰寫中文文檔時查詢標準譯名
- AI生成程式碼時確保術語一致
- 團隊溝通時統一專業術語

**特殊說明**：
- 優先使用英文命名（程式碼中）
- 中文僅用於註解和文檔

## 使用建議

### 開發階段使用策略

| 階段 | 使用文檔 | 目的 |
|------|---------|------|
| **快速查詢** | `.agent/docs/` | 確認命名、模式、解決常見錯誤 |
| **深入學習** | `.agent/rules/` | 理解架構原理、最佳實踐 |
| **技能執行** | `.agent/skills/` | 自動化CRUD生成、品質檢查 |
| **決策路由** | `.agent/rules/00-INDEX.md` | 選擇正確的規則/技能/工作流 |

### 維護準則

**更新頻率**：
- 當 `.agent/rules/` 中的核心規則更新時
- 發現新的常見問題時（FAQ）
- 新增重要術語時（Glossary）

**一致性保證**：
- 每次更新時在檔案頭部註明「最後更新日期」
- 使用 `grep` 驗證與 `.agent/rules/` 的一致性
- 在 CI/CD 中新增自動化檢查

## 貢獻指南

**新增快速參考**：
1. 確保內容源自 `.agent/rules/` 的正式規則
2. 保持簡潔（每個主題不超過2頁）
3. 新增指向完整規則的連結
4. 更新本 README.md 的檔案清單

**不應包含的內容**：
- ❌ 詳細的架構設計討論（屬於 `.agent/rules/`）
- ❌ 完整的程式碼範例（屬於 `.agent/skills/`）
- ❌ 特定專案的實作細節（屬於專案文檔）
