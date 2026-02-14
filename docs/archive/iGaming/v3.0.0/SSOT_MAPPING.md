# SSOT 映射表 (Single Source of Truth)

**版本**: 1.0.0
**創建日期**: 2026-02-03
**狀態**: ✅ v2.0 重組規範

---

## 📚 什麼是 SSOT？

**Single Source of Truth（唯一真相來源）** 原則確保每個核心概念只在一個地方定義，其他文檔通過引用方式使用，避免內容重複和版本衝突。

**標記規範**：
- **權威定義**：`<!-- SSOT: Authoritative definition of XXX -->`
- **引用位置**：`<!-- Reference: See [Doc §X](path#section) for XXX -->`

---

## 🔑 核心概念映射表

### 財務系統（Financial Loop）

| ID | 概念 | 唯一權威文檔 | 章節 | 其他引用位置 |
|-----|------|------------|------|-------------|
| F-01 | **可下注餘額公式** | [01-02 Wallet_Architecture](01_Core_Financial_Loop_NEW/01-02_Wallet_Architecture.md) | §2.3 | 02-02, 04-01 |
| F-02 | **錢包鎖定邏輯** | [01-02 Wallet_Architecture](01_Core_Financial_Loop_NEW/01-02_Wallet_Architecture.md) | §2.4 | 02-02, 05-01 |
| F-03 | **交易事件驅動架構** | [01-04 Transaction_Flow](01_Core_Financial_Loop_NEW/01-04_Transaction_Flow.md) | §3 | 02-02, 04-01 |
| F-04 | **Outbox Pattern 實作** | [01-04 Transaction_Flow](01_Core_Financial_Loop_NEW/01-04_Transaction_Flow.md) | §3.2 | 07-01 |
| F-05 | **出金審核流程** | [01-05 Withdrawal_Risk](01_Core_Financial_Loop_NEW/01-05_Withdrawal_Risk.md) | §4 | 04-01, 05-04 |
| F-06 | **SAGA 事務補償** | [01-05 Withdrawal_Risk](01_Core_Financial_Loop_NEW/01-05_Withdrawal_Risk.md) | §4.3 | 04-04 |
| F-07 | **對帳模型** | [01-06 Reconciliation](01_Core_Financial_Loop_NEW/01-06_Reconciliation.md) | §2 | 02-03 |

### 遊戲營運（Game Operations）

| ID | 概念 | 唯一權威文檔 | 章節 | 其他引用位置 |
|-----|------|------------|------|-------------|
| G-01 | **Token 驗證流程** | [02-02 Seamless_Wallet_API](02_Game_Operations_NEW/02-02_Seamless_Wallet_API.md) | §4.2 | 07-03, 05-03 |
| G-02 | **冪等性設計** | [02-02 Seamless_Wallet_API](02_Game_Operations_NEW/02-02_Seamless_Wallet_API.md) | §4.3 | 07-03 |
| G-03 | **並發控制（Lua 原子性）** | [02-02 Seamless_Wallet_API](02_Game_Operations_NEW/02-02_Seamless_Wallet_API.md) | §5 | 01-02 |
| G-04 | **有效投注算法** | [02-03 Turnover_Calculation](02_Game_Operations_NEW/02-03_Turnover_Calculation.md) | §3.1 | 04-01, 05-01 |
| G-05 | **流水三層驗證架構** | [02-03 Turnover_Calculation](02_Game_Operations_NEW/02-03_Turnover_Calculation.md) | §2 | 01-06 |
| G-06 | **體育博彩 Valid Bet 計算** | [02-04 Game_Provider_Cases](02_Game_Operations_NEW/02-04_Game_Provider_Cases.md) | §1 | 02-03 |
| G-07 | **老虎機 Free Spins 流水** | [02-04 Game_Provider_Cases](02_Game_Operations_NEW/02-04_Game_Provider_Cases.md) | §2 | 03-01 |

### 活動系統（Promotion System）

| ID | 概念 | 唯一權威文檔 | 章節 | 其他引用位置 |
|-----|------|------------|------|-------------|
| P-01 | **Bonus 計算引擎** | [03-01 Bonus_Engine](03_Promotion_System_NEW/03-01_Bonus_Engine.md) | §2 | 01-02, 04-01 |
| P-02 | **流水要求追蹤** | [03-03 Wagering_Rules](03_Promotion_System_NEW/03-03_Wagering_Rules.md) | §3 | 02-03, 01-06 |
| P-03 | **促銷錢包轉帳邏輯** | [03-03 Wagering_Rules](03_Promotion_System_NEW/03-03_Wagering_Rules.md) | §3.2 | 01-02 |
| P-04 | **VIP 等級計算** | [03-04 VIP_Loyalty](03_Promotion_System_NEW/03-04_VIP_Loyalty.md) | §2 | 01-01 |

### 風控系統（Risk Control）

| ID | 概念 | 唯一權威文檔 | 章節 | 其他引用位置 |
|-----|------|------------|------|-------------|
| R-01 | **風控規則引擎** | [04-01 Risk_Engine](04_Risk_Control_NEW/04-01_Risk_Engine.md) | §2 | 01-05, 02-03 |
| R-02 | **對沖檢測算法** | [04-02 Fraud_Detection](04_Risk_Control_NEW/04-02_Fraud_Detection.md) | §3.1 | 02-04 |
| R-03 | **代理信用額度計算** | [04-03 Agent_Credit_Risk](04_Risk_Control_NEW/04-03_Agent_Credit_Risk.md) | §2.2 | 05-02 |
| R-04 | **風控工作流狀態機** | [04-04 Risk_Workflow](04_Risk_Control_NEW/04-04_Risk_Workflow.md) | §2 | 01-05 |

### 平台治理（Platform Governance）

| ID | 概念 | 唯一權威文檔 | 章節 | 其他引用位置 |
|-----|------|------------|------|-------------|
| S-01 | **多租戶隔離策略** | [05-01 Multi_Tenant_Arch](05_Platform_Governance_NEW/05-01_Multi_Tenant_Arch.md) | §2.1 | 07-02, 05-03 |
| S-02 | **租戶數據分區模型** | [05-01 Multi_Tenant_Arch](05_Platform_Governance_NEW/05-01_Multi_Tenant_Arch.md) | §2.2 | 01-06 |
| S-03 | **RBAC 權限模型** | [05-03 RBAC_Security](05_Platform_Governance_NEW/05-03_RBAC_Security.md) | §2 | 05-06 |
| S-04 | **審計日誌格式** | [05-04 Audit_System](05_Platform_Governance_NEW/05-04_Audit_System.md) | §3 | 05-06 |
| S-05 | **AES-256-GCM 加密策略** | [05-05 Data_Security](05_Platform_Governance_NEW/05-05_Data_Security.md) | §2.1 | 01-02, 05-04 |
| S-06 | **Blind Index 架構** | [05-05 Data_Security](05_Platform_Governance_NEW/05-05_Data_Security.md) | §2.2 | 01-01 |
| S-07 | **Crypto-Shredding（GDPR）** | [05-05 Data_Security](05_Platform_Governance_NEW/05-05_Data_Security.md) | §2.3 | 05-04 |
| S-08 | **Maker-Checker 審批模式** | [05-06 Approval_Workflow](05_Platform_Governance_NEW/05-06_Approval_Workflow.md) | §2 | 01-05 |

### 技術基礎設施（Technical Infrastructure）

| ID | 概念 | 唯一權威文檔 | 章節 | 其他引用位置 |
|-----|------|------------|------|-------------|
| T-01 | **Blue-Green 部署流程** | [07-01 Deployment](07_Technical_Infrastructure_NEW/07-01_Deployment.md) | §3 | 12-04 |
| T-02 | **API 限流算法** | [07-02-02 Rate_Limiting](07_Technical_Infrastructure_NEW/07-02-02_Rate_Limiting.md) | §2 | 02-02 |
| T-03 | **HMAC 簽名驗證** | [07-03-02 Authentication](07_Technical_Infrastructure_NEW/07-03-02_Authentication.md) | §2.1 | 02-02 |
| T-04 | **RESTful 錯誤碼標準** | [07-03-01 Design_Principles](07_Technical_Infrastructure_NEW/07-03-01_Design_Principles.md) | §4 | 所有 API 文檔 |

---

## 📊 統計資訊

| 模塊 | SSOT 概念數量 | 關鍵程度 |
|------|--------------|---------|
| 01_Core_Financial_Loop | 7 | 🔴 極高 |
| 02_Game_Operations | 7 | 🔴 極高 |
| 03_Promotion_System | 4 | 🟠 高 |
| 04_Risk_Control | 4 | 🟠 高 |
| 05_Platform_Governance | 8 | 🔴 極高 |
| 06_Analytics_Operations | 0 | 🟢 低（無 SSOT）|
| 07_Technical_Infrastructure | 4 | 🟠 高 |
| 08_Frontend_CMS | 0 | 🟢 低（無 SSOT）|
| **總計** | **34** | - |

---

## 🔍 如何使用 SSOT 映射表

### 1. 查找權威定義

**場景**：我需要實作可下注餘額計算，但不確定公式在哪裡。

**步驟**：
1. 查閱本映射表，搜索 "可下注餘額"
2. 找到 F-01：權威文檔是 `01-02 Wallet_Architecture.md §2.3`
3. 閱讀該章節的權威定義

### 2. 添加新引用

**場景**：我在寫新文檔，需要引用流水計算邏輯。

**步驟**：
1. 查閱映射表，找到 G-04：`02-03 Turnover_Calculation.md §3.1`
2. 在新文檔中添加引用：
   ```markdown
   <!-- Reference: See [Turnover Calculation §3.1](../02_Game_Operations_NEW/02-03_Turnover_Calculation.md#有效投注算法) -->

   有效投注的計算邏輯詳見 [流水計算文檔 §3.1](../02_Game_Operations_NEW/02-03_Turnover_Calculation.md#有效投注算法)。
   ```
3. **禁止重複定義**：不要在新文檔中再次定義公式

### 3. 驗證 SSOT 一致性

**使用自動化腳本**：
```bash
python scripts/check_ssot_violations.py
```

**檢查項目**：
- 是否有多個文檔聲稱為同一概念的 SSOT
- 是否有引用指向不存在的 SSOT 章節
- 是否有重複定義（未使用 Reference 標記）

---

## 📝 更新日誌

| 日期 | 版本 | 變更說明 |
|------|------|---------|
| 2026-02-03 | 1.0.0 | 初始版本，34 個核心概念 |

---

## 🔗 相關文檔

- [重組計劃](../../../.claude/plans/mutable-kindling-dijkstra.md)
- [遷移對照表](./MIGRATION_MAPPING.md)
- [文檔更新規範](../.github/DOCUMENTATION_STANDARDS.md)

---

**維護團隊**: Architecture Team
**反饋聯繫**: architecture@company.com
