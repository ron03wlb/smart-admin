# 02-02 支付閘道整合 (Payment Gateway Integration)

## 1. 系統概述
負責處理所有與外部金流渠道 (PSP) 的交互，確保資金進出的安全、穩定與自動化。
需支援多種支付方式與動態路由切換。

## 2. 核心功能需求

### 2.1 支付方式 (Payment Methods)
- **法幣支付**：
  - 網銀轉帳 (Bank Transfer)
  - 快捷支付 (Credit/Debit Card)
  - 電子錢包 (E-Wallet: LinePay, Momo, GCash)
- **加密貨幣 (Crypto)**：
  - 支援 USDT (TRC20, ERC20), BTC, ETH
  - **自動匯率換算**：由於平台主帳戶通常為法幣，需串接匯率 API (如 Binance/Oanda) 實時計算匯率。

### 2.2 支付路由 (Smart Routing)
- **動態切換**：
  - 當某一通道成功率低於閾值 (如 80%)，自動切換至備用通道。
  - 依據玩家等級路由：VIP玩家走專屬高速通道。
- **商戶配置**：商戶可獨立開關特定支付渠道，並設定單筆/單日限額。

### 2.3 存款流程 (Deposit)
1. 玩家發起存款 -> 系統建立訂單 (Pending) -> 跳轉第三方支付頁面
2. 玩家完成支付 -> PSP 發送 Callback -> 系統驗證簽名 (Signature)
3. 驗證通過 -> 寫入資料庫 -> 增加玩家餘額 -> 更新訂單狀態 (Success) -> 發送通知

#### 2.3.1 充值流程時序圖 (Deposit Flow Sequence)

以下時序圖展示了從玩家發起充值到餘額到帳的完整流程，包括 PSP 路由、簽名驗證、回調處理及冪等性保護：

```mermaid
sequenceDiagram
    participant Player
    participant Frontend
    participant API Gateway
    participant PaymentService
    participant PSP Router
    participant PSP (Nuvei)
    participant Webhook Handler
    participant WalletService
    participant Redis
    participant DB
    participant Notification

    Note over Player,Notification: Deposit Flow - From Request to Credit

    Player->>Frontend: Click "Deposit" (Amount: $100, Method: Credit Card)
    Frontend->>API Gateway: POST /api/v1/payments/deposit<br/>{amount: 100, currency: USD, payment_method: credit_card}

    API Gateway->>PaymentService: createDepositOrder(playerId, amount, method)
    activate PaymentService

    PaymentService->>PSP Router: selectBestPSP(player, amount, method, country)
    activate PSP Router

    Note over PSP Router: Smart Routing Algorithm (詳見 2.3.2 圖表)

    PSP Router->>PSP Router: Calculate PSP Scores:<br/>Nuvei: 85, Adyen: 78, Stripe: 72
    PSP Router-->>PaymentService: Selected PSP: Nuvei (Score: 85)
    deactivate PSP Router

    PaymentService->>DB: BEGIN TRANSACTION
    PaymentService->>DB: INSERT INTO transactions<br/>(id, player_id, amount, status, psp_code, created_at)<br/>VALUES (txn_20260127_001, player_123, 100, 'PENDING', 'nuvei', NOW())

    PaymentService->>PaymentService: Generate Checksum:<br/>SHA256(merchantId + amount + currency + timestamp + secret)

    PaymentService->>PSP (Nuvei): POST /ppp/api/v1/payment.do<br/>{merchantId, amount, currency, checksum, returnUrl}
    PSP (Nuvei)-->>PaymentService: {status: OK, redirect_url, payment_token, expires_at}

    PaymentService->>DB: UPDATE transactions SET<br/>psp_order_id = payment_token,<br/>redirect_url = redirect_url,<br/>expires_at = NOW() + 15min

    PaymentService->>DB: COMMIT
    PaymentService-->>API Gateway: {transaction_id, redirect_url, expires_at}
    deactivate PaymentService

    API Gateway-->>Frontend: 200 OK {redirect_url}
    Frontend->>PSP (Nuvei): 302 Redirect to Payment Page

    Note over Player,PSP (Nuvei): Player fills in card details & completes 3DS verification

    Player->>PSP (Nuvei): Submit Card Details (Card: 4111****1111, CVV: 123)
    PSP (Nuvei)->>PSP (Nuvei): Process Payment (3-30 seconds)

    alt Payment Success
        PSP (Nuvei)->>Webhook Handler: POST /webhook/deposit<br/>X-PSP-Signature: HMAC-SHA256<br/>Body: {transaction_id, status: APPROVED, psp_txn_id, amount}

        activate Webhook Handler

        Webhook Handler->>Webhook Handler: Step 1: Verify IP Whitelist<br/>(Request IP in PSP_IPS?)

        alt IP Not Whitelisted
            Webhook Handler-->>PSP (Nuvei): 403 Forbidden
            Webhook Handler->>Notification: Alert Security Team (Suspicious IP)
        end

        Webhook Handler->>Webhook Handler: Step 2: Verify HMAC Signature<br/>Expected = HMAC(body, webhook_secret)<br/>Received = X-PSP-Signature

        alt Signature Mismatch
            Webhook Handler-->>PSP (Nuvei): 403 Forbidden (Invalid Signature)
            Webhook Handler->>Notification: Alert Security Team (Signature Forgery)
        end

        Webhook Handler->>Webhook Handler: Step 3: Verify Timestamp<br/>(NOW() - request_timestamp < 5min?)

        alt Timestamp Expired
            Webhook Handler-->>PSP (Nuvei): 400 Bad Request (Replay Attack)
        end

        Webhook Handler->>Redis: SET NX callback:txn_20260127_001 1 EX 60
        Redis-->>Webhook Handler: OK (Lock Acquired)

        alt Lock Failed (Duplicate Callback)
            Redis-->>Webhook Handler: NIL (Already Processing)
            Webhook Handler-->>PSP (Nuvei): 200 OK {status: ALREADY_PROCESSED}
        end

        Webhook Handler->>DB: SELECT * FROM transactions<br/>WHERE id = txn_20260127_001 FOR UPDATE
        DB-->>Webhook Handler: {status: PENDING, player_id: player_123, amount: 100}

        alt Status != PENDING (Idempotent Check)
            Webhook Handler-->>PSP (Nuvei): 200 OK {status: ALREADY_PROCESSED}
        else Status = PENDING (Normal Path)
            Webhook Handler->>DB: BEGIN TRANSACTION

            Webhook Handler->>DB: UPDATE transactions SET<br/>status = 'SUCCESS',<br/>psp_transaction_id = psp_txn_id,<br/>completed_at = NOW()

            Webhook Handler->>WalletService: creditBalance(player_id: player_123, amount: 100, ref_id: txn_20260127_001)
            activate WalletService
            WalletService->>DB: UPDATE player_wallet SET<br/>balance = balance + 100,<br/>version = version + 1<br/>WHERE player_id = player_123 AND version = current_version
            WalletService->>DB: INSERT INTO wallet_transactions<br/>(player_id, type, amount, ref_id, created_at)<br/>VALUES (player_123, 'DEPOSIT', 100, txn_20260127_001, NOW())
            WalletService-->>Webhook Handler: {status: OK, new_balance: 1100}
            deactivate WalletService

            Webhook Handler->>DB: INSERT INTO payment_audit_log<br/>(transaction_id, event, details, created_at)<br/>VALUES (txn_20260127_001, 'WEBHOOK_RECEIVED', {...}, NOW())

            Webhook Handler->>DB: COMMIT

            Webhook Handler->>Notification: sendDepositSuccessNotification(player_123, amount: 100)
            Notification->>Player: Email + SMS: "Deposit $100 Successful. New Balance: $1,100"

            Webhook Handler-->>PSP (Nuvei): 200 OK {status: SUCCESS}
        end

        Webhook Handler->>Redis: DEL callback:txn_20260127_001
        deactivate Webhook Handler

        PSP (Nuvei)->>Frontend: 302 Redirect to Return URL (success_page)
        Frontend->>Player: Display "Deposit Successful" + New Balance

    else Payment Failed
        PSP (Nuvei)->>Webhook Handler: POST /webhook/deposit<br/>Body: {transaction_id, status: DECLINED, error_code: INSUFFICIENT_FUNDS}

        Webhook Handler->>DB: UPDATE transactions SET<br/>status = 'FAILED',<br/>error_code = 'INSUFFICIENT_FUNDS',<br/>completed_at = NOW()

        Webhook Handler->>Notification: sendDepositFailedNotification(player_123, reason: INSUFFICIENT_FUNDS)
        Notification->>Player: Email: "Deposit Failed. Please try another card."

        Webhook Handler-->>PSP (Nuvei): 200 OK {status: RECEIVED}

        PSP (Nuvei)->>Frontend: 302 Redirect to Return URL (failure_page)
        Frontend->>Player: Display "Payment Failed. Reason: Insufficient Funds"

    else Payment Timeout (No Callback Received)
        Note over PaymentService,DB: Scheduled Job - Reconciliation (Every 15 min)

        PaymentService->>DB: SELECT * FROM transactions<br/>WHERE status = 'PENDING'<br/>AND created_at < NOW() - INTERVAL '30 minutes'
        DB-->>PaymentService: [txn_20260127_001, ...]

        loop For each Pending Transaction
            PaymentService->>PSP (Nuvei): GET /api/v1/query?transaction_id=txn_20260127_001<br/>Authorization: HMAC-SHA256

            alt PSP Status: APPROVED
                PSP (Nuvei)-->>PaymentService: {status: APPROVED, psp_txn_id, amount: 100}
                PaymentService->>PaymentService: Trigger補單流程 (Same as Webhook Handler)
                PaymentService->>WalletService: creditBalance(player_123, 100)
                PaymentService->>DB: UPDATE transactions SET status = 'SUCCESS'
                PaymentService->>Notification: Send補單通知
            else PSP Status: DECLINED
                PSP (Nuvei)-->>PaymentService: {status: DECLINED}
                PaymentService->>DB: UPDATE transactions SET status = 'FAILED'
            else PSP Status: PENDING
                PSP (Nuvei)-->>PaymentService: {status: PENDING}
                PaymentService->>PaymentService: Keep PENDING, retry later
            end
        end
    end

    style Webhook Handler fill:#DDA0DD
    style WalletService fill:#90EE90
    style PSP Router fill:#FFE4B5
    style Redis fill:#FFD93D
```text

**充值流程關鍵設計要點**：

| 階段 | 關鍵步驟 | 安全機制 | 性能目標 |
|------|---------|---------|---------|
| **1. PSP 路由** | 智能選擇最佳 PSP | 多維度評分（成功率、成本、速度） | < 100ms |
| **2. 訂單創建** | 生成唯一 transaction_id | DB 唯一約束防止重複 | < 50ms |
| **3. 簽名生成** | SHA256 checksum | 防止請求篡改 | < 10ms |
| **4. PSP 請求** | 獲取 redirect_url | HTTPS + TLS 1.2+ | < 500ms |
| **5. 回調驗證** | 3 層驗證（IP、簽名、時間戳） | 防止偽造、重放攻擊 | < 50ms |
| **6. 冪等性** | Redis SET NX 鎖 | 防止重複回調 | < 10ms |
| **7. 餘額更新** | 樂觀鎖 (version) | 防止併發衝突 | < 100ms |
| **8. 補單機制** | 定時查詢 PSP 狀態 | 防止掉單 | 每 15 分鐘 |

**安全驗證三層防護**：

```
Layer 1: IP 白名單驗證
  ├─ 僅接受來自 PSP 指定 IP 的請求
  └─ 拒絕其他 IP → 403 Forbidden + 告警

Layer 2: HMAC 簽名驗證
  ├─ Expected = HMAC-SHA256(request_body, webhook_secret)
  ├─ Received = X-PSP-Signature Header
  └─ Mismatch → 403 Forbidden + 安全告警

Layer 3: 時間戳驗證 (防重放攻擊)
  ├─ NOW() - request_timestamp < 5 minutes
  └─ Expired → 400 Bad Request
```sql

**冪等性保護機制**：

| 機制 | 實現方式 | TTL | 目的 |
|------|---------|-----|------|
| **Redis 鎖** | `SET NX callback:{txn_id} 1 EX 60` | 60 秒 | 防止同一回調並發處理 |
| **數據庫狀態** | `WHERE status = 'PENDING' AND UPDATE status = 'SUCCESS'` | N/A | 確保狀態轉換唯一性 |
| **Unique Constraint** | `UNIQUE (transaction_id, psp_transaction_id)` | N/A | 防止重複入帳 |

**異常場景處理**：

| 異常類型 | 觸發條件 | 處理策略 | 通知對象 |
|---------|---------|---------|---------|
| **IP 不在白名單** | Request IP ∉ PSP_IPS | 拒絕 + 安全告警 | Security Team |
| **簽名驗證失敗** | HMAC Mismatch | 拒絕 + 安全告警 | Security Team + CTO |
| **重複回調** | Redis Lock Failed | 返回 200 OK (ALREADY_PROCESSED) | 無（正常場景） |
| **狀態已變更** | Status != PENDING | 返回 200 OK (Idempotent) | 無（正常場景） |
| **回調超時** | 30 分鐘無回調 | 主動查詢 PSP 狀態 | Tech Team |
| **補單失敗** | PSP 返回 NOT_FOUND | 人工審核 + 玩家申訴 | CS Team + Finance |

### 2.4 提款流程 (Withdrawal)
- **代付 (Payout)**：
  - 系統需支援 API 自動代付。
  - **安全閾值**：小額提款 (如 < $500) 自動代付，大額提款需人工審核後觸發 API。
- **手動出款**：財務人員後台查看銀行資訊，手動轉帳後標記完成。

## 3. 安全與異常處理
- **簽名驗證**：所有 Callback 必須驗證 HMAC/MD5 簽名，防止偽造請求。
- **IP 白名單**：僅接收來自 PSP 指定 IP 的 Callback。
- **掉單處理**：
  - 定時任務 (Cron) 輪詢 PSP 訂單狀態接口，若發現狀態不一致，自動補單。

## 4. 具體 PSP 對接案例

### 4.1 Nuvei (Formerly SafeCharge) 對接

**API 端點**：
```text
Production: https://ppp.nuvei.com/ppp/api/v1/payment.do
Sandbox: https://ppp-test.nuvei.com/ppp/api/v1/payment.do
```

**存款 API 請求範例**：
```json
POST /ppp/api/v1/payment.do
{
  "merchantId": "123456789",
  "merchantSiteId": "987654",
  "clientRequestId": "txn_202601271234",
  "amount": "100.00",
  "currency": "USD",
  "userId": "player_12345",
  "timeStamp": "2026-01-27 10:30:00",
  "checksum": "e7f8a1b2c3d4e5f6..."  // SHA256(merchantId + merchantSiteId + clientRequestId + amount + currency + timeStamp + secret)
}
```markdown

**Callback 處理**：

### 4.2 Adyen 對接

**3DS 2.0 整合**：
Adyen 要求強制 3D Secure 驗證以符合 PSD2 規範。

**存款流程**：
1. 前端收集卡號、CVV、持卡人姓名
2. 調用 Adyen `/payments` API，返回 `action.type = "threeDS2"`
3. 前端加載 Adyen 3DS Component（iframe）
4. 玩家完成銀行驗證後，調用 `/payments/details` 獲取最終結果

**代付（Payout）API**：
```json
POST /pal/servlet/Payout/v68/payout
{
  "merchantAccount": "IGamingPlatformEU",
  "amount": {
    "currency": "EUR",
    "value": 5000  // 50.00 EUR（Adyen使用最小單位）
  },
  "reference": "withdrawal_98765",
  "shopperEmail": "player@example.com",
  "card": {
    "number": "4111111111111111",
    "expiryMonth": "03",
    "expiryYear": "2030",
    "holderName": "John Doe"
  }
}
```text

---

## 5. 智能路由算法詳解

### 5.1 路由決策矩陣

**多維度評分模型**：

#### 5.1.1 PSP 智能路由決策流程圖 (Smart Routing Decision Flow)

以下流程圖展示了 PSP 路由的完整決策邏輯。為提升可讀性，將複雜路由流程拆分為 **主流程圖 + 2 個子流程圖**。

##### 主流程圖：PSP 路由整體架構 (Routing Architecture Overview)

```mermaid
flowchart LR
    START[Player Deposit Request] --> INPUT[Input Parameters<br/>━━━━━━━━━━━━━━<br/>• Amount: $1000<br/>• Currency: USD<br/>• Country: US<br/>• Payment Method: Credit Card<br/>• VIP Level: 2]

    INPUT --> STEP1[🌍 Step 1<br/>Country & Payment<br/>Method Filtering<br/>━━━━━━━━━━━━━━<br/>Filter by Geography<br/>& Payment Type]

    STEP1 --> CANDIDATES[Candidate PSPs<br/>━━━━━━━━━━━━━━<br/>• Nuvei<br/>• Adyen<br/>• Stripe<br/>(3 candidates)]

    CANDIDATES --> STEP2[🏥 Step 2<br/>Health Status Check<br/>━━━━━━━━━━━━━━<br/>Filter Unavailable PSPs]

    STEP2 --> AVAILABLE[Available PSPs<br/>━━━━━━━━━━━━━━<br/>• Nuvei: Healthy<br/>• Adyen: Healthy<br/>• Stripe: Degraded<br/>(2 healthy)]

    AVAILABLE --> STEP3[📊 Step 3<br/>Multi-Dimensional<br/>Scoring Algorithm<br/>━━━━━━━━━━━━━━<br/>5 Dimensions Evaluation]

    STEP3 --> RANKED[Ranked PSPs<br/>━━━━━━━━━━━━━━<br/>1️⃣ Nuvei: 92.25<br/>2️⃣ Adyen: 78.0<br/>3️⃣ Stripe: 72.0]

    RANKED --> STEP4[🔍 Step 4<br/>Final Health Check<br/>━━━━━━━━━━━━━━<br/>Verify Top PSP Status]

    STEP4 --> DECISION{Top PSP Status?}

    DECISION -->|Healthy| ROUTE[✅ Route to Nuvei<br/>━━━━━━━━━━━━━━<br/>Payment URL Generated<br/>Settlement ETA: 10 min]
    DECISION -->|Degraded/Down| FALLBACK[⚠️ Fallback to Adyen<br/>━━━━━━━━━━━━━━<br/>Retry with 2nd PSP]

    FALLBACK --> DECISION

    ROUTE --> LOG[📝 Log Routing Decision<br/>━━━━━━━━━━━━━━<br/>Selected: Nuvei<br/>Score: 92.25<br/>Fallback Queue: [Adyen, Stripe]]

    LOG --> RETURN[Return to Gateway<br/>━━━━━━━━━━━━━━<br/>Redirect URL + ETA]

    RETURN --> END1[End - Success]

    style STEP1 fill:#FFE4B5
    style STEP2 fill:#ADD8E6
    style STEP3 fill:#DDA0DD
    style STEP4 fill:#90EE90
    style ROUTE fill:#90EE90
    style FALLBACK fill:#FFD93D
    style DECISION fill:#FFD700
```markdown

**主流程關鍵步驟**：
- **Step 1**: 地理位置與支付方式過濾（減少候選 PSP 數量）
- **Step 2**: 健康狀態檢查（排除不可用 PSP）
- **Step 3**: 多維度評分（5 個維度：成功率、費率、速度、VIP、幣別）
- **Step 4**: 最終健康檢查（避免路由至剛故障的 PSP）

---

##### 子流程圖 1：國家與支付方式過濾 (Country & Payment Method Filtering)

```mermaid
flowchart TD
    START[Step 1: Filter PSPs] --> INPUT[Input: Country, Payment Method]

    INPUT --> COUNTRY{Country?}

    COUNTRY -->|🇺🇸 US| US[US PSPs<br/>━━━━━━━━━━━━━━<br/>• Stripe<br/>• Nuvei<br/>• PayPal<br/>• Coinbase]
    COUNTRY -->|🇪🇺 EU| EU[EU PSPs<br/>━━━━━━━━━━━━━━<br/>• Adyen<br/>• Trustly<br/>• Klarna<br/>• Skrill]
    COUNTRY -->|🇨🇳 CN| CN[CN PSPs<br/>━━━━━━━━━━━━━━<br/>• Alipay<br/>• WeChat Pay<br/>• UnionPay]
    COUNTRY -->|🇵🇭 PH| PH[PH PSPs<br/>━━━━━━━━━━━━━━<br/>• GCash<br/>• PayMaya<br/>• GrabPay]
    COUNTRY -->|🇧🇷 BR| BR[BR PSPs<br/>━━━━━━━━━━━━━━<br/>• PagSeguro<br/>• MercadoPago<br/>• Pix]
    COUNTRY -->|🇯🇵 JP| JP[JP PSPs<br/>━━━━━━━━━━━━━━<br/>• PayPay<br/>• Line Pay<br/>• Rakuten Pay]
    COUNTRY -->|Other| GLOBAL[Global PSPs<br/>━━━━━━━━━━━━━━<br/>• Stripe<br/>• Adyen<br/>• Nuvei]

    US --> METHOD
    EU --> METHOD
    CN --> METHOD
    PH --> METHOD
    BR --> METHOD
    JP --> METHOD
    GLOBAL --> METHOD

    METHOD{Payment Method?}

    METHOD -->|💳 Credit Card| CARD[PSPs Supporting Cards<br/>━━━━━━━━━━━━━━<br/>Example: Stripe, Adyen, Nuvei<br/>Card Networks: VISA, MC, AMEX]
    METHOD -->|📱 E-Wallet| EWALLET[PSPs Supporting E-Wallets<br/>━━━━━━━━━━━━━━<br/>Example: PayPal, Skrill, Neteller<br/>Local: Alipay, WeChat, GCash]
    METHOD -->|🏦 Bank Transfer| BANK[PSPs Supporting Bank Transfers<br/>━━━━━━━━━━━━━━<br/>Example: Trustly, Klarna, Pix<br/>Settlement: T+1 to T+3]
    METHOD -->|₿ Crypto| CRYPTO[PSPs Supporting Crypto<br/>━━━━━━━━━━━━━━<br/>Example: Coinbase, BitPay<br/>Currencies: USDT, BTC, ETH]

    CARD --> STATUS_FILTER
    EWALLET --> STATUS_FILTER
    BANK --> STATUS_FILTER
    CRYPTO --> STATUS_FILTER

    STATUS_FILTER[Health Status Filtering] --> HEALTH{PSP Status?}

    HEALTH -->|✅ Healthy<br/>Success >= 80%| HEALTHY[Available for Scoring<br/>━━━━━━━━━━━━━━<br/>Pass to Step 3]
    HEALTH -->|⚠️ Degraded<br/>Success 50-79%| DEGRADED[Lower Priority<br/>━━━━━━━━━━━━━━<br/>Score Penalty: -20]
    HEALTH -->|❌ Unavailable<br/>Success < 50%| UNAVAILABLE[Skip<br/>━━━━━━━━━━━━━━<br/>Use Fallback]

    HEALTHY --> RETURN1[Return Filtered PSPs]
    DEGRADED --> RETURN1
    UNAVAILABLE --> FALLBACK_OPTION[Manual Bank Transfer<br/>━━━━━━━━━━━━━━<br/>Settlement: T+2]

    RETURN1 --> END1[Proceed to Step 3: Scoring]
    FALLBACK_OPTION --> END2[Notify Ops Team]

    style US fill:#B0E0E6
    style CARD fill:#FFE4B5
    style HEALTHY fill:#90EE90
    style DEGRADED fill:#FFD93D
    style UNAVAILABLE fill:#FFB6C1
    style CRYPTO fill:#DDA0DD
```markdown

**過濾策略說明**：
- **地理優先**：優先使用本地 PSP（降低跨境費率、提升成功率）
- **支付方式匹配**：確保 PSP 支持玩家選擇的支付方式
- **健康狀態過濾**：Success Rate < 50% 的 PSP 直接排除

---

##### 子流程圖 2：多維度評分算法 (Multi-Dimensional Scoring Algorithm)

```mermaid
flowchart TD
    START[Step 3: Calculate Score for Each PSP] --> INIT[Initialize Score = 0]

    INIT --> DIM1{Dimension 1<br/>Success Rate (24h)<br/>━━━━━━━━━━━━━━<br/>Weight: 50%}

    DIM1 -->|>= 95%| S1A[Score += 50 × 0.95<br/>= 47.5]
    DIM1 -->|90-94%| S1B[Score += 50 × 0.92<br/>= 46.0]
    DIM1 -->|85-89%| S1C[Score += 50 × 0.88<br/>= 44.0]
    DIM1 -->|80-84%| S1D[Score += 50 × 0.85<br/>= 42.5]
    DIM1 -->|< 80%| S1E[Score += 50 × 0.75<br/>= 37.5]

    S1A --> DIM2
    S1B --> DIM2
    S1C --> DIM2
    S1D --> DIM2
    S1E --> DIM2

    DIM2{Dimension 2<br/>Fee Rate<br/>━━━━━━━━━━━━━━<br/>Weight: 30%}

    DIM2 -->|< 2%| S2A[Score += 30 × 0.98<br/>= 29.4]
    DIM2 -->|2-3%| S2B[Score += 30 × 0.95<br/>= 28.5]
    DIM2 -->|3-4%| S2C[Score += 30 × 0.92<br/>= 27.6]
    DIM2 -->|4-5%| S2D[Score += 30 × 0.90<br/>= 27.0]
    DIM2 -->|> 5%| S2E[Score += 30 × 0.80<br/>= 24.0]

    S2A --> DIM3
    S2B --> DIM3
    S2C --> DIM3
    S2D --> DIM3
    S2E --> DIM3

    DIM3{Dimension 3<br/>Settlement Speed<br/>━━━━━━━━━━━━━━<br/>Weight: 15%}

    DIM3 -->|< 5 min| S3A[Score += 15 × 1.0<br/>= 15.0]
    DIM3 -->|5-15 min| S3B[Score += 15 × 0.8<br/>= 12.0]
    DIM3 -->|15-30 min| S3C[Score += 15 × 0.6<br/>= 9.0]
    DIM3 -->|30-60 min| S3D[Score += 15 × 0.5<br/>= 7.5]
    DIM3 -->|> 60 min| S3E[Score += 15 × 0.2<br/>= 3.0]

    S3A --> DIM4
    S3B --> DIM4
    S3C --> DIM4
    S3D --> DIM4
    S3E --> DIM4

    DIM4{Dimension 4<br/>VIP Channel<br/>━━━━━━━━━━━━━━<br/>Weight: 5%}

    DIM4 -->|VIP >= 3<br/>AND<br/>PSP has VIP channel| S4A[Score += 5.0<br/>🌟 VIP Bonus]
    DIM4 -->|Otherwise| S4B[Score += 0]

    S4A --> DIM5
    S4B --> DIM5

    DIM5{Dimension 5<br/>Currency Match<br/>━━━━━━━━━━━━━━<br/>Weight: 3%}

    DIM5 -->|Exact Match<br/>No FX Fee| S5A[Score += 3.0]
    DIM5 -->|Need Conversion<br/>FX Fee Applied| S5B[Score += 0]

    S5A --> TOTAL
    S5B --> TOTAL

    TOTAL[Calculate Total Score<br/>━━━━━━━━━━━━━━<br/>Range: 0-103<br/>Typical: 70-95] --> EXAMPLE[Scoring Example:<br/>━━━━━━━━━━━━━━<br/>Nuvei<br/>Success 95%: 47.5<br/>Fee 2.5%: 28.5<br/>Speed 10min: 12.0<br/>VIP: 0<br/>Currency Match: 3.0<br/>━━━━━━━━━━━━━━<br/>Total: 91.0 ✅]

    EXAMPLE --> RANK[Rank All PSPs by Score DESC]

    RANK --> SELECT[Select Top 1 PSP]

    SELECT --> HEALTH_CHECK{Final Health Check}

    HEALTH_CHECK -->|Healthy| ROUTE[✅ Route to Selected PSP]
    HEALTH_CHECK -->|Degraded/Down| RETRY[Retry with 2nd Ranked PSP]

    RETRY --> HEALTH_CHECK

    ROUTE --> LOG[Log Decision + Fallback Queue]
    LOG --> END[Return PSP Details]

    style DIM1 fill:#FFE4B5
    style DIM2 fill:#ADD8E6
    style DIM3 fill:#DDA0DD
    style DIM4 fill:#FFD700
    style DIM5 fill:#90EE90
    style EXAMPLE fill:#E6E6FA
    style ROUTE fill:#90EE90
    style RETRY fill:#FFD93D
```text

**評分權重說明**：
- **成功率 (50%)**：最重要指標，直接影響玩家體驗與平台損失
- **費率 (30%)**：成本控制，高額交易時影響顯著（$10k × 3% = $300）
- **速度 (15%)**：用戶體驗，快速到帳提升滿意度與留存率
- **VIP 通道 (5%)**：差異化服務，高價值玩家專屬優先級
- **貨幣匹配 (3%)**：避免匯損，降低 FX 風險與手續費

**計算範例**（美國玩家，$1000 Credit Card 充值）：

| PSP | Success Rate | Fee Rate | Speed | VIP | Currency | **Total** |
|-----|-------------|----------|-------|-----|----------|-----------|
| **Nuvei** | 95% → 47.5 | 2.5% → 28.5 | 10min → 12.0 | No → 0 | USD → 3.0 | **91.0** ✅ |
| **Adyen** | 92% → 46.0 | 3.0% → 27.0 | 5min → 15.0 | No → 0 | EUR → 0 | **88.0** |
| **Stripe** | 90% → 45.0 | 2.9% → 27.6 | 15min → 7.5 | Yes → 5.0 | USD → 3.0 | **88.1** |

**決策結果**：Nuvei 得分最高（91.0），路由至 Nuvei；Fallback 隊列：[Adyen, Stripe]

---

**路由評分權重配置**：

| 評分維度 | 權重 % | 計算公式 | 業務考量 |
|---------|-------|---------|---------|
| **成功率** | 50% | `success_rate × 50` | 最重要：直接影響玩家體驗與平台損失 |
| **手續費** | 30% | `(1 - fee_rate) × 30` | 成本控制：高額交易時影響顯著 |
| **到帳速度** | 15% | `(1 - settlement_time/60) × 15` | 用戶體驗：快速到帳提升滿意度 |
| **VIP 專屬通道** | 5% | `+5` (VIP ≥ 3 且 PSP 支持) | 差異化服務：提升 VIP 留存率 |
| **貨幣匹配** | 3% | `+3` (無需換匯) | 避免匯損：降低 FX 風險 |

**評分計算範例** (美國玩家，$1000 Credit Card 存款)：

```
Nuvei:
  Success Rate: 95% → 95% × 50 = 47.5
  Fee: 2.5% → (1 - 0.025) × 30 = 29.25
  Settlement: 10 min → (1 - 10/60) × 15 = 12.5
  VIP Bonus: No → 0
  Currency Match: USD → +3
  Total Score: 92.25 ✅ Selected

Adyen:
  Success Rate: 92% → 46.0
  Fee: 3.0% → 29.1
  Settlement: 5 min → 13.75
  VIP Bonus: No → 0
  Currency Match: EUR (need convert) → 0
  Total Score: 88.85

Stripe:
  Success Rate: 90% → 45.0
  Fee: 2.9% → 29.13
  Settlement: 15 min → 11.25
  VIP Bonus: No → 0
  Currency Match: USD → +3
  Total Score: 88.38
```text

**故障轉移決策表**：

| PSP 狀態 | 成功率 | 路由決策 | 降級時間 | 恢復策略 |
|---------|-------|---------|---------|---------|
| **Healthy** | >= 80% | 正常路由 | N/A | N/A |
| **Degraded** | 50-80% | 降低優先級（Score × 0.7） | 持續 30 分鐘 | 連續 3 次成功恢復 |
| **Unavailable** | < 50% | 完全排除，使用 Backup | 持續 1 小時 | 人工驗證後恢復 |
| **Down** | No Response | 跳過，使用下一順位 | 持續故障 | 緊急告警 + 人工處理 |

**地區 vs PSP 優先級矩陣**：

| 地區 | 首選 PSP | 備選 PSP | 特殊要求 | 成功率基準 |
|------|---------|---------|---------|-----------|
| 🇺🇸 **US** | Nuvei | Stripe, PayPal | PCI-DSS Level 1 | > 92% |
| 🇪🇺 **EU** | Adyen | Trustly, Klarna | PSD2 3DS 2.0 | > 90% |
| 🇨🇳 **CN** | Alipay | WeChat Pay | 需商戶資質 | > 95% |
| 🇵🇭 **PH** | GCash | PayMaya | E-Wallet 主導 | > 88% |
| 🇧🇷 **BR** | MercadoPago | PagSeguro | PIX 即時轉帳 | > 85% |
| 🌐 **Global** | Stripe | Adyen, Nuvei | 多貨幣支持 | > 90% |

**VIP 專屬通道範例**：

| VIP 等級 | 專屬 PSP | 額外優勢 | Score Bonus |
|---------|---------|---------|------------|
| **VIP 5** | Adyen Premium | 專屬客戶經理 + 優先處理 | +5 |
| **VIP 4** | Nuvei VIP | 手續費 -0.5% | +5 |
| **VIP 3** | Stripe Priority | 快速到帳 (< 5 min) | +5 |
| **VIP 1-2** | Standard PSPs | 無額外優勢 | +0 |

### 5.2 地區與支付方式矩陣

**不同地區推薦不同PSP**：
| 地區 | 推薦PSP | 主要支付方式 | 註釋 |
|------|---------|------------|------|
| 🇺🇸 美國 | Stripe, Nuvei | Credit Card, ACH | PCI-DSS Level 1 必需 |
| 🇪🇺 歐洲 | Adyen, Trustly | SEPA, iDEAL, Sofort | 符合 PSD2 強認證 |
| 🇨🇳 中國 | Alipay, WeChat Pay | 掃碼支付 | 需要商戶資質認證 |
| 🇵🇭 菲律賓 | GCash, PayMaya | E-Wallet | 高現金使用率市場 |
| 🇧🇷 巴西 | PagSeguro, MercadoPago | Boleto, PIX | PIX 即時轉帳主導 |

---

## 6. 支付通道故障轉移 (Failover)

### 6.1 健康檢查機制

**定時健康探測**（每 5 分鐘執行）：

**降級規則**：

### 6.2 自動切換策略

**Primary → Backup 切換**：
```
Primary PSP: Nuvei (Status: Unavailable)
    ↓ 自動切換
Backup PSP: Adyen (Status: Healthy)
    ↓ 如果也失敗
Fallback PSP: Manual Bank Transfer（通知財務團隊）
```sql

**恢復檢測**：
- 每 10 分鐘測試一次已降級的 PSP
- 連續 3 次成功後自動恢復 `available` 狀態

---

## 7. 支付安全

### 7.1 3D Secure (3DS) 實施

**3DS 1.0 vs 3DS 2.0 對比**：
| 特性 | 3DS 1.0 | 3DS 2.0 (EMV 3DS) |
|------|---------|------------------|
| 用戶體驗 | 跳轉銀行頁面（高放棄率）| 原生 App 內驗證 |
| 數據傳輸 | 僅基本卡片資訊 | 包含設備指紋、行為數據 |
| 風險評估 | 發卡行單方決定 | 多方協作（發卡行+PSP+商戶）|
| 適用場景 | 桌面端 | 移動端優先 |

**風險豁免（Exemption）條件**：
根據 PSD2，以下情況可豁免 3DS：
- 交易金額 < €30
- 商戶風控評分極高（低風險商戶）
- 經常性付款（Subscription）

### 7.2 PCI-DSS 合規

**Level 1 要求**（年交易量 > 600萬筆）：
1. ❌ **禁止儲存完整卡號**：
   - 僅允許儲存 Token（由 PSP 提供）
   - 範例：`card_token = "tok_1A2B3C4D5E6F"`

2. ✅ **使用 PSP Hosted Payment Page**：
   - 不直接處理卡片數據，跳轉至 PSP 的安全頁面
   - 範例：Stripe Checkout、Adyen Drop-in

3. ✅ **加密傳輸**：
   - 所有 API 請求必須使用 TLS 1.2+
   - 定期更新 SSL 證書（Let's Encrypt / DigiCert）

4. ✅ **存取控制**：
   - 支付系統資料庫只允許特定 IP 存取
   - 使用 Vault 管理 API 密鑰（如 HashiCorp Vault）

---

## 8. 掉單補單機制

### 8.1 主動對帳任務

**Cron Job（每 15 分鐘執行）**：

### 8.2 玩家申訴處理

**申訴流程**：
1. 玩家上傳支付憑證（銀行轉帳截圖）
2. 客服調用 PSP API 查詢
3. 若 PSP 確認收款但平台未入帳 → 手動補單 + 記錄審計日誌

#### 8.2.1 對帳與補單流程圖 (Reconciliation & Manual Credit Flow)

以下流程圖展示了定時對帳任務的完整邏輯。為提升可讀性，將複雜對帳流程拆分為 **主流程圖 + 2 個子流程圖**。

##### 主流程圖：對帳任務整體架構 (Reconciliation Task Overview)

```mermaid
flowchart TD
    START[⏰ Cron Job<br/>━━━━━━━━━━━━━━<br/>Trigger: Every 15 minutes<br/>Target: Pending > 30 min] --> QUERY[Query Pending Transactions<br/>━━━━━━━━━━━━━━<br/>SELECT * FROM transactions<br/>WHERE status = 'PENDING'<br/>AND created_at < NOW() - 30min]

    QUERY --> CHECK{Found Pending<br/>Transactions?}

    CHECK -->|No| END1[✅ End<br/>━━━━━━━━━━━━━━<br/>No Action Needed<br/>Next Run: 15 min]

    CHECK -->|Yes| COUNT[Pending Count: 25<br/>━━━━━━━━━━━━━━<br/>Begin Reconciliation Loop]

    COUNT --> LOOP[For Each Transaction<br/>━━━━━━━━━━━━━━<br/>Query PSP Status via API]

    LOOP --> PSP_API[PSP Query Result<br/>━━━━━━━━━━━━━━<br/>GET /api/v1/query<br/>HMAC Signature Auth]

    PSP_API --> ROUTE{PSP Status?}

    ROUTE -->|✅ SUCCESS| PATH_SUCCESS[🔄 Auto补单 Flow<br/>━━━━━━━━━━━━━━<br/>PSP confirmed but not credited<br/>→ Proceed to补单 Process]

    ROUTE -->|❌ FAILED| PATH_FAILED[Update Status<br/>━━━━━━━━━━━━━━<br/>Mark as FAILED<br/>Notify Player<br/>No补单 Needed]

    ROUTE -->|⏳ PENDING| PATH_PENDING[Continue Waiting<br/>━━━━━━━━━━━━━━<br/>Check Duration:<br/>< 2h: Wait<br/>>= 2h: Alert CS Team]

    ROUTE -->|🔍 NOT_FOUND| PATH_NOT_FOUND[🚨 Manual Review Flow<br/>━━━━━━━━━━━━━━<br/>PSP has no record<br/>→ Proceed to Appeal Process]

    ROUTE -->|⚠️ API ERROR| PATH_ERROR[Retry Logic<br/>━━━━━━━━━━━━━━<br/>Retry < 3: Wait 5min<br/>Retry >= 3: Escalate]

    PATH_SUCCESS --> AUTO补单[See: Auto补单 Flow<br/>━━━━━━━━━━━━━━<br/>Idempotency + Lock + Credit]
    PATH_NOT_FOUND --> MANUAL_REVIEW[See: Manual Review Flow<br/>━━━━━━━━━━━━━━<br/>Appeal + Verify + Approval]

    AUTO补单 --> RESULT1[Result: 补单 Success/Failed]
    MANUAL_REVIEW --> RESULT2[Result: Approved/Rejected]
    PATH_FAILED --> RESULT3[Result: Marked Failed]
    PATH_PENDING --> RESULT4[Result: Still Pending]
    PATH_ERROR --> RESULT5[Result: Retry/Escalated]

    RESULT1 --> NEXT
    RESULT2 --> NEXT
    RESULT3 --> NEXT
    RESULT4 --> NEXT
    RESULT5 --> NEXT

    NEXT{More Pending<br/>Transactions?}

    NEXT -->|Yes| LOOP
    NEXT -->|No| SUMMARY[Generate Report<br/>━━━━━━━━━━━━━━<br/>Total Checked: 25<br/>补单 Success: 5<br/>Failed: 3<br/>Still Pending: 15<br/>Manual Review: 2]

    SUMMARY --> REPORT[Send to Finance Team<br/>━━━━━━━━━━━━━━<br/>Daily Report @ 08:00 AM<br/>Email + Dashboard]

    REPORT --> END2[✅ End<br/>━━━━━━━━━━━━━━<br/>Reconciliation Complete<br/>Next Run: 15 min]

    style PATH_SUCCESS fill:#90EE90
    style PATH_FAILED fill:#FFB6C1
    style PATH_PENDING fill:#FFD93D
    style PATH_NOT_FOUND fill:#FF6B6B
    style PATH_ERROR fill:#DDA0DD
    style SUMMARY fill:#E6E6FA
```markdown

**對帳主流程關鍵節點**：
- **觸發頻率**：每 15 分鐘掃描 Pending > 30 分鐘的訂單
- **PSP 狀態分類**：SUCCESS (補單) / FAILED (標記) / PENDING (等待) / NOT_FOUND (人工審核) / ERROR (重試)
- **並行處理**：使用 Redis 分散式鎖避免重複補單
- **報表生成**：每日 08:00 AM 發送彙總報表至財務團隊

---

##### 子流程圖 1：自動補單處理流程 (Auto Manual Credit Flow)

```mermaid
flowchart TD
    START[Auto补单 Trigger<br/>━━━━━━━━━━━━━━<br/>Condition: PSP Status = SUCCESS<br/>Platform Status = PENDING] --> IDEMPOTENT{Idempotent Check<br/>━━━━━━━━━━━━━━<br/>Already Credited?}

    IDEMPOTENT -->|Yes| SKIP[Skip补单<br/>━━━━━━━━━━━━━━<br/>Log: Duplicate Attempt<br/>Reason: Already Processed<br/>Action: None]

    IDEMPOTENT -->|No| LOCK[Acquire Redis Lock<br/>━━━━━━━━━━━━━━<br/>Key: reconcile:txn_{id}<br/>Command: SET NX<br/>TTL: 300 seconds]

    LOCK --> LOCK_CHECK{Lock Acquired?}

    LOCK_CHECK -->|No| SKIP2[Skip补单<br/>━━━━━━━━━━━━━━<br/>Reason: Another Job Processing<br/>Action: Wait Next Cycle]

    LOCK_CHECK -->|Yes| DB_TXN[BEGIN DB Transaction<br/>━━━━━━━━━━━━━━<br/>Isolation: READ_COMMITTED]

    DB_TXN --> UPDATE_TXN[UPDATE transactions SET<br/>━━━━━━━━━━━━━━<br/>status = 'SUCCESS',<br/>psp_transaction_id = ?,<br/>补单_flag = TRUE,<br/>补单_at = NOW(),<br/>completed_at = NOW()<br/>WHERE id = ? AND status = 'PENDING']

    UPDATE_TXN --> AFFECTED{Affected Rows?}

    AFFECTED -->|0 rows| ROLLBACK[ROLLBACK Transaction<br/>━━━━━━━━━━━━━━<br/>Reason: Already Updated<br/>Release Lock]

    AFFECTED -->|1 row| CREDIT[Credit Player Balance<br/>━━━━━━━━━━━━━━<br/>wallet_service.credit(<br/>  player_id,<br/>  amount,<br/>  source: 'RECONCILIATION'<br/>)]

    CREDIT --> CREDIT_CHECK{Credit Success?}

    CREDIT_CHECK -->|Failed| ROLLBACK2[ROLLBACK Transaction<br/>━━━━━━━━━━━━━━<br/>Reason: Wallet Service Error<br/>Action: Retry Later]

    CREDIT_CHECK -->|Success| AUDIT[Insert Audit Log<br/>━━━━━━━━━━━━━━<br/>INSERT INTO payment_audit_log<br/>(transaction_id, event, operator, details)<br/>VALUES (?, 'RECONCILIATION_CREDITED',<br/>'SYSTEM', JSON)]

    AUDIT --> COMMIT[COMMIT Transaction<br/>━━━━━━━━━━━━━━<br/>Status: Success<br/>Balance Updated]

    COMMIT --> NOTIFY_PLAYER[Notify Player<br/>━━━━━━━━━━━━━━<br/>Channel: Email + SMS<br/>Subject: "Deposit Credited (Delayed)"<br/>Content: "Your $100 deposit has been credited"]

    NOTIFY_PLAYER --> NOTIFY_FINANCE[Alert Finance Team<br/>━━━━━━━━━━━━━━<br/>Channel: Slack + Email<br/>Info: 补单 Success<br/>Transaction ID: ?<br/>Reason: Webhook Not Received]

    NOTIFY_FINANCE --> RELEASE[Release Redis Lock<br/>━━━━━━━━━━━━━━<br/>Command: DEL reconcile:txn_{id}]

    RELEASE --> SUCCESS[✅ 补单 Successful<br/>━━━━━━━━━━━━━━<br/>Balance: +$100<br/>Status: SUCCESS<br/>Flag: 补单_flag = TRUE]

    ROLLBACK --> ERROR1[❌ 补单 Failed<br/>━━━━━━━━━━━━━━<br/>Reason: Already Updated<br/>Action: Skip]

    ROLLBACK2 --> ERROR2[❌ 补单 Failed<br/>━━━━━━━━━━━━━━<br/>Reason: Wallet Error<br/>Action: Retry Next Cycle]

    SKIP --> END1[End - Skipped]
    SKIP2 --> END1
    SUCCESS --> END2[End - Success]
    ERROR1 --> END3[End - Error]
    ERROR2 --> END3

    style SUCCESS fill:#90EE90
    style ERROR1 fill:#FFB6C1
    style ERROR2 fill:#FF6B6B
    style COMMIT fill:#ADD8E6
    style ROLLBACK fill:#FFD93D
    style ROLLBACK2 fill:#FFD93D
```markdown

**自動補單關鍵設計**：
- **冪等性保證**：通過 `補單_flag` 欄位防止重複加錢
- **分散式鎖**：Redis SET NX 防止並發補單（TTL 5 分鐘）
- **事務保證**：DB 事務確保狀態更新與餘額增加原子性
- **通知機制**：補單成功後通知玩家（郵件+簡訊）與財務團隊（Slack）

---

##### 子流程圖 2：人工審核與申訴流程 (Manual Review & Appeal Flow)

```mermaid
flowchart TD
    START[Manual Review Trigger<br/>━━━━━━━━━━━━━━<br/>Condition: PSP Status = NOT_FOUND<br/>PSP has no transaction record] --> SEVERITY{Amount Severity?}

    SEVERITY -->|>= $1000<br/>High Value| CRITICAL[🔴 CRITICAL Alert<br/>━━━━━━━━━━━━━━<br/>Notify: Finance + Security + CTO<br/>Priority: HIGH<br/>SLA: 2 hours]

    SEVERITY -->|< $1000<br/>Low Value| STANDARD[🟡 STANDARD Alert<br/>━━━━━━━━━━━━━━<br/>Notify: CS Team<br/>Priority: MEDIUM<br/>SLA: 24 hours]

    CRITICAL --> TICKET[Create Review Ticket<br/>━━━━━━━━━━━━━━<br/>System: Jira<br/>Type: Payment Investigation<br/>Assignee: Finance Team<br/>Fields: {txn_id, amount, player_id, psp}]

    STANDARD --> TICKET

    TICKET --> APPEAL{Player Appeals?<br/>━━━━━━━━━━━━━━<br/>Timeout: 7 days}

    APPEAL -->|No - Timeout| TIMEOUT[Close Ticket<br/>━━━━━━━━━━━━━━<br/>Status: EXPIRED<br/>Reason: No Player Response<br/>Action: Mark as FAILED]

    APPEAL -->|Yes - Upload Receipt| VERIFY[CS Agent Verifies Receipt<br/>━━━━━━━━━━━━━━<br/>Check:<br/>• Bank Reference Number<br/>• Transaction Amount<br/>• Transaction Date<br/>• Payment Method]

    VERIFY --> CONTACT_PSP[Contact PSP Support<br/>━━━━━━━━━━━━━━<br/>Action: Submit Ticket to PSP<br/>Evidence: Bank Receipt<br/>Wait: 1-3 business days]

    CONTACT_PSP --> PSP_CONFIRM{PSP Confirms<br/>Payment?}

    PSP_CONFIRM -->|No - Not Found| REJECT[Reject Appeal<br/>━━━━━━━━━━━━━━<br/>Status: REJECTED<br/>Reason: No Payment Proof from PSP<br/>Notify Player: Email]

    PSP_CONFIRM -->|Yes - Confirmed| MANUAL补单[Manual补单 Request<br/>━━━━━━━━━━━━━━<br/>Operator: CS Agent<br/>Evidence: PSP Confirmation Email<br/>Audit Trail: Logged]

    MANUAL补单 --> APPROVAL{Approval Required?<br/>━━━━━━━━━━━━━━<br/>Threshold: $1000}

    APPROVAL -->|No<br/>(Amount < $1000)| EXECUTE[Execute补单<br/>━━━━━━━━━━━━━━<br/>Same as Auto补单 Flow<br/>Operator: CS Agent]

    APPROVAL -->|Yes<br/>(Amount >= $1000)| AWAIT[Await CFO Approval<br/>━━━━━━━━━━━━━━<br/>Approval System: Workflow<br/>Approver: CFO<br/>SLA: 24 hours]

    AWAIT --> APPROVED{Approved?}

    APPROVED -->|No - Rejected| REJECT2[Reject Appeal<br/>━━━━━━━━━━━━━━<br/>Status: CFO_REJECTED<br/>Reason: Insufficient Evidence<br/>Notify Player + CS]

    APPROVED -->|Yes - Approved| EXECUTE

    EXECUTE --> CREDIT[Credit Player Balance<br/>━━━━━━━━━━━━━━<br/>Source: MANUAL_CREDIT<br/>Operator: {cs_agent_id}<br/>Approver: {cfo_id if required}]

    CREDIT --> CREDIT_CHECK{Credit Success?}

    CREDIT_CHECK -->|Failed| ERROR[❌ 补单 Failed<br/>━━━━━━━━━━━━━━<br/>Reason: Wallet Service Error<br/>Action: Escalate to Tech Team]

    CREDIT_CHECK -->|Success| AUDIT[Insert Audit Log<br/>━━━━━━━━━━━━━━<br/>Event: MANUAL_CREDIT<br/>Evidence: {psp_confirmation, bank_receipt}<br/>Approver: {cfo_id}<br/>Compliance: 7-year retention]

    AUDIT --> NOTIFY[Notify Stakeholders<br/>━━━━━━━━━━━━━━<br/>Player: Email + SMS<br/>Finance: Slack Alert<br/>Audit: Log to SIEM]

    NOTIFY --> SUCCESS[✅ Manual补单 Complete<br/>━━━━━━━━━━━━━━<br/>Status: SUCCESS<br/>Flag: manual_credit = TRUE<br/>Audit Trail: Complete]

    TIMEOUT --> END1[End - Expired]
    REJECT --> END2[End - Rejected]
    REJECT2 --> END2
    SUCCESS --> END3[End - Success]
    ERROR --> END4[End - Error]

    style CRITICAL fill:#FF6B6B
    style STANDARD fill:#FFD93D
    style SUCCESS fill:#90EE90
    style REJECT fill:#FFB6C1
    style REJECT2 fill:#FFB6C1
    style ERROR fill:#FF6B6B
    style AUDIT fill:#E6E6FA
```text

**人工審核關鍵流程**：
- **嚴重性分級**：$1000 以上觸發 CRITICAL 告警（通知 CTO）
- **申訴機制**：玩家上傳銀行收據 → CS 驗證 → 聯繫 PSP 確認
- **審批工作流**：$1000 以上需 CFO 批准（防止內部欺詐）
- **審計合規**：所有人工補單記錄留存 7 年（監管要求）

**對帳與補單關鍵指標**：

| 指標 | 定義 | 目標值 | 告警閾值 | 監控頻率 |
|------|------|-------|---------|---------|
| **掉單率** | (补单數 / 總成功數) × 100% | < 1% | > 3% | 每日 |
| **补单成功率** | (补单成功 / 补單嘗試) × 100% | > 95% | < 90% | 每日 |
| **平均补單延遲** | 從 PSP 成功到平台入帳的時間 | < 30 min | > 2 hours | 實時 |
| **Pending 積壓** | status='PENDING' 且 > 2 小時的訂單數 | < 10 | > 50 | 每 15 分鐘 |
| **手動審核率** | (手動處理 / 總補單) × 100% | < 5% | > 15% | 每日 |
| **PSP API 成功率** | (成功查詢 / 總查詢) × 100% | > 99% | < 95% | 實時 |

**補單場景分類與處理策略**：

| 場景 | PSP 狀態 | 平台狀態 | 處理策略 | 優先級 | 通知對象 |
|------|---------|---------|---------|--------|---------|
| **1. Webhook 掉單** | SUCCESS | PENDING | 自動補單 + 通知玩家 | 🟢 P2 | Player |
| **2. 狀態同步失敗** | SUCCESS | FAILED | 回滾狀態 + 補單 | 🟡 P1 | Player + CS |
| **3. PSP 延遲回調** | PENDING | PENDING | 繼續等待（< 2h） | 🟢 P3 | 無 |
| **4. PSP 超時** | PENDING | PENDING (> 2h) | 人工調查 | 🟡 P1 | CS Team |
| **5. PSP 無記錄** | NOT_FOUND | PENDING | 高額：緊急告警<br/>低額：標準告警 | 🔴 P0 / 🟡 P1 | Finance + Security |
| **6. 玩家申訴** | SUCCESS (經驗證) | PENDING | 手動補單 + CFO 審批 (> $1k) | 🟡 P1 | CFO + CS |
| **7. PSP API 故障** | Error / Timeout | PENDING | 重試 3 次 → 技術升級 | 🔴 P0 | Tech Team |

**補單冪等性保護**：

```
Layer 1: Redis 分散式鎖
  ├─ SET NX reconcile:{txn_id} 1 EX 300 (5 分鐘)
  └─ 防止多個 Cron Job 同時補單同一筆訂單

Layer 2: 資料庫狀態檢查
  ├─ UPDATE ... WHERE status = 'PENDING'
  └─ 若 status 已變更（如已補單），Affected Rows = 0

Layer 3: Wallet Service 冪等性
  ├─ 使用 transaction_id 作為 ref_id
  └─ 若已存在相同 ref_id 的入帳記錄，拒絕重複入帳
```text

**手動補單審批規則**：

| 金額範圍 | 審批層級 | 審批時限 | 所需證據 | 備註 |
|---------|---------|---------|---------|------|
| **< $100** | CS Agent | 即時 | 玩家申訴 + 銀行截圖 | 自動通過 |
| **$100 - $1000** | CS Manager | 1 小時 | 銀行截圖 + PSP 郵件確認 | 需二次驗證 |
| **$1000 - $10k** | CFO | 4 小時 | 完整銀行流水 + PSP 官方證明 | 高風險審核 |
| **> $10k** | CFO + CEO | 24 小時 | 完整銀行流水 + PSP 官方證明 + 視頻會議 | 最高級別審批 |

**異常告警分級**：

| 告警等級 | 觸發條件 | 通知方式 | 響應時間 | 處理團隊 |
|---------|---------|---------|---------|---------|
| 🔴 **P0 - Critical** | 高額訂單 NOT_FOUND<br/>PSP API 完全不可用 | Phone + SMS + Email | 15 分鐘 | Finance + CTO + Security |
| 🟡 **P1 - High** | 掉單率 > 3%<br/>Pending 積壓 > 50 筆 | Email + Slack | 1 小時 | Finance + CS Manager |
| 🟢 **P2 - Medium** | 單筆掉單成功補單 | Email (Daily Summary) | 24 小時 | Finance (日報) |
| 🟢 **P3 - Low** | PSP PENDING 狀態 | 無通知 | 監控即可 | 無 |

**補單審計日誌範例**：


**每日對帳報告範例**：

```
Reconciliation Report - 2026-01-27

Summary:
- Total Pending Checked: 125 transactions
- Auto补单 Success: 8 (6.4%)
- PSP Failed: 15 (12%)
- Still Pending: 95 (76%)
- Manual Review Required: 5 (4%)
- PSP API Errors: 2 (1.6%)

补单 Details:
- Average补单 Delay: 38 minutes
- Max補單 Delay: 2.5 hours (txn_20260127_045)
- Total Amount 补单: $12,350

Alerts:
🟡 WARNING: 3 high-value transactions (> $1000) pending > 2 hours
🟢 INFO: PSP API error rate: 1.6% (within acceptable range)

Action Required:
- Review 5 manual review cases (Assigned: CS Team)
- Investigate txn_20260127_045 (2.5h delay)
```markdown

---

## 9. 審批流程

### 9.1 配置變更審批

**敏感操作清單**：
- **新增/修改 PSP**：修改商戶號/密鑰為高風險操作
- **調整路由權重**：可能影響成本與成功率
- **變更限額**：單筆/單日限額調整

**審批流程**（引用 09-04 審批工作流系統）：
```
1. 技術人員提交變更申請
   ↓
2. 系統自動試算影響範圍（預估受影響交易量）
   ↓
3. 財務總監/CTO 審核
   - 批准：進入排程
   - 拒絕：返回修訂
   ↓
4. 生效後自動通知運營團隊
```text

---

## 10. API 設計規範

### 10.1 統一存款 API

**請求格式**（參考 12-05 API 設計標準）：
```http
POST /api/v1/payments/deposit
Content-Type: application/json
Authorization: Bearer <player_jwt_token>

{
  "amount": 100.00,
  "currency": "USD",
  "payment_method": "credit_card",  // credit_card, e_wallet, crypto
  "psp_code": "nuvei",  // 可選：指定 PSP，否則自動路由
  "return_url": "https://platform.com/deposit/callback"
}
```text

**響應格式**：
```json
{
  "code": 1000,
  "message": "Success",
  "data": {
    "transaction_id": "txn_202601271234",
    "redirect_url": "https://psp.com/payment?token=abc123",
    "expires_at": "2026-01-27T11:00:00Z"
  }
}
```

### 10.2 Webhook 回調規範

**安全要求**：
1. **簽名驗證**：

2. **冪等性保護**：
   - 使用 `transaction_id` 作為唯一鍵，防止重複處理
   - Redis 鎖：`SET callback:{txn_id} 1 EX 60 NX`

---

## 📚 相關文檔

### 業務邏輯參考
- [02-06 統一錢包模型](./02-06_Unified_Wallet_Model.md) - 存款入帳錢包邏輯
- [02-01 出金風控](./02-01_Withdrawal_Risk_Control.md) - 提款流程與風控
- [02-03 對賬系統](./02-03_Reconciliation_System.md) - PSP 對賬流程

### 技術架構參考
- [09-03 數據安全標準](../09_System_Security/09-03_Data_Security_Standard.md) - 支付數據加密
- [12-05 API 設計標準](../12_Technical_Operations/12-05_API_Design_Standard.md) - API 規範
- [09-02 審計日誌系統](../09_System_Security/09-02_Audit_Log_System.md) - 配置變更審計
- [09-04 審批工作流系統](../09_System_Security/09-04_Approval_Workflow_System.md) - 配置變更審批

---

**文檔版本**: 1.1.0
**最後更新**: 2026-01-27
**維護團隊**: Finance Team & Backend Team
