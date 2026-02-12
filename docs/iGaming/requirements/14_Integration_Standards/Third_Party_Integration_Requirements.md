# 第三方整合需求 (Third-Party Integration Requirements)

> **Canonical Source**: [source-archive/14_Third_Party_Integration/14-01](../../source-archive/14_Third_Party_Integration/14-01_Third_Party_Integration.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Integration Managers, Operations Team
> **Related Architecture**: [Third Party Integration Architecture](../../architecture/14_Third_Party/Third_Party_Integration_Architecture.md)
> **Last Synced**: 2026-02-09

---

## 業務價值 (Business Value)

此第三方整合框架提供關鍵價值：
- **服務連續性 (Service Continuity)**: 統一適配器層配備故障轉移策略（備用 PSP、手動 KYC 審查）防止第三方中斷造成的收入損失——預計正常運行時間從 98% 提升至 99.9%，每年可節省 $500K-$1M 的停機損失
- **安全與合規 (Security & Compliance)**: 在 HashiCorp Vault 中加密存儲 API 密鑰並自動輪換（PSP：90天，內部：30天）防止憑證洩露，將洩露風險降低 95% 並確保 PCI-DSS 合規
- **營運韌性 (Operational Resilience)**: Webhook 重試配備指數退避（6次重試，耗盡後進入 DLQ）確保 99.9% 的支付回調成功率，防止每月 $50K-$100K 的存款/提款對帳失敗和手動解決成本
- **成本優化 (Cost Optimization)**: 速率限制感知與佇列處理（Onfido: 100 req/min, SendGrid: 1000 req/hour）防止超限收費和服務限流，降低整合成本 20-30%
- **監控與警報 (Monitoring & Alerting)**: 實時健康儀表板配備 SLA 追蹤（PSP 99.5%, KYC 98%, GP 99%）實現主動事件響應，將平均修復時間 (MTTR) 降低 60%

---

## 1. 整合原則 (Integration Principles)

- **統一接口 (Unified Interface)**: 所有第三方整合通過統一適配器層訪問
- **容錯性 (Fault Tolerance)**: 第三方服務故障不得導致核心平台功能崩潰
- **監控優先 (Monitoring First)**: 每個整合點必須具備健康檢查和警報
- **安全優先 (Security First)**: API 密鑰加密存儲，Webhook 簽名驗證

---

## 2. 整合類別 (Integration Categories)

### 2.1 遊戲供應商 (Game Providers, GP)

- Seamless Wallet 整合
- 遊戲啟動 URL 生成
- 投注/支付 Webhook 接收

### 2.2 支付服務供應商 (Payment Service Providers, PSP)

- 支付請求 API
- 支付回調 Webhook
- 對帳報告下載

### 2.3 KYC/AML 供應商 (KYC/AML Vendors)

| 供應商 | 服務類型 | 每次檢查成本 |
|--------|-------------|---------------|
| Onfido | 身份驗證、面部識別 | ~$2 |
| Jumio | 文件驗證 | ~$1.5 |
| ComplyAdvantage | AML 制裁篩查 | ~$0.5 |
| Sumsub | 全面 KYC | ~$3 |

### 2.4 營銷工具 (Marketing Tools)

| 工具類型 | 供應商 | 用途 |
|-----------|---------|---------|
| 電子郵件 (Email) | SendGrid, AWS SES | 交易和營銷郵件 |
| 簡訊 (SMS) | Twilio, Vonage | 驗證碼、提款通知 |
| 推送通知 (Push Notifications) | OneSignal, Firebase | 應用推送通知 |
| 營銷自動化 (Marketing Automation) | Braze, Customer.io | 玩家生命週期管理 |

### 2.5 分析工具 (Analytics Tools)

| 工具 | 用途 |
|------|---------|
| Google Analytics 4 | 網站流量、用戶行為 |
| Mixpanel | 產品分析、漏斗分析 |
| Amplitude | 留存、事件追蹤 |
| Segment | 數據管道（統一接口） |

---

## 3. 供應商 SLA 要求 (Vendor SLA Requirements)

### 3.1 可用性要求 (Availability Requirements)

| 服務類型 | 最低可用性 | 響應時間 SLA |
|-------------|---------------------|-------------------|
| PSP（支付） | 99.5% | P99 < 3s |
| KYC 供應商 | 98.0% | P99 < 5s |
| 遊戲供應商 | 99.0% | P99 < 3s |
| 電子郵件服務 | 99.0% | P99 < 1s |
| 分析工具 | 95.0% | 盡力而為 |

### 3.2 速率限制感知 (Rate Limiting Awareness)

| 服務 | 限制 | 時間窗口 | 超限操作 |
|---------|-------|--------|-------------------|
| Onfido KYC | 100 req/min | 60秒 | 佇列 + 429 |
| SendGrid 電子郵件 | 1000 req/hour | 3600秒 | 佇列 + 延遲發送 |
| GP 遊戲啟動 | 500 req/min | 60秒 | 返回快取 URL |
| Google Analytics | 無限制 | - | - |

---

## 4. Webhook 要求 (Webhook Requirements)

### 4.1 重試策略 (Retry Policy)

| 重試 | 延遲 | 累計等待 |
|-------|-------|----------------|
| 第1次 | 5秒 | 5秒 |
| 第2次 | 10秒 | 15秒 |
| 第3次 | 20秒 | 35秒 |
| 第4次 | 40秒 | 75秒 |
| 第5次 | 80秒 | 155秒 |
| 第6次（最終） | 160秒 | 315秒 |

### 4.2 死信佇列 (Dead Letter Queue, DLQ)

- 6次重試後失敗的事件自動進入 DLQ
- DLQ 保留7天
- 可進行手動重播或失敗分析
- 警報觸發：DLQ 累積 > 100 個事件

---

## 5. API 密鑰管理要求 (API Key Management Requirements)

### 5.1 密鑰存儲 (Key Storage)

- 所有 API 密鑰必須存儲在加密的秘密管理系統中（例如 HashiCorp Vault）
- 密鑰絕不能出現在源代碼、日誌或配置文件中
- 對密鑰的訪問受基於角色的策略限制

### 5.2 密鑰輪換策略 (Key Rotation Policy)

| 服務類型 | 輪換週期 | 自動化 | 觸發條件 |
|-------------|----------------|------------|---------|
| PSP API 密鑰 | 90天 | 自動 | 定期 + 可疑活動 |
| 內部服務密鑰 | 30天 | 自動 | 定期 |
| Webhook 秘密 | 按需 | 手動 | 疑似洩露 |
| 數據庫密碼 | 180天 | 自動 | 定期 |

---

## 6. 服務降級要求 (Service Degradation Requirements)

### 6.1 降級優先級 (Degradation Priority)

| 服務類型 | 優先級 | 中斷影響 | 故障轉移 |
|-------------|----------|-----------------|----------|
| 支付閘道 (Payment Gateway) | 嚴重 | 無法存款/提款 | 切換到備用 PSP |
| KYC 供應商 | 重要 | 無法驗證身份 | 手動審查流程 |
| 遊戲供應商 | 重要 | 特定遊戲不可用 | 顯示維護通知 |
| 電子郵件服務 | 可選 | 郵件延遲 | 佇列 + 稍後重試 |
| 分析工具 | 可選 | 無法追蹤事件 | 本地日誌記錄 |

### 6.2 服務恢復檢測 (Service Recovery Detection)

- 健康檢查間隔：30秒
- 恢復條件：連續3次成功的健康檢查（可用性 > 95%）
- 恢復後自動切換回主要服務
- 恢復事件記錄到警報渠道

---

## 7. 監控要求 (Monitoring Requirements)

### 7.1 健康儀表板 (Health Dashboard)

實時監控所有第三方服務健康狀況：
- 可用性百分比（最近一小時）
- P99 響應時間
- 錯誤率
- 狀態指標（健康/降級/中斷）

### 7.2 警報配置 (Alert Configuration)

| 警報 | 條件 | 嚴重性 |
|-------|-----------|----------|
| PSP 失敗率 > 5% | 5分鐘窗口 | 嚴重 |
| KYC P99 延遲 > 5秒 | 10分鐘持續 | 警告 |
| 電子郵件投遞失敗 | 任何失敗 | 警告 |
| Webhook DLQ > 100 | 累積計數 | 高 |

---

## 8. 驗收標準 (Acceptance Criteria)

- [ ] 所有第三方整合通過統一適配器層訪問，具有一致的接口模式
- [ ] 第三方服務故障不會導致核心平台服務崩潰（實施斷路器）
- [ ] Webhook 重試配備指數退避（5秒 → 10秒 → 20秒 → 40秒 → 80秒 → 160秒，6次失敗後進入 DLQ）
- [ ] API 密鑰存儲在加密的秘密管理系統（HashiCorp Vault），絕不出現在源代碼或日誌中
- [ ] 強制執行密鑰輪換策略：PSP 90天，內部 30天，數據庫 180天
- [ ] 服務降級故障轉移功能正常：PSP 備份切換、KYC 手動審查、遊戲維護通知
- [ ] 健康儀表板顯示所有整合的實時狀態（可用性%、P99 延遲、錯誤率）
- [ ] 配置警報：PSP 失敗 > 5%（嚴重）、KYC P99 > 5秒（警告）、DLQ > 100 個事件（高）
- [ ] 實施速率限制感知並對 Onfido（100/分鐘）、SendGrid（1000/小時）進行適當佇列處理
- [ ] 服務恢復檢測在連續3次成功健康檢查後觸發自動切換回主服務
