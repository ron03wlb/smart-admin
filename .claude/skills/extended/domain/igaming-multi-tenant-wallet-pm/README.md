# iGaming Multi-Tenant Wallet PM Expert - Quick Reference

**Version**: 1.0.0
**Priority**: P1 (Important - Business Logic & Quality)
**Phase-Based**: Yes (4 Phases)

---

## 🎯 Quick Start

### 最常見使用示例

```bash
# 完整流程（所有階段，55-75 分鐘）
"設計多商戶 VIP 系統，支持白標定制，歐洲牌照合規"
→ 執行: Phase 1-4 (需求收集 → 多商戶設計 → 無縫錢包設計 → 地區合規)

# 單階段執行（20-25 分鐘）
"無縫錢包對接 Evolution Gaming"
→ 執行: Phase 3 (無縫錢包設計)

# 地區合規查詢（10-15 分鐘）
"歐洲市場 KYC 合規要求對比表"
→ 執行: Phase 4 (地區合規)
```

### 輸出物範例

- ✅ 標準化繁體中文 PRD 文檔（6 章節結構）
- ✅ 多種 Mermaid 圖表（flowchart, sequenceDiagram, erDiagram, C4 architecture）
- ✅ SmartAdmin 分層設計方案（Controller → Service → Manager → Dao）
- ✅ 地區合規對比表（歐洲/亞洲/美洲/中國）
- ✅ 風險評估矩陣（資金安全/性能/合規三維）

---

## 📚 Documentation Structure

```
igaming-multi-tenant-wallet-pm/
├── SKILL.md                          # 核心文檔（~1000 行）
├── README.md                         # 本文件 - 快速參考
│
├── phases/                           # 4 階段執行文檔
│   ├── phase-1-requirement-gathering.md      (~600 行)
│   ├── phase-2-multi-tenant-design.md        (~700 行)
│   ├── phase-3-seamless-wallet-design.md     (~800 行)
│   └── phase-4-regional-compliance.md        (~500 行)
│
├── knowledge/                        # 知識庫（核心參考資料）
│   ├── regional-requirements.md      # 地區合規對比表（~800 行）
│   ├── wallet-patterns.md            # 無縫錢包 13 個專題整合（~1500 行）
│   ├── multi-tenant-patterns.md      # 多商戶模式整合（~600 行）
│   ├── prd-template.md               # 增強型 PRD 模板（~400 行）
│   └── mermaid-best-practices.md     # Mermaid 圖表策略（~300 行）
│
├── examples/                         # 3 個完整案例
│   ├── example-1-multi-tenant-vip-system.md
│   ├── example-2-seamless-wallet-integration.md
│   └── example-3-regional-compliance-kyc.md
│
└── references/                       # 參考資料
    ├── ultrathink-methodology.md     # Ultrathink 方法論詳解
    ├── igaming-glossary.md           # iGaming 術語表（多語言）
    └── smartadmin-integration.md     # SmartAdmin 集成模式
```

---

## 🚀 核心能力（7 個核心模式）

| 核心能力 | 說明 | 文檔參考 |
|---------|------|---------|
| **Regional Compliance Mapping** | 自動生成歐洲/亞洲/美洲/中國的牌照、KYC、稅率對比表 | [knowledge/regional-requirements.md](knowledge/regional-requirements.md) |
| **Multi-Tenant Architecture Design** | 多商戶隔離策略（Schema/行級/完全隔離）與白標定制 | [knowledge/multi-tenant-patterns.md](knowledge/multi-tenant-patterns.md) |
| **Seamless Wallet Pattern Generation** | 無縫錢包核心模式（13 個專題整合） | [knowledge/wallet-patterns.md](knowledge/wallet-patterns.md) |
| **Ultrathink Methodology** | 第一性原理深度分析框架 | [references/ultrathink-methodology.md](references/ultrathink-methodology.md) |
| **PRD Document Generation** | 標準化繁體中文 PRD 生成（6 章節結構） | [knowledge/prd-template.md](knowledge/prd-template.md) |
| **Mermaid Diagram Strategy** | 5 種圖表類型的場景化應用 | [knowledge/mermaid-best-practices.md](knowledge/mermaid-best-practices.md) |
| **Risk Assessment Framework** | 資金安全/性能/合規三維風險評估 | [SKILL.md §模式 7](SKILL.md) |

---

## 🔄 Phase-Based Execution（4 階段執行）

### Phase 1: Requirement Gathering（需求收集）

**目標**: 收集用戶需求,明確功能範圍和約束條件

**輸入**: 用戶描述的需求（自然語言）

**輸出**:
- 需求分析文檔（PRD §1 + §2）
- 用戶角色定義表
- 功能需求清單
- 非功能性需求清單

**時間估計**: 10-15 分鐘

**詳細文檔**: [phases/phase-1-requirement-gathering.md](phases/phase-1-requirement-gathering.md)

---

### Phase 2: Multi-Tenant Design（多商戶設計）

**目標**: 設計多商戶隔離策略與白標定制方案

**輸入**:
- Phase 1 的需求分析文檔
- 租戶數量（< 10, 10-100, > 100）
- 安全要求（高/中/低）

**輸出**:
- 多商戶架構設計文檔（PRD §3.1）
- 租戶數據模型（Mermaid erDiagram）
- 租戶配置表 DDL
- MyBatis 攔截器示例代碼

**時間估計**: 15-20 分鐘

**詳細文檔**: [phases/phase-2-multi-tenant-design.md](phases/phase-2-multi-tenant-design.md)

---

### Phase 3: Seamless Wallet Design（無縫錢包設計）

**目標**: 設計無縫錢包 API 規格與流水計算邏輯

**輸入**:
- Phase 2 的多商戶架構設計文檔
- 遊戲供應商列表（如 Evolution Gaming, Pragmatic Play）
- 錢包類型（單錢包/多錢包）

**輸出**:
- 錢包 API 規格文檔（Bet, Settle, Rollback）
- Token 驗證決策樹（Mermaid flowchart）
- Bet API 時序圖（Mermaid sequenceDiagram）
- 冪等性設計文檔
- 數據庫 DDL（t_wallet_transaction）

**時間估計**: 20-25 分鐘

**詳細文檔**: [phases/phase-3-seamless-wallet-design.md](phases/phase-3-seamless-wallet-design.md)

---

### Phase 4: Regional Compliance（地區合規）

**目標**: 生成地區合規要求對比表與風險評估

**輸入**:
- Phase 3 的錢包設計文檔
- 目標地區列表（歐洲/亞洲/美洲/中國）

**輸出**:
- 地區合規對比表（PRD §6.1）
- 合規風險評估（PRD §6.2）
- 緩解方案（PRD §6.4）
- 地區選擇決策樹（Mermaid flowchart）

**時間估計**: 10-15 分鐘

**詳細文檔**: [phases/phase-4-regional-compliance.md](phases/phase-4-regional-compliance.md)

---

## 📋 無縫錢包 13 個專題索引

| 編號 | 專題 | 優先級 | 使用場景 | 文檔參考 |
|------|------|-------|---------|---------|
| 01 | Token 驗證決策樹 | P0 | API 身份驗證 | [wallet-patterns.md §1](knowledge/wallet-patterns.md) |
| 02 | 冪等性分層設計 | P0 | 防止重複扣款 | [wallet-patterns.md §2](knowledge/wallet-patterns.md) |
| 03 | 體育博彩邏輯 | P1 | 體育遊戲流水計算 | [wallet-patterns.md §3](knowledge/wallet-patterns.md) |
| 04 | 免費旋轉流水 | P1 | 老虎機活動 | [wallet-patterns.md §4](knowledge/wallet-patterns.md) |
| 05 | 輪盤對沖檢測 | P1 | 風控檢測 | [wallet-patterns.md §5](knowledge/wallet-patterns.md) |
| 06 | 百家樂平局邏輯 | P2 | 桌面遊戲流水 | [wallet-patterns.md §6](knowledge/wallet-patterns.md) |
| 07 | 流水並發累積 | P0 | 高併發場景 | [wallet-patterns.md §7](knowledge/wallet-patterns.md) |
| 08 | 會計條目修正 | P1 | 財務合規 | [wallet-patterns.md §8](knowledge/wallet-patterns.md) |
| 09 | 對賬模型分離 | P1 | 財務對賬 | [wallet-patterns.md §9](knowledge/wallet-patterns.md) |
| 10 | 錯誤恢復場景 | P0 | 異常處理 | [wallet-patterns.md §10](knowledge/wallet-patterns.md) |
| 11 | 流水要求追蹤 | P1 | 紅利流水 | [wallet-patterns.md §11](knowledge/wallet-patterns.md) |
| 12 | 紅利錢包轉賬 | P1 | 錢包間轉賬 | [wallet-patterns.md §12](knowledge/wallet-patterns.md) |

**快速查詢**:
- 第三方遊戲對接：模式 01 + 02 + 10 (P0)
- 體育博彩平台：模式 03 + 07 (P1)
- 老虎機活動：模式 04 + 11 (P1)
- 風控檢測：模式 05 (P1)
- 財務對賬：模式 08 + 09 (P1)

---

## 🌍 地區合規快速參考

| 地區 | 牌照類型 | KYC 嚴格度 | 稅率 | 處理時間 | 推薦場景 |
|-----|---------|----------|------|---------|---------|
| 🇪🇺 **歐洲 MGA** | 馬爾他博彩管理局 | ⭐⭐⭐⭐⭐ 嚴格 | 5% GGR | 6-12 個月 | 合規要求高的歐洲市場 |
| 🇨🇼 **Curacao** | Curacao eGaming | ⭐⭐ 寬鬆 | 固定費用 | 1-3 個月 | 快速上線,預算有限 |
| 🇵🇭 **菲律賓 PAGCOR** | 菲律賓娛樂博彩公司 | ⭐⭐⭐ 中等 | 5% GGR | 3-6 個月 | 亞洲市場主流選擇 |
| 🇺🇸 **內華達州** | Nevada Gaming Control Board | ⭐⭐⭐⭐⭐ 最嚴格 | 6.75% GGR | 12-24 個月 | 美國合法市場 |
| 🇨🇷 **哥斯達黎加** | 自我監管 | ⭐ 最寬鬆 | 無稅 | 1 個月 | 離岸運營（風險高） |
| 🇲🇴 **澳門** | 澳門博彩監察協調局 | ⭐⭐⭐⭐ 嚴格 | 39% GGR | 特許制 | 實體賭場為主 |

**詳細對比**: [knowledge/regional-requirements.md](knowledge/regional-requirements.md)

---

## ✅ PRD 質量檢查清單

使用本 Skill 生成 PRD 後，請完成以下檢查:

### 內容完整性

- [ ] 包含 Ultrathink 深度分析（§4 章節）
- [ ] 包含至少 2 種 Mermaid 圖表（flowchart + sequenceDiagram）
- [ ] 明確 SmartAdmin 分層設計（§3.2）
- [ ] 評估三維風險（資金/性能/合規，§6）
- [ ] 列出 Foundation 模組依賴
- [ ] 包含數據庫 DDL（§3.3）

### 多租戶設計

- [ ] 明確隔離策略（Schema/行級/完全隔離）
- [ ] 定義租戶配置項清單（功能開關、配額限制）
- [ ] 設計租戶識別機制（JWT Token / HTTP Header / Subdomain）
- [ ] MyBatis 攔截器自動注入 tenant_id
- [ ] 所有業務表包含 tenant_id 欄位

### 無縫錢包設計

- [ ] Token 驗證策略明確（Bet API vs Result API）
- [ ] 冪等性機制完整（Redis + DB + 分佈式鎖）
- [ ] 餘額計算公式正確（Playable Balance）
- [ ] 流水累積邏輯正確（Lua 腳本原子性）
- [ ] Bet/Settle/Rollback API 完整
- [ ] 數據庫 DDL 包含 tenant_id

### 地區合規

- [ ] 地區對比表完整（歐洲/亞洲/美洲/中國）
- [ ] KYC 要求明確（分級：1-5）
- [ ] AML 要求明確（大額交易閾值）
- [ ] 稅率計算正確（GGR 稅率）
- [ ] 合規風險評估完整（風險等級 + 緩解方案）

---

## 🚫 常見錯誤與反模式

### 反模式 1: 多租戶數據洩漏

❌ **錯誤做法**：未在 WHERE 子句添加 tenant_id 過濾

```sql
-- 危險！可能查詢到其他租戶的數據
SELECT * FROM t_wallet_transaction WHERE player_id = 12345;
```

✅ **正確做法**：MyBatis 攔截器自動注入 tenant_id

```sql
-- 自動注入（由 TenantInterceptor 處理）
SELECT * FROM t_wallet_transaction
WHERE player_id = 12345 AND tenant_id = 1 AND deleted_flag = 0;
```

**詳細說明**: [SKILL.md §反模式 1](SKILL.md)

---

### 反模式 2: 無縫錢包非冪等

❌ **錯誤做法**：transaction_id 未緩存，可能重複扣款

✅ **正確做法**：三層冪等性防護

1. **Layer 1**: Redis 快取（1-5ms）
2. **Layer 2**: 資料庫唯一索引（10-50ms）
3. **Layer 3**: 分散式鎖（Redisson，5-20ms）

**詳細說明**: [SKILL.md §反模式 2](SKILL.md) 或 [knowledge/wallet-patterns.md §2](knowledge/wallet-patterns.md)

---

### 反模式 3: Token 驗證邏輯混淆

❌ **錯誤做法**：Bet API 和 Result API 使用相同的 Token 驗證邏輯

✅ **正確做法**：根據 API 類型和遊戲類型採用不同策略

- **Bet API**: STRICT（嚴格驗證有效期）
- **Result API** (老虎機/桌遊): STRICT（重用 Bet Token）
- **Result API** (體育博彩): CONDITIONAL（Fallback 到注單驗證）

**詳細說明**: [knowledge/wallet-patterns.md §1](knowledge/wallet-patterns.md)

---

### 反模式 4: 流水要求驗證時機錯誤

❌ **錯誤做法**：投注時自動解鎖紅利錢包

✅ **正確做法**：取款時驗證達標才解鎖

```
投注時：僅累積流水進度，不解鎖
取款時：驗證達標才允許取款
```

**詳細說明**: [knowledge/wallet-patterns.md §11](knowledge/wallet-patterns.md)

---

## 📖 與其他 Skills 的關係

### vs igame-pm-analyst

**關係**: **擴展（Extension）**

| Skill | 適用場景 | 專業深度 |
|------|---------|---------|
| **igame-pm-analyst** | 通用 iGaming 需求分析 | ⭐⭐⭐ 中等 |
| **igaming-multi-tenant-wallet-pm** | 多商戶 + 無縫錢包 + 跨地區合規 | ⭐⭐⭐⭐⭐ 極高 |

**自動觸發**：檢測到 "multi-tenant" 或 "seamless wallet" 關鍵詞時優先使用此 Skill

---

## 🔗 相關文檔

### SmartAdmin 文檔

- [SmartAdmin 核心模式](.claude/shared/knowledge/smartadmin-patterns.md)
- [架構規則](.agent/rules/foundation/10-architecture-rules.md)
- [Foundation 模組](.claude/shared/knowledge/project-architecture.md)

### iGaming 文檔

- [無縫錢包分析（13 個專題）](docs/IGaming/02_Finance_Center/seamless-wallet/)
- [多商戶技術指南](docs/plans/tenant/)
- [統一錢包模型](docs/IGaming/02_Finance_Center/02-06_Unified_Wallet_Model.md)

### Skill 開發指南

- [Skills Catalog](.claude/skills/README.md)
- [Skill 開發規範](.claude/skills/skill-development-guide.md)

---

## 📊 版本資訊

- **Version**: 1.0.0
- **Last Updated**: 2026-01-29
- **Author**: SmartAdmin Team
- **Maintained By**: Claude Code Skills Team
- **Priority**: P1 (Important - Business Logic & Quality)

### 變更歷史

| 版本 | 日期 | 變更內容 |
|------|------|---------|
| 1.0.0 | 2026-01-29 | 初始版本發布 |

---

## 🎓 學習路徑

### 新手入門（15 分鐘）

1. 閱讀本文件（README.md）
2. 查看示例：[examples/example-2-seamless-wallet-integration.md](examples/example-2-seamless-wallet-integration.md)
3. 執行單階段：Phase 3 無縫錢包設計

### 中級使用（30 分鐘）

1. 閱讀核心文檔：[SKILL.md](SKILL.md)
2. 閱讀無縫錢包模式：[knowledge/wallet-patterns.md](knowledge/wallet-patterns.md)
3. 執行完整流程：Phase 1-4

### 高級定制（60 分鐘）

1. 研究 Ultrathink 方法論：[references/ultrathink-methodology.md](references/ultrathink-methodology.md)
2. 學習 Mermaid 最佳實踐：[knowledge/mermaid-best-practices.md](knowledge/mermaid-best-practices.md)
3. 定制地區合規要求：[knowledge/regional-requirements.md](knowledge/regional-requirements.md)

---

## 🤝 貢獻指南

如需擴展本 Skill，請遵循以下原則:

1. **保持文檔同步**：修改 SKILL.md 時同步更新 README.md
2. **新增模式**：在 knowledge/wallet-patterns.md 添加新模式（編號 13+）
3. **新增地區**：在 knowledge/regional-requirements.md 添加新地區合規要求
4. **新增示例**：在 examples/ 目錄添加完整案例
5. **版本管理**：遵循語義化版本（Semantic Versioning）

---

## 📧 聯繫方式

- **技術支持**: SmartAdmin Team
- **文檔問題**: 提交 Issue 到 SmartAdmin GitHub
- **功能建議**: 聯繫產品經理團隊

---

**文檔結束**

**下一步**: 閱讀 [SKILL.md](SKILL.md) 了解詳細功能，或直接開始使用！
