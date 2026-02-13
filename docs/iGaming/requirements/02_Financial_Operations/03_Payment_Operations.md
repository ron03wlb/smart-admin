# 支付營運（Payment Operations）

> **Canonical Source**: [source-archive/02_Finance_Center/02-02_Payment_Gateway_Integration.md](../../source-archive/02_Finance_Center/02-02_Payment_Gateway_Integration.md)
> **受眾**: 高階主管、產品經理、合規官
> **相關架構**: [Payment_Gateway_Technical.md](../../architecture/02_Finance_Service/06_Payment_Gateway_Technical.md)
> **最後同步**: 2026-02-09
>
> **精簡說明**: 技術細節（PSP webhook 實作、簽章驗證演算法、智慧路由程式碼、排程對帳任務、連線池配置、Prometheus 指標）已移至架構層。本文件僅聚焦於業務規則。

---

## 業務價值（Business Value）

本支付營運系統提供以下價值：
- **支付成功率優化**: 智慧路由演算法根據成功率（50% 權重）、交易費用（30% 權重）、結算速度（15% 權重）動態選擇最佳 PSP，將掉單率從業界平均 3-5% 降至目標 <1%
- **成本效益**: 多 PSP 競爭與動態通道切換，相較於單一供應商設置，每筆交易費用降低 15-30%，尤其對高額 VIP 玩家效果顯著
- **合規遵循**: 確保 PCI-DSS Level 1 合規（不儲存卡片資料）、PSD2 3D Secure 強制要求（歐盟）、加密貨幣支付的錢包地址風險評分（AML）

## 成功指標（Success Metrics）

| 指標 | 目標 | 衡量方式 |
|------|------|----------|
| 掉單率（Drop Rate） | <1% | (入款成功數 / 總成功數) x 100%; >3% 觸發警報 |
| 入款成功率（Credit Success Rate） | >95% | (成功入款數 / 入款嘗試數) x 100%; <90% 觸發警報 |
| 平均入款延遲（Average Credit Delay） | <30 分鐘 | PSP 成功回調至平台餘額入款的時間; >2 小時觸發警報 |
| PSP API 成功率 | >99% | (成功查詢數 / 總查詢數) x 100%; <95% 觸發警報 |
| 待處理積壓（Pending Backlog） | <10 筆訂單 | 待處理 >2 小時的訂單; >50 筆訂單觸發警報 |
| 人工審核率（Manual Review Rate） | <5% | (人工審核數 / 總入款數) x 100%; >15% 觸發警報 |

---

## 1. 概述

支付營運模組負責與外部支付服務供應商（Payment Service Providers, PSP）的所有互動，確保資金流入流出的安全、穩定與自動化。系統必須支援多種支付方式與動態路由能力。

---

## 2. 支援的支付方式

### 2.1 法定貨幣支付

| 支付類型 | 範例 | 業務考量 |
|----------|------|----------|
| **銀行轉帳（Bank Transfer）** | 電匯（Wire Transfer）、ACH、SEPA | T+1 至 T+3 結算 |
| **信用卡/金融卡（Credit/Debit Card）** | VISA、Mastercard、AMEX | 受司法管轄區限制 |
| **電子錢包（E-Wallet）** | LinePay、Momo、GCash、PayPal、Skrill | 區域可用性不同 |

### 2.2 加密貨幣支付

| 貨幣 | 網路 | 特殊要求 |
|------|------|----------|
| USDT | TRC20、ERC20 | 需即時匯率轉換 |
| BTC | Bitcoin Network | 錢包地址風險評分（AML） |
| ETH | Ethereum | Gas fee 考量 |

**業務規則**: 平台主帳戶通常以法定貨幣計價。加密貨幣支付需整合匯率 API（例如 Binance、Oanda）進行即時轉換。

---

## 3. 司法管轄區限制

### 3.1 信用卡禁令合規

| 司法管轄區 | 生效日期 | 範圍 | 影響 |
|-----------|---------|------|------|
| **英國（United Kingdom）** | 2020 年 4 月 | 所有賭博 | 信用卡封鎖 |
| **澳洲（Australia）** | 2026 年 4 月 | 線上賭博 | 需提前準備 |
| **瑞典（Sweden）** | 2025+ | 線上品牌 | 禁令擴大中 |
| **德國（Germany）** | 2021 | 所有賭博 | 完全禁止 |

**業務要求**: 支付方式白名單必須根據玩家司法管轄區動態載入。

### 3.2 區域 PSP 矩陣

| 地區 | 偏好 PSP | 主要支付方式 | 備註 |
|------|---------|--------------|------|
| 美國（United States） | Stripe、Nuvei | 信用卡（Credit Card）、ACH | 需 PCI-DSS Level 1 |
| 歐盟（European Union） | Adyen、Trustly | SEPA、iDEAL、Sofort | PSD2 強認證 |
| 中國（China） | 支付寶（Alipay）、微信支付（WeChat Pay） | QR Code 支付 | 需商戶資格 |
| 菲律賓（Philippines） | GCash、PayMaya | 電子錢包（E-Wallet） | 高現金使用市場 |
| 巴西（Brazil） | MercadoPago、PagSeguro | Boleto、PIX | PIX 即時轉帳為主 |
| 日本（Japan） | PayPay、Line Pay、樂天支付（Rakuten Pay） | QR Code、電子錢包 | 行動優先市場 |

---

## 4. 支付路由規則

### 4.1 智慧路由原則

**動態切換條件**:
- 當支付通道成功率低於門檻（例如 80%），自動切換至備用通道
- 將 VIP 玩家路由至專屬高速通道
- 優先使用本地 PSP 以降低跨境費用並提升成功率

### 4.2 路由權重因子

| 因子 | 權重 | 業務理由 |
|------|------|----------|
| **成功率（Success Rate）** | 50% | 影響玩家體驗與平台損失的主要指標 |
| **交易費用（Transaction Fee）** | 30% | 成本控制；對高額交易影響顯著 |
| **結算速度（Settlement Speed）** | 15% | 使用者體驗；更快入款提升滿意度 |
| **VIP 優先級（VIP Priority）** | 5% | 高價值玩家的差異化服務 |
| **貨幣匹配（Currency Match）** | 3% | 避免匯率損失與額外費用 |

→ **[Smart Routing Algorithm](../../architecture/02_Finance_Service/06_Payment_Gateway_Technical.md#smart-routing-algorithm)** - 分數計算公式、加權排序實作、即時 PSP 選擇邏輯

### 4.3 VIP 通道優勢

| VIP 等級 | 通道優勢 | 費用折扣 | 優先級 |
|----------|----------|----------|---------|
| VIP 5 | 專屬客戶經理 | 優先處理 | 最高 |
| VIP 4 | 費用減免 -0.5% | 快速通道 | 高 |
| VIP 3 | 加速結算（<5 分鐘） | 標準 | 中 |
| VIP 1-2 | 標準 PSP | 無 | 普通 |

---

## 5. 存款業務規則

### 5.1 存款流程摘要

1. 玩家發起存款 -> 系統建立訂單（Pending）-> 重定向至 PSP 支付頁面
2. 玩家完成支付 -> PSP 發送回調（callback）-> 系統驗證真實性
3. 驗證通過 -> 入款至玩家餘額 -> 更新訂單狀態（Success）-> 發送通知

→ **[PSP Webhook Integration](../../architecture/02_Finance_Service/06_Payment_Gateway_Technical.md#psp-webhook-integration)** - 簽章驗證（HMAC-SHA256）、回調處理、資料庫更新

### 5.2 金額限制

| 限制類型 | 配置層級 | 執行方式 |
|----------|----------|----------|
| 單筆最低金額 | 商戶可配置 | 即時 |
| 單筆最高金額 | 商戶可配置 | 即時 |
| 每日限額 | 依玩家等級 | 累計檢查 |
| 每月限額 | 依 VIP 等級 | 累計檢查 |

---

## 6. 提款業務規則

### 6.1 出款方式

**自動出款（API）**:
- 小額提款（例如 < $500）：自動處理
- 大額提款：需人工審核後觸發 API

**人工出款（Manual）**:
- 財務人員在後台審核銀行資料
- 手動轉帳並標記完成

### 6.2 審批門檻

| 金額範圍 | 審批層級 | SLA | 所需證據 |
|----------|----------|-----|----------|
| < $100 | CS 專員 | 立即 | 玩家請求 |
| $100 - $1,000 | CS 經理 | 1 小時 | 銀行驗證 |
| $1,000 - $10,000 | CFO | 4 小時 | 完整銀行對帳單 |
| > $10,000 | CFO + CEO | 24 小時 | 完整文件 + 視訊通話 |

---

## 7. 失敗交易處理

### 7.1 對帳要求

**自動對帳（Automatic Reconciliation）**:
- 頻率：每 15 分鐘
- 目標：待處理 > 30 分鐘的交易
- 動作：查詢 PSP 狀態並對帳

→ **[Auto Reconciliation Implementation](../../architecture/02_Finance_Service/06_Payment_Gateway_Technical.md#auto-reconciliation)** - 排程任務配置、PSP API 整合、SQL 查詢、狀態同步邏輯

### 7.2 玩家申訴流程

1. 玩家上傳支付證明（銀行轉帳截圖）
2. CS 查詢 PSP API 驗證
3. 若 PSP 確認收款但平台未入款 -> 人工入款 + 稽核日誌

### 7.3 嚴重性分類

| 場景 | 優先級 | SLA | 通知接收者 |
|------|--------|-----|------------|
| 高額 NOT_FOUND（>$1000） | P0 重大 | 15 分鐘 | 財務 + CTO + 安全團隊 |
| 掉單率 > 3% | P1 高 | 1 小時 | 財務 + CS 經理 |
| 單筆掉單（已自動入款） | P2 中 | 24 小時 | 每日摘要 |
| PSP PENDING 狀態 | P3 低 | 僅監控 | 無 |

---

## 8. SLA 要求

### 8.1 效能目標

| 指標 | 定義 | 目標 | 警報門檻 |
|------|------|------|----------|
| **掉單率（Drop Rate）** | (入款成功數 / 總成功數) x 100% | < 1% | > 3% |
| **入款成功率（Credit Success Rate）** | (成功入款數 / 入款嘗試數) x 100% | > 95% | < 90% |
| **平均入款延遲（Average Credit Delay）** | PSP 成功至平台入款的時間 | < 30 分鐘 | > 2 小時 |
| **待處理積壓（Pending Backlog）** | 待處理 > 2 小時的訂單 | < 10 筆 | > 50 筆 |
| **人工審核率（Manual Review Rate）** | (人工審核數 / 總入款數) x 100% | < 5% | > 15% |
| **PSP API 成功率** | (成功查詢數 / 總查詢數) x 100% | > 99% | < 95% |

→ **[Performance Monitoring](../../architecture/02_Finance_Service/06_Payment_Gateway_Technical.md#performance-monitoring)** - Prometheus 指標配置、Grafana 儀表板、警報規則（PagerDuty、Slack）

### 8.2 PSP 健康狀態門檻

| 狀態 | 成功率 | 路由決策 | 恢復條件 |
|------|--------|----------|----------|
| 健康（Healthy） | >= 80% | 正常路由 | N/A |
| 降級（Degraded） | 50-80% | 降低優先級（分數 x 0.7） | 連續 3 次成功 |
| 不可用（Unavailable） | < 50% | 排除，使用備用 | 人工驗證 |
| 當機（Down） | 無回應 | 跳過，使用佇列中下一個 | 緊急警報 + 人工 |

---

## 9. 合規要求

### 9.1 安全標準

| 要求 | 標準 | 業務規則 |
|------|------|----------|
| 卡片資料儲存 | PCI-DSS Level 1 | 僅儲存 Token；不儲存完整卡號 |
| 支付頁面 | PCI-DSS | 使用 PSP 託管支付頁面 |
| 資料傳輸 | 加密 | 所有 API 請求必須加密 |
| API 金鑰管理 | 安全保險庫 | 金鑰儲存於安全憑證保險庫 |

→ **[Security Implementation](../../architecture/02_Finance_Service/06_Payment_Gateway_Technical.md#security-implementation)** - TLS 1.2+ 配置、HashiCorp Vault 整合、API 金鑰輪替政策

### 9.2 3D Secure 要求

| 法規 | 要求 | 影響 |
|------|------|------|
| PSD2（歐盟） | 強制 3DS 2.0 | 所有歐盟卡片交易 |
| SCA 豁免（SCA Exemptions） | 小額（<30 EUR）、定期付款 | 符合資格交易的摩擦降低 |

→ **[3DS Integration](../../architecture/02_Finance_Service/06_Payment_Gateway_Technical.md#3ds-integration)** - 3DS 2.0 流程實作、SCA 豁免邏輯、PSD2 合規驗證

### 9.3 AML 合規

- 加密貨幣錢包地址風險評分（必要）
- 可疑交易報告
- 交叉比對制裁名單

---

## 10. 配置變更治理

### 10.1 高風險操作

- 新增/修改 PSP 商戶憑證
- 調整路由權重
- 變更交易限額

### 10.2 變更審批流程

1. 技術團隊提交變更請求
2. 系統計算影響範圍（預估受影響交易量）
3. CFO/CTO 審核與批准
4. 排程部署並通知營運團隊

---

## 11. 相關文件

### 業務參考
- [Wallet Architecture](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md) - 存款入帳邏輯
- [Withdrawal Risk Control](../../source-archive/01_Player_Center/01-05_Withdrawal_Risk.md) - 提款流程與風控
- [Reconciliation System](../../source-archive/02_Finance_Center/02-03_Reconciliation_System.md) - PSP 對帳流程

### 合規參考
- [Payment Restrictions](../../source-archive/12_System_Security/12-06_Payment_Restrictions.md) - 信用卡禁令與加密貨幣合規
- [Multi-Jurisdiction Framework](../../source-archive/06_Platform_Governance/06-07_Multi_Jurisdiction_Framework.md) - 多牌照支付配置
- [UKGC Compliance](../../source-archive/06_Platform_Governance/06-08_UKGC_Compliance.md) - 英國信用卡禁令細節

### 技術實作

→ **[Payment Gateway API Architecture](../../architecture/02_Finance_Service/05_Payment_Gateway_API.md)** - PSP 適配器實作、webhook 處理、智慧路由演算法、對帳自動化、HikariCP 調校、Prometheus 監控

---

**文件版本**: 1.0.0
**最後更新**: 2026-02-08
**維護者**: 財務團隊
