# 支付安全需求 (Payment Security Requirements)

> **規範來源**: [12-06_Payment_Restrictions.md](../../source-archive/12_System_Security/12-06_Payment_Restrictions.md), [12-07_Data_Portability_SAR.md](../../source-archive/12_System_Security/12-07_Data_Portability_SAR.md), [12-08_MITM_Detection.md](../../source-archive/12_System_Security/12-08_MITM_Detection.md)
> **目標讀者**: 產品經理、合規官、支付營運
> **相關架構**: [Payment_Restrictions.md](../../architecture/12_Security/09_Payment_Restrictions.md), [Data_Portability_SAR.md](../../architecture/12_Security/05_Data_Portability_SAR.md), [MITM_Detection.md](../../architecture/12_Security/10_MITM_Detection.md)
> **最後同步**: 2026-02-09

---

## 1. 信用卡禁令需求 (Credit Card Ban Requirements)

### 1.1 司法管轄區限制 (Jurisdictional Restrictions)

| 地區 | 生效日期 | 範圍 | 狀態 |
|--------|---------------|-------|--------|
| **英國** | 2020年4月 | 所有賭博 | 完全禁止 |
| **澳洲** | 2026年4月 | 線上賭博 | 即將實施 |
| **瑞典** | 2025+ | 線上品牌 | 逐步擴大 |
| **德國** | 2021年 | 所有賭博 | 完全禁止 |

### 1.2 業務規則 (Business Rules)

- 系統必須根據玩家司法管轄區自動檢測並阻止信用卡存款
- 所有司法管轄區均允許使用借記卡
- 加密貨幣限制因司法管轄區而異，必須可配置

### 1.3 依司法管轄區劃分的支付方式白名單 (Payment Method Whitelist by Jurisdiction)

**英國允許的方式**:

| 支付類型 | 允許 | 備註 |
|-------------|---------|-------|
| 借記卡 (Debit Card) | 是 | Visa/Mastercard 借記卡 |
| 銀行轉帳 (Bank Transfer) | 是 | 銀行電匯 |
| 電子錢包 (e-Wallet) | 是 | PayPal, Skrill, Neteller |
| 預付卡 (Prepaid Card) | 是 | Paysafecard |
| 信用卡 (Credit Card) | **否** | 2020年4月起禁止 |

**巴西要求的方式**:

| 支付類型 | 要求 | 備註 |
|-------------|-------------|-------|
| PIX | **強制性** | 巴西即時支付 |
| 銀行轉帳 (Bank Transfer) | 建議 | 銀行電匯 |
| Boleto | 建議 | 現金支付 |
| 信用卡 (Credit Card) | 允許 | 目前允許 |

### 1.4 加密貨幣合規 (Cryptocurrency Compliance)

| 地區 | 立場 | 要求 |
|--------|--------|-------------|
| 英國 | 謹慎 | 需要完整的 AML |
| 馬爾他 | 允許 | 需要監管框架 |
| 庫拉索 | 允許 | 較不嚴格 |

**加密貨幣 AML 要求**:
- 接受存款前對錢包地址進行風險評分
- 交易追蹤以檢測黑名單地址
- 所有加密貨幣 AML 檢查必須記錄以供審計

---

## 2. 數據可攜性與主體訪問請求 (Data Portability and Subject Access Requests, SAR)

### 2.1 監管截止日期 (Regulatory Deadlines)

| 法規 | 權利 | 回應截止日期 | 延期 |
|-----------|-------|-------------------|-----------|
| GDPR Art. 15 | 訪問權 | 30天 | 最多90天（複雜情況） |
| GDPR Art. 20 | 可攜性權 | 30天 | 無 |
| UK GDPR | 與 GDPR 相同 | 30天 | 相同 |
| CCPA | 知情權 | 45天 | 無 |

### 2.2 請求類型 (Request Types)

| 類型 | 代碼 | 描述 |
|------|------|-------------|
| 訪問請求 (Access Request, SAR) | ACCESS | 獲取個人數據副本 |
| 可攜性請求 (Portability Request) | PORTABILITY | 以機器可讀格式導出 |
| 刪除請求 (Erasure Request) | ERASURE | 被遺忘權 |
| 更正請求 (Rectification Request) | RECTIFICATION | 更正不準確數據 |
| 處理限制 (Processing Restriction) | RESTRICTION | 暫停數據處理 |

### 2.3 SAR 處理要求 (SAR Processing Requirements)

1. **身份驗證 (Identity Verification)**: 標準（電子郵件 + OTP）或增強（政府ID + 交易驗證），5個工作日內完成
2. **請求評估 (Request Assessment)**: 確定清晰度、範圍和任何例外情況
3. **數據收集 (Data Collection)**: 從所有來源（數據庫、日誌、第三方系統、備份）收集，15個工作日內完成
4. **數據審查 (Data Review)**: 編輯第三方數據、商業機密、法律特權信息和正在進行的調查詳情
5. **打包交付 (Package Delivery)**: 通過安全下載鏈接提供加密文件（7天有效期）

### 2.4 數據導出範圍 (Data Export Scope)

**必須包含**:
- 帳戶信息（用戶名、電子郵件、電話、姓名、地址、日期）
- KYC 驗證狀態和日期
- 財務數據（存款/提款歷史、餘額）
- 遊戲數據（投注歷史、獎金歷史、遊戲時間統計）
- 負責任博彩設置（存款限額、自我排除歷史）
- 通訊記錄（客服對話、通知）
- 技術數據（登錄歷史、設備信息）

**必須排除**:
- 其他玩家的個人數據
- 客服代理姓名（改用 ID）
- 風險控制規則配置
- 欺詐檢測分數
- 內部調查筆記
- 正在進行的法律程序
- 匯總/匿名數據

### 2.5 可攜性格式要求 (Portability Format Requirements)

- 需要機器可讀格式：JSON（結構化）、CSV（表格）、XML（兼容性）
- 僅 PDF 不可接受（不可機器讀取）
- 必須包含數據字典/架構文檔
- 應要求支持直接傳輸給第三方（GDPR Art. 20(2)）

### 2.6 例外處理 (Exception Handling)

在以下情況下可拒絕請求：
- 明顯無根據或過度
- 12個月內重複請求
- 會對他人權利產生不利影響

---

## 3. MITM 攻擊保護需求 (MITM Attack Protection Requirements)

### 3.1 威脅概覽 (Threat Overview)

| 攻擊類型 | 嚴重性 | 描述 |
|------------|----------|-------------|
| TLS 降級 (TLS Downgrade) | 嚴重 | 強制使用弱加密協議 |
| SSL 剝離 (SSL Stripping) | 嚴重 | 將 HTTPS 降級為 HTTP |
| 證書偽造 (Certificate Forgery) | 嚴重 | 使用偽造的 CA 證書 |
| DNS 欺騙 (DNS Spoofing) | 高 | 篡改 DNS 解析結果 |
| ARP 欺騙 (ARP Spoofing) | 高 | 局域網流量攔截 |
| 代理注入 (Proxy Injection) | 高 | 注入惡意透明代理 |

### 3.2 檢測需求 (Detection Requirements)

| 能力 | 描述 | 檢測延遲 |
|-----------|-------------|-------------------|
| TLS 降級檢測 | 檢測強制 TLS 版本降級 | 實時 |
| 證書固定 (Certificate pinning) | 驗證服務器證書合法性 | 實時 |
| 會話劫持檢測 | 識別被盜會話令牌 | 近實時（<1秒） |
| DNS 欺騙檢測 | 檢測 DNS 解析異常 | 實時 |
| 代理注入檢測 | 識別惡意代理攔截 | 實時 |

### 3.3 響應操作 (Response Actions)

| 警報類型 | 觸發條件 | 嚴重性 | 操作 |
|-----------|-------------------|----------|--------|
| TLS 降級嘗試 | 檢測到 TLS 1.0/1.1 請求 | 嚴重 | 阻止 + 記錄 |
| 證書固定失敗 | 證書不匹配 | 嚴重 | 阻止 + 調查 |
| 會話劫持 | 設備 + IP 同時更改 | 嚴重 | 終止會話 |
| 可疑代理 | 多個代理信號 | 高 | 步進身份驗證 |
| DNS 異常 | 解析結果不一致 | 高 | 驗證 + 記錄 |

### 3.4 會話綁定規則 (Session Binding Rules)

- 每個會話必須綁定到設備指紋和 IP 地址
- 設備指紋更改觸發重新身份驗證
- "不可能旅行"檢測：如果 IP 地理位置更改速度超過物理可能，標記為潛在劫持
- 風險級別：嚴重（設備 + 不可能旅行）、高（僅設備不匹配）、中（僅 IP 更改）

---

## 4. 監控 KPI (Monitoring KPIs)

### 4.1 SAR 指標 (SAR Metrics)

| 指標 | 目標 | 警報 |
|--------|--------|-------|
| 待處理請求數量 | < 10 | > 10 |
| 逾期請求數量 | 0 | > 0 |
| 平均處理天數 | < 20 | > 20 |
| 完成率 | > 95% | < 95% |

### 4.2 MITM 指標 (MITM Metrics)

| 指標 | 目標 | 警報 |
|--------|--------|-------|
| TLS 1.3 採用率 | > 90% | < 80% |
| 證書固定失敗率 | < 0.01% | > 0.1% |
| 每日會話劫持檢測 | < 5 | > 20 |
| 代理檢測率 | 監控趨勢 | 異常增加 |

---

## 5. 驗收標準 (Acceptance Criteria)

1. 在英國、德國和其他限制司法管轄區自動阻止信用卡存款
2. 按司法管轄區強制執行支付方式白名單
3. 加密貨幣存款進行錢包風險評分和交易追蹤
4. SAR 請求在30天截止日期內處理並安全交付
5. 數據可攜性導出包含所有必需數據，採用機器可讀格式
6. MITM 檢測涵蓋 TLS 降級、證書固定、會話劫持、DNS 欺騙和代理檢測
7. 會話綁定正確強制執行設備指紋和 IP
8. 所有安全事件已記錄並配置警報閾值
