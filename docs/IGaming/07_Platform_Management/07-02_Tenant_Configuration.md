# 07-02 商戶配置管理 (Tenant Configuration)

## 1. 系統概述
提供 "開箱即用" 的站點配置能力，讓商戶可自助化管理站點基礎屬性。
所有配置需支援動態生效，無需重啟服務。

## 2. 核心配置項

### 2.1 基礎資訊 (Basic Info)
- **站點名稱**：顯示於瀏覽器 Title。
- **域名綁定 (Domains)**：
  - 主域名 (www.abc.com)
  - API 域名 (api.abc.com)
  - CDN 域名 (img.abc.com)
- **幣種 (Currency)**：
  - 主幣種 (Base Currency)
  - 支援的錢包幣種 (如 USD, VND, THB)。

### 2.2 限額與開關 (Limits & Switches)
- **維護開關**：一鍵開啟 "全站維護" 或 "前台維護"。
- **註冊開關**：暫停新用戶註冊。
- **IP 限制**：
  - 允許訪問的國家 (GeoIP Allowlist)。
  - 禁止訪問的國家 (Blacklist)。
  - 辦公室 IP 白名單 (後台訪問用)。

### 2.3 遊戲額度控制 (Credit Quota)
- **買分模式 (Transfer Mode)**：
  - 商戶需先向平台 "購買額度" (Pre-paid)，玩家才能遊戲。
  - 當商戶餘額不足時，系統自動阻擋玩家轉入遊戲，並發送警報 (Low Balance Alert)。

### 2.4 遊戲管理 (Game Management)
允許商戶針對特定遊戲廠商或遊戲進行屏蔽 (例如：因當地法規或商業考量)。
- **黑名單配置 (Blocklist)**：
  - `Provider Level`: 屏蔽整個廠商 (e.g. Block 'PGSoft')。
  - `Game Level`: 屏蔽特定遊戲 (e.g. Block 'mahjong-ways-2')。
- **維護模式 (Maintenance)**：
  - 商戶可自行設定顯示 "維護中" 頁面，而不依賴平台的全局維護。


## 3. 動態配置與審批 (Dynamic Config & Approval)

### 3.1 動態配置原則
- **實時生效**：IP 白名單、域名綁定、維護開關等配置修改後，必須在 < 1分鐘內同步至所有 Gateway 節點。
- **無停機變更**：配置變更絕不可導致服務重啟。

### 3.2 審批工作流
- **高危操作 (雙人覆核)**：
  - **修改 IP 限制**：防止惡意封鎖正常流量或誤開後門。
  - **修改域名綁定**：防止域名劫持風險。
  - **流程**：商戶 Admin 提交 -> 平台運營覆核 -> 生效。
- **一般操作 (記錄留痕)**：
  - **修改站點名稱/Logo**：無需審批，但必須記錄 Audit Log。
  - **Audit Log 格式**：`[User] [Action] [Target] [OldValue] -> [NewValue]`。

---

## 4. 配置模板系統

### 4.1 預設配置模板

**模板分類**：
- **亞洲市場模板** (Asia Template)：預設貨幣 VND/THB，啟用 Prompt Pay/VietQR
- **歐洲市場模板** (EU Template)：預設貨幣 EUR，啟用 SEPA/Trustly，強制 GDPR 合規
- **拉美市場模板** (LATAM Template)：預設貨幣 BRL/MXN，啟用 PIX/Mercado Pago

**模板內容範例** (JSON):
```json
{
  "template_name": "Asia-VN",
  "currency": "VND",
  "timezone": "Asia/Ho_Chi_Minh",
  "payment_methods": ["VietQR", "MoMo", "ZaloPay"],
  "languages": ["vi", "en"],
  "game_providers": ["PG Soft", "Pragmatic Play", "Evolution"],
  "kyc_level": "BASIC", // 亞洲市場降低 KYC 門檻
  "withdrawal_limit_daily": 50000000, // 50M VND
  "auto_features": {
    "auto_approve_withdrawal_under": 1000000, // 1M VND 以下自動批准
    "auto_enable_bonus_on_deposit": true
  }
}
```

### 4.2 配置驗證規則

**前端驗證**：
- 域名格式檢查（正則表達式驗證）
- IP 地址格式檢查（支持 IPv4/IPv6）
- 金額範圍檢查（最小/最大限額）

**後端驗證**：

---

## 5. 熱更新機制

### 5.1 配置同步架構

```
┌──────────────────────────────────────────────────┐
│  Admin Portal (配置更新)                          │
└──────────────────┬───────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────┐
│  Configuration Service (主庫)                    │
│  - 驗證配置                                      │
│  - 寫入 MySQL                                   │
│  - 發布事件到 Redis Pub/Sub                     │
└──────────────────┬───────────────────────────────┘
                   │
       ┌───────────┼───────────┬───────────┐
       ▼           ▼           ▼           ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ Gateway  │ │ Gateway  │ │ API      │ │ Worker   │
│ Node 1   │ │ Node 2   │ │ Server   │ │ Node     │
└──────────┘ └──────────┘ └──────────┘ └──────────┘
     ↓ 訂閱 Redis 'config:update' 事件
     ↓ 刷新本地快取 (Caffeine Cache)
     ↓ 生效時間：< 5 秒
```

### 5.2 配置優先級

**配置覆蓋順序** (由高到低):
1. **Runtime Override** (運營後台實時修改)
2. **Tenant Config** (商戶自訂配置)
3. **Template Config** (市場模板預設)
4. **System Default** (系統全局預設)

**範例場景**：
- 系統預設最小充值 $10
- 亞洲模板覆蓋為 $5
- VIP 商戶 A 自訂為 $1
- 運營臨時調整為 $20（促銷活動結束後需手動改回）

---

## 6. 配置管理 API

### 6.1 獲取配置 API

```http
GET /api/v1/tenant/config

Headers:
  Authorization: Bearer {token}
  X-Tenant-ID: {tenantId}

Response (200 OK):
{
  "code": 0,
  "data": {
    "site_name": "Casino VN",
    "primary_domain": "casino-vn.com",
    "base_currency": "VND",
    "timezone": "Asia/Ho_Chi_Minh",
    "maintenance_mode": false,
    "registration_enabled": true,
    "allowed_countries": ["VN", "TH", "ID"],
    "payment_methods": ["VietQR", "MoMo", "Bank Transfer"],
    "kyc_level": "BASIC",
    "limits": {
      "min_deposit": 100000,
      "max_withdrawal_daily": 50000000
    }
  }
}
```

### 6.2 更新配置 API

```http
PUT /api/v1/tenant/config

Request Body:
{
  "maintenance_mode": true,
  "maintenance_message": "系統升級中，預計 2 小時後恢復",
  "allowed_countries": ["VN", "TH"] // 移除印尼
}

Response (200 OK):
{
  "code": 0,
  "msg": "配置已更新，將於 5 秒內同步至所有節點",
  "data": {
    "config_version": "v2.1.45",
    "updated_at": "2026-01-27T10:30:00Z"
  }
}
```

---

## 7. 監控與告警

### 7.1 配置健康檢查

**檢查項目**：
- ✅ 所有 Gateway 節點配置版本一致
- ✅ 域名 DNS 解析正常
- ✅ IP 白名單無衝突（不可同時在黑白名單）
- ✅ 商戶餘額充足（> 最小閾值）

**告警規則**：
- ⚠️ 配置同步失敗（節點配置版本不一致超過 1 分鐘）
- ⚠️ 商戶餘額低於閾值（< $1000）
- 🔴 維護模式意外開啟（非計劃維護窗口）

### 7.2 配置變更審計

**審計日誌格式**：
```json
{
  "audit_id": "audit_20260127_001",
  "tenant_id": 123,
  "user_id": "admin_001",
  "action": "UPDATE_CONFIG",
  "target": "allowed_countries",
  "old_value": ["VN", "TH", "ID"],
  "new_value": ["VN", "TH"],
  "reason": "印尼市場法規變更",
  "approval_status": "APPROVED",
  "approved_by": "platform_admin_005",
  "timestamp": "2026-01-27T10:30:00Z",
  "ip_address": "192.168.1.100"
}
```

---

## 📚 相關文檔

### 核心依賴
- [07-01 系統層級架構](./07-01_Hierarchy_Architecture.md) - 多租戶架構設計
- [09-04 審批工作流系統](../09_System_Security/09-04_Approval_Workflow_System.md) - 高危操作審批

### 技術參考
- [12-03 網關架構](../12_Technical_Operations/12-03_Gateway_Architecture.md) - 配置同步機制
- [09-02 審計日誌系統](../09_System_Security/09-02_Audit_Log_System.md) - 配置變更記錄

### 業務整合
- [02-02 支付網關集成](../02_Finance_Center/02-02_Payment_Gateway_Integration.md) - 支付方式配置
- [03-02 遊戲大廳管理](../03_Game_Center/03-02_Game_Lobby_Management.md) - 遊戲黑名單配置

---

**最後更新**: 2026-01-27
**維護團隊**: Platform Team

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Platform Team & DevOps Team
