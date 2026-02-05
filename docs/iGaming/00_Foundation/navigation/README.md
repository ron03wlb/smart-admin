# iGaming 文檔導航中心

歡迎來到 iGaming 平台文檔！根據您的角色和需求選擇合適的導航方式。

## 按角色導航（推薦）

**業務角色**:
- [風控人員](by-role/risk-officer.md) - 風控規則、對沖檢測、異常調查
- [審核人員](by-role/approval-officer.md) - 出金審批、活動審核、投注爭議
- [商戶管理者](by-role/tenant-admin.md) - 玩家管理、代理管理、活動配置
- [品牌管理者](by-role/brand-admin.md) - 多租戶財務匯總、風控策略
- [老闆儀表板](by-role/boss-dashboard.md) - KPI、GGR/NGR、營收報表

**運營角色**:
- [支付運營](by-role/payment-ops.md) - PSP 對帳、手動補償、通道切換
- [遊戲運營](by-role/game-ops.md) - 遊戲上下架、GP 參數配置、RTP 監控
- [活動運營](by-role/promo-ops.md) - 活動配置、標籤系統、Bonus 發放

**技術角色**:
- [開發人員](by-role/developer.md) - API、架構、集成指南

## 按任務導航

**核心業務流程**:
- [存款流程](by-task/deposit-flow.md) - 存款全路徑（風控 → 入賬 → 對帳）
- [出金流程](by-task/withdrawal-flow.md) - 出金全路徑（風控提案 → 審批 → 支付）
- [投注流程](by-task/betting-flow.md) - 投注全路徑（L1/L2/L3 流水驗證）
- [Bonus 流程](by-task/bonus-flow.md) - Bonus 發放全路徑（發放 → 流水 → 釋放）
- [欺詐調查](by-task/fraud-investigation.md) - 欺詐調查全路徑（檢測 → 調查 → 處置）

## 按模塊導航（技術索引）

技術人員可直接訪問 [模塊索引](by-module/module-index.md) 查看完整模塊列表。

---

## 快速查找

### 我想了解...

**玩家相關**:
- 玩家註冊與 KYC → [02_Player_Center](../../02_Player_Center/)
- 玩家標籤與分群 → [02_Player_Center/02-04_Player_Tags_System.md](../../02_Player_Center/02-04_Player_Tags_System.md)

**財務相關**:
- 存款/出金流程 → [出金流程全路徑](by-task/withdrawal-flow.md)
- 無縫錢包設計 → [02_Finance_Center/seamless-wallet/](../../02_Finance_Center/seamless-wallet/)
- 交易對帳 → [02_Finance_Center/02-04_Transaction_Reconciliation.md](../../02_Finance_Center/02-04_Transaction_Reconciliation.md)

**風控相關**:
- 風控規則引擎 → [05_Risk_Management/](../../05_Risk_Management/)
- 異步風控提案 → [architecture-decisions/012-async-risk-proposal-system.md](../../architecture-decisions/012-async-risk-proposal-system.md)
- 出金風控關聯 → [technical-specs/P1-important/07-withdrawal-risk-correlation.md](../../technical-specs/P1-important/07-withdrawal-risk-correlation.md)

**活動相關**:
- 活動系統設計 → [04_Activity_Center/01-system-design.md](../../04_Activity_Center/01-system-design.md)
- Bonus 引擎 → [04_Activity_Center/](../../04_Activity_Center/)

**技術架構**:
- 架構決策記錄 (ADR) → [architecture-decisions/](../architecture-decisions/)
- 技術規格 → [technical-specs/](../technical-specs/)

---

## 文檔使用指南

### 適合業務人員

如果您是業務人員（風控、審核、運營、管理者），推薦使用 **按角色導航**：
1. 找到您的角色對應的導航文檔
2. 查看「核心職責」和「關鍵文檔路徑」
3. 根據「常見任務」快速定位到具體操作指南

### 適合技術人員

如果您是技術人員（開發、測試、運維），推薦使用 **按模塊導航**：
1. 訪問 [模塊索引](by-module/module-index.md)
2. 找到相關的技術模塊（如「02_Finance_Center」）
3. 查看模塊內的詳細技術文檔和 API 規格

### 跨模塊任務

如果您需要了解跨多個模塊的完整業務流程，使用 **按任務導航**：
- 每個任務導航文檔會串聯所有相關模塊
- 提供端到端的流程說明和關鍵決策點
- 標注各角色在流程中的觸達點

---

**最後更新**: 2026-02-03
**維護者**: Architecture Team
**反饋**: 如有文檔問題或改進建議，請提交 Issue
