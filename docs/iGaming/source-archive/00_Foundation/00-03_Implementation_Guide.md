# 00-00 實作指南 (Implementation Guide)

**版本**: 4.0.0
**創建日期**: 2026-02-03
**狀態**: ✅ 索引文件 (Index Document)

---

## 📋 文檔目的

本文檔為**任務導向的實作指南索引**，幫助開發者快速找到"我要實作 XXX"的完整閱讀路徑。

**適用對象**：
- 後端開發工程師
- 全端開發工程師
- 技術架構師
- 系統整合工程師

**使用方式**：
1. 找到你的開發任務類別
2. 點擊對應的子文檔鏈接
3. 按順序閱讀推薦文檔
4. 參考實作步驟和代碼範例

---

## 📚 子文檔導航

| 類別 | 子文檔 | 涵蓋任務 | 說明 |
|------|--------|----------|------|
| **財務流程** | [00-00-01_Financial_Implementation.md](implementation-guides/00-00-01_Financial_Implementation.md) | 1-4 | 錢包系統、支付對接、出金風控、對帳系統 |
| **遊戲營運** | [00-00-02_Game_Integration_Implementation.md](implementation-guides/00-00-02_Game_Integration_Implementation.md) | 5-7 | 遊戲廠商對接、Seamless Wallet、流水計算 |
| **活動系統** | [00-00-03_Promotion_Implementation.md](implementation-guides/00-00-03_Promotion_Implementation.md) | 8-10 | Bonus 引擎、流水追蹤、VIP 系統 |
| **風控系統** | [00-00-04_Risk_Implementation.md](implementation-guides/00-00-04_Risk_Implementation.md) | 11-13 | 風控規則、欺詐檢測、代理信用 |
| **平台治理** | [00-00-05_Governance_Implementation.md](implementation-guides/00-00-05_Governance_Implementation.md) | 14-17 | 多租戶、RBAC、審計日誌、數據加密 |
| **技術基礎設施** | [00-00-06_Infrastructure_Implementation.md](implementation-guides/00-00-06_Infrastructure_Implementation.md) | 18-20 | API 閘道、Blue-Green 部署、限流機制 |

---

## 📖 任務快速索引

### 核心財務流程 → [Financial Implementation](implementation-guides/00-00-01_Financial_Implementation.md)

| # | 任務 | 關鍵技術 |
|---|------|----------|
| 1 | 實作錢包系統 | Redis Lua、樂觀鎖、可下注餘額公式 |
| 2 | 對接支付閘道 | Webhook、冪等性、狀態機 |
| 3 | 實作出金風控流程 | 風控規則、延遲檢查、審批流程 |
| 4 | 建立對帳系統 | 三層驗證、差異報告、自動修復 |

### 遊戲營運 → [Game Integration](implementation-guides/00-00-02_Game_Integration_Implementation.md)

| # | 任務 | 關鍵技術 |
|---|------|----------|
| 5 | 對接新遊戲廠商 | Provider Adapter、Token 驗證、回調處理 |
| 6 | 實作 Seamless Wallet API | 冪等性、並發控制、結算流程 |
| 7 | 設計流水計算邏輯 | 三層驗證、遊戲權重、有效投注 |

### 活動系統 → [Promotion Implementation](implementation-guides/00-00-03_Promotion_Implementation.md)

| # | 任務 | 關鍵技術 |
|---|------|----------|
| 8 | 建立 Bonus 發放引擎 | LiteFlow 規則、錢包隔離、過期處理 |
| 9 | 設計流水要求追蹤 | 進度計算、遊戲權重、完成判定 |
| 10 | 實作 VIP 等級系統 | 積分計算、升降級規則、權益發放 |

### 風控系統 → [Risk Implementation](implementation-guides/00-00-04_Risk_Implementation.md)

| # | 任務 | 關鍵技術 |
|---|------|----------|
| 11 | 建立風控規則引擎 | 配置驅動、規則優先級、動態更新 |
| 12 | 實作欺詐檢測算法 | 行為分析、ML 模型、風險評分 |
| 13 | 設計代理信用管理 | 信用額度、結算週期、風險控制 |

### 平台治理 → [Governance Implementation](implementation-guides/00-00-05_Governance_Implementation.md)

| # | 任務 | 關鍵技術 |
|---|------|----------|
| 14 | 實作多租戶架構 | Schema 隔離、資源配額、租戶上下文 |
| 15 | 設計 RBAC 權限系統 | Sa-Token、權限碼、數據權限 |
| 16 | 建立審計日誌系統 | AOP 攔截、敏感數據脫敏、合規報告 |
| 17 | 實作數據加密策略 | SM2/SM3/SM4、字段加密、密鑰管理 |

### 技術基礎設施 → [Infrastructure Implementation](implementation-guides/00-00-06_Infrastructure_Implementation.md)

| # | 任務 | 關鍵技術 |
|---|------|----------|
| 18 | 設計 API 閘道 | 路由規則、認證統一、限流熔斷 |
| 19 | 建立 Blue-Green 部署 | K8s Deployment、流量切換、回滾策略 |
| 20 | 實作 API 限流機制 | 滑動窗口、令牌桶、租戶配額 |

---

## 🔗 相關文檔

- **快速開始**: [00-01_Quickstart.md](./00-01_Quickstart.md)
- **概念與指南**: [guides/](./guides/)
- **架構決策**: [../adr/](../adr/)

---

**文檔類型**: 索引文件
**子文檔數量**: 6
**最後更新**: 2026-02-07
