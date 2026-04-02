# ADR-016: 交叉引用（XREF）Header 格式三種等效標準

## 狀態

✅ **已接受** (Accepted) — 2026-03-31

---

## 背景 (Context)

在 docs/iGaming 的文件整理（2026-03-31）過程中，發現文件庫中已有三種不同格式的交叉引用 Header：

1. `> **XREF（交叉引用）**:` — 早期格式（2026-02 規範中定義）
2. `> **Related Architecture**:` — 部分 requirements 文件使用
3. `> **Business Requirements**:` — architecture 文件反向引用使用
4. `> **業務需求**:` — 繁體中文等效格式

如果強制將所有文件統一為單一格式，需修改大量已合規文件，產生不必要的 diff noise，且破壞了已建立的文件 SSOT chain。

---

## 決策 (Decision)

**接受以下四種 XREF Header 格式為等效合規（Equivalent Compliant Forms）**：

| 格式 | 使用場合 | 範例 |
|------|---------|------|
| `> **XREF**:` | 通用交叉引用（雙向均適用） | `> **XREF**: [玩家生命週期](../01_Player_Service/01_Player_Lifecycle_Implementation.md)` |
| `> **Related Architecture**:` | requirements 文件 → architecture 前向連結 | `> **Related Architecture**: [Player Protection API](../../architecture/15_Responsible_Gambling/04_Player_Protection_API.md)` |
| `> **Business Requirements**:` | architecture 文件 → requirements 後向連結（英文） | `> **Business Requirements**: [KYC/AML Requirements](../../requirements/05_Risk_Compliance/03_KYC_AML_Requirements.md)` |
| `> **業務需求**:` | architecture 文件 → requirements 後向連結（繁體中文） | `> **業務需求**: [KYC/AML 需求](../../requirements/05_Risk_Compliance/03_KYC_AML_Requirements.md)` |

**關鍵原則**：
- 文件只要有任一格式的 XREF header，即視為已符合交叉引用標準
- 不強制重格式化已合規文件（避免 diff noise）
- 新建文件優先使用 `> **XREF**:` 格式

---

## 後果 (Consequences)

**正面影響**：
- 無需修改 ~400+ 已合規文件，大幅降低維護成本
- 保留各文件的語境意圖（requirements 文件用 "Related Architecture" 更直觀）
- 驗證腳本可用 OR 邏輯同時接受四種格式

**負面影響**：
- 文件格式輕度不一致（可接受，因為語意等效）
- 新進開發者需要了解四種格式均合規

---

## 替代方案（未採用）

**強制統一為 `> **XREF**:` 格式**：需修改 200+ 文件，pure syntax change 且無語義收益，拒絕。

---

**文檔版本**: 1.0.0
**決策日期**: 2026-03-31
**維護團隊**: SmartAdmin Architecture Team

---

**Navigation**: [ADR Index](INDEX.md) | [iGaming Architecture](../) | [STANDARDS.md](../../STANDARDS.md)
