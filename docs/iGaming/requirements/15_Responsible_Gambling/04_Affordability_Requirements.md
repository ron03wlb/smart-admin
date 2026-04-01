# 可負擔性評估需求（Affordability Assessment Requirements）— RG 視角

> **SSOT 文件**：`05_Risk_Compliance/09_Affordability_Requirements.md` 為可負擔性評估的唯一事實來源（SSOT），包含完整的業務規則（閾值、觸發條件、審查週期）。
> 詳見：[Affordability 需求（SSOT）](../05_Risk_Compliance/09_Affordability_Requirements.md)

本文件僅保留**負責任博弈（RG）視角**的特有內容，不重複定義已在 SSOT 中定義的業務規則。

---

## 1. RG 模組職責說明

可負擔性評估（Affordability Assessment）在 RG 模組中的職責定位如下：

- **SSOT（05_Risk_Compliance）**：負責定義監管要求、觸發閾值（GBP 125 / 500 / 2,000 年度淨損失；GBP 150 月度淨存款）、評估等級、第三方驗證、有效期與合規報告。
- **本文件（15_Responsible_Gambling RG 視角）**：聚焦 RG 工具整合——即可負擔性評估結果如何驅動玩家保護行動（警告訊息、限額建議、強制限額、脆弱性偵測介入）。

> 完整業務規則、監管摘要、驗收標準請參閱 [Affordability 需求（SSOT）](../05_Risk_Compliance/09_Affordability_Requirements.md)。

---

## 2. RG 視角：評估結果驅動的保護行動

可負擔性評估結果觸發以下 RG 工具行動（詳細評估邏輯見 SSOT）：

| 評估結果 | RG 保護行動 | 相關 RG 工具 |
|---------|-----------|-------------|
| Basic（警告） | 顯示遊戲中 RG 警告訊息 | 現實提醒（Reality Check） |
| Enhanced（自我申報） | 顯示自我申報表單，計算建議存款限額 | 存款限制（Deposit Limits） |
| Full PASSED | 顯示建議限額，玩家可接受或設定更低限額 | 存款限制（Deposit Limits） |
| Full FAILED | 強制套用存款限額，無法覆蓋 | 存款限制（Deposit Limits）+ 帳戶限制 |
| 脆弱性 HIGH | 觸發完整評估 + 發送關懷訊息 | 主動關懷（Proactive Care） |
| 脆弱性 MEDIUM | 顯示 RG 警告訊息 | 現實提醒（Reality Check） |

---

## 3. RG 與 Risk Engine 的整合邊界

```
可負擔性評估觸發（Risk Engine / 05_Risk_Compliance 負責）
    ↓
評估結果通知至 RG 模組
    ↓
RG 模組執行保護行動（本文件範圍）
    ↓
玩家限額更新、通知發送、Gamstop 同步
```

- **觸發邏輯（何時觸發）** → [Affordability 需求（SSOT）](../05_Risk_Compliance/09_Affordability_Requirements.md)
- **執行策略（如何執行）** → 本文件及 [Player_Protection_API.md](../../architecture/15_Responsible_Gambling/04_Player_Protection_API.md)

---

## 4. 相關文件

| 文件 | 說明 |
|------|------|
| [Affordability 需求（SSOT）](../05_Risk_Compliance/09_Affordability_Requirements.md) | 完整業務規則 |
| [Player_Protection_API.md](../../architecture/15_Responsible_Gambling/04_Player_Protection_API.md) | 技術架構 |
| [Deposit_Limits_Requirements.md](02_Deposit_Limits_Requirements.md) | 存款限制需求 |
| [Self_Exclusion_Requirements.md](01_Self_Exclusion_Requirements.md) | 自我排除需求 |

---

**Navigation**: [Responsible Gambling Module](../15_Responsible_Gambling/) | [iGaming Home](../../README.md)
