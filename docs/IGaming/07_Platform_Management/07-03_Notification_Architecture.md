# 07-03 通訊基礎設施 (Notification Infrastructure)

## 1. 系統概述
Notification Service 是平台對外的唯一發信通道，負責管理所有觸達用戶的訊息。
旨在解決 **"渠道分散"**、**"模板難維護"** 與 **"費用失控"** 三大痛點。

## 2. 核心架構
```
[Business Service] -> [Notification API] -> [Template Engine] -> [Deduplication/RateLimit] -> [Provider Adapter] -> [External Gateway]
```

### 2.1 支援渠道 (Channels)
1.  **Transactional (高優先)**：
    *   **SMS**: OTP 驗證碼 (Twilio, AWS SNS)。
    *   **Email**: 註冊確認、密碼重置 (AWS SES, SendGrid)。
2.  **Marketing (低優先)**：
    *   **Web Push**: 瀏覽器通知 (Firebase FCM)。
    *   **In-App Message**: 站內信 (WebSocket)。
    *   **IM Integration**: Telegram Bot, Line OA, WhatsApp Business。

### 2.2 智選路由 (Smart Routing)
系統依據 **"成本"** 與 **"送達率"** 自動選擇渠道：
*   **Case 1 (OTP)**: 優先嘗試 Telegram/WhatsApp (免費) -> 失敗轉 SMS (付費)。
*   **Case 2 (Marketing)**: 優先發送 App Push -> 未讀轉 Email。

---

## 3. 功能需求

### 3.1 模板管理 (Template Management)
*   所有訊息內容禁止 Hardcode，必須使用模板代碼 (Template Code)。
*   **多語言支援 (Localization)**：
    *   模板內容不直接存儲多語言文本，而是引用 `08-05_Localization_System` 定義的 Translation Keys。
    *   **Key**: `notification.otp.register_body`
    *   **Fallback**: 若 Translation Service 異常，使用英文默認值。
    *   **範例**:
        *   Template: `{{t "notification.otp.register_body" code=code}}`
        *   Resolved (ZH): `您的驗證碼是 123456，5分鐘內有效。`

### 3.2 頻率限制與防騷擾 (Rate Limiting)
*   **Global Cap**: 同一手機號 1 小時內最多接收 5 條 OTP (防止惡意刷費)。
*   **Cool-down**: 兩次發送需間隔 60 秒。
*   **DND (Do Not Disturb)**: 行銷訊息禁止在當地時間 22:00 - 08:00 發送。

---

## 4. 數據結構
```sql
CREATE TABLE notification_log (
    id UUID PRIMARY KEY,
    tenant_id INT,
    user_id UUID,
    
    channel VARCHAR(20), -- SMS, EMAIL, TG
    provider VARCHAR(20), -- TWILIO, AWS
    template_code VARCHAR(50),
    
    destination VARCHAR(255), -- Phone or Email
    status ENUM('PENDING', 'SENT', 'DELIVERED', 'FAILED'),
    cost DECIMAL(10, 4), -- 預估費用
    
    sent_at TIMESTAMP,
    created_at TIMESTAMP
);
```

### 4.2 站內信存儲 (Inbox Persistence)
除了發送 Push，行銷訊息需持久化，供玩家在 "訊息中心" 查看。
*   **Storage**: MongoDB (Schemaless, High Write).
*   **Collection**: `inbox_messages`
    *   `_id`: UUID
    *   `user_id`: Index
    *   `title`, `body`, `deep_link`
    *   `is_read`: Boolean
    *   `expire_at`: TTL Index (自動刪除過期訊息，e.g. 30天)。

## 5. 安全性
*   **脫敏存儲**：`notification_log` 中的手機號與 Email 必須加密存儲。
*   **內容過濾**：自動掃描內容是否包含敏感詞 (如 "必中", "穩贏" 等違規行銷詞彙)。
