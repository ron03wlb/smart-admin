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
```
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
```

**Callback 處理**：
```python
def nuvei_callback(request):
    # 1. 驗證簽名
    expected_checksum = sha256(
        f"{request['merchantId']}{request['totalAmount']}"
        f"{request['currency']}{request['responseTimeStamp']}{MERCHANT_SECRET}"
    ).hexdigest()

    if request['checksum'] != expected_checksum:
        return {"status": "error", "message": "Invalid signature"}

    # 2. 更新訂單狀態
    transaction = Transaction.objects.get(id=request['clientRequestId'])
    if request['transactionStatus'] == 'APPROVED':
        transaction.status = 'success'
        transaction.psp_transaction_id = request['transactionId']
        transaction.save()

        # 3. 增加玩家餘額（引用 02-06 統一錢包模型）
        wallet_service.credit(player_id=transaction.player_id, amount=transaction.amount)

    return {"status": "ok"}
```

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
```

---

## 5. 智能路由算法詳解

### 5.1 路由決策矩陣

**多維度評分模型**：
```python
def calculate_psp_score(psp, player, amount):
    score = 0

    # 1. 成功率權重（50%）
    recent_success_rate = get_success_rate(psp, last_hours=24)
    score += recent_success_rate * 50

    # 2. 成本權重（30%）
    fee_rate = psp.fee_percentage + (psp.fixed_fee / amount)
    score += (1 - fee_rate) * 30

    # 3. 速度權重（15%）
    avg_settlement_time = psp.avg_settlement_minutes
    score += (1 - min(avg_settlement_time / 60, 1)) * 15

    # 4. VIP 專用通道加成（5%）
    if player.vip_level >= 3 and psp.is_vip_channel:
        score += 5

    return score

# 選擇最高分的 PSP
best_psp = max(available_psps, key=lambda p: calculate_psp_score(p, player, amount))
```

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
```sql
-- 計算過去 1 小時每個 PSP 的成功率
SELECT
    psp_code,
    COUNT(*) AS total_transactions,
    SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) AS successful_transactions,
    (SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END)::DECIMAL / COUNT(*)) AS success_rate
FROM transactions
WHERE created_at > NOW() - INTERVAL '1 hour'
GROUP BY psp_code;
```

**降級規則**：
```python
if success_rate < 0.80:  # 成功率低於 80%
    psp.status = 'degraded'
    alert_ops_team(f"PSP {psp.code} success rate dropped to {success_rate}")

if success_rate < 0.50:  # 成功率低於 50%
    psp.status = 'unavailable'
    switch_to_backup_psp(psp)
```

### 6.2 自動切換策略

**Primary → Backup 切換**：
```
Primary PSP: Nuvei (Status: Unavailable)
    ↓ 自動切換
Backup PSP: Adyen (Status: Healthy)
    ↓ 如果也失敗
Fallback PSP: Manual Bank Transfer（通知財務團隊）
```

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
```python
def reconcile_pending_transactions():
    # 查詢超過 30 分鐘仍為 Pending 的訂單
    pending_txns = Transaction.objects.filter(
        status='pending',
        created_at__lt=timezone.now() - timedelta(minutes=30)
    )

    for txn in pending_txns:
        # 主動查詢 PSP 狀態
        psp_response = psp_client.query_transaction(txn.psp_order_id)

        if psp_response['status'] == 'SUCCESS':
            # 補單：更新狀態並增加餘額
            txn.status = 'success'
            txn.save()
            wallet_service.credit(txn.player_id, txn.amount)
            logger.info(f"补单成功: {txn.id}")

        elif psp_response['status'] == 'FAILED':
            txn.status = 'failed'
            txn.save()
```

### 8.2 玩家申訴處理

**申訴流程**：
1. 玩家上傳支付憑證（銀行轉帳截圖）
2. 客服調用 PSP API 查詢
3. 若 PSP 確認收款但平台未入帳 → 手動補單 + 記錄審計日誌

---

## 9. 審批流程

### 9.1 配置變更審批

**敏感操作清單**：
- **新增/修改 PSP**：修改商戶號/密鑰為高風險操作
- **調整路由權重**：可能影響成本與成功率
- **變更限額**：單筆/單日限額調整

**審批流程**（引用 09-02 審批系統）：
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
```

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
```

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
   ```python
   received_signature = request.headers['X-PSP-Signature']
   expected_signature = hmac.new(
       WEBHOOK_SECRET.encode(),
       request.body,
       hashlib.sha256
   ).hexdigest()

   if received_signature != expected_signature:
       return Response(status=403)
   ```

2. **冪等性保護**：
   - 使用 `transaction_id` 作為唯一鍵，防止重複處理
   - Redis 鎖：`SET callback:{txn_id} 1 EX 60 NX`

---

## 11. 相關文檔

### 業務邏輯參考
- [02-06 統一錢包模型](./02-06_Unified_Wallet_Model.md) - 存款入帳錢包邏輯
- [02-01 出金風控](./02-01_Withdrawal_Risk_Control.md) - 提款流程與風控
- [02-03 對賬系統](./02-03_Reconciliation_System.md) - PSP 對賬流程

### 技術架構參考
- [09-03 數據安全標準](../09_System_Security/09-03_Data_Security_Standard.md) - 支付數據加密
- [12-05 API 設計標準](../12_Technical_Operations/12-05_API_Design_Standard.md) - API 規範
- [09-02 審計日誌與審批](../09_System_Security/09-02_Audit_Log_&_Approval.md) - 配置變更審批

---

**文檔版本**: 1.1.0
**最後更新**: 2026-01-27
**維護團隊**: Finance Team & Backend Team
